package com.heavyrental.integration

import com.heavyrental.data.models.BookingStatus
import com.heavyrental.data.repository.AuthRepository
import com.heavyrental.data.repository.BookingRepository
import com.heavyrental.network.HeavyRentalApiService
import com.heavyrental.viewmodel.AppViewModel
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * End-to-end slice covering O1 (specification/product/05-offline-fallback.md): real AppViewModel +
 * real BookingRepository + real Retrofit/OkHttp against MockWebServer — nothing mocked except the
 * HTTP transport being a local server instead of the actual backend. Confirms the failure-type
 * split survives all three layers, not just the mocked-repository unit test in AppViewModelTest.
 *
 * Runs on a real background dispatcher (not a virtual-time test dispatcher) since the network round
 * trip is genuine async I/O; assertions poll with a timeout instead of relying on coroutine-test
 * scheduler control.
 */
class StatusUpdateIntegrationTest {

    private lateinit var server: MockWebServer
    private lateinit var viewModel: AppViewModel
    private lateinit var client: OkHttpClient

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Default)

        server = MockWebServer()
        server.start()

        val json = Json { ignoreUnknownKeys = true; isLenient = true }
        // retryOnConnectionFailure = false: keeps the DISCONNECT_AT_START scenario deterministic,
        // see BookingRepositoryIntegrationTest for why.
        client = OkHttpClient.Builder().retryOnConnectionFailure(false).build()
        val api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(HeavyRentalApiService::class.java)

        viewModel = AppViewModel(mockk<AuthRepository>(), BookingRepository(api))
    }

    @After
    fun tearDown() {
        server.shutdown()
        Dispatchers.resetMain()
    }

    private suspend fun awaitUntil(timeoutMs: Long = 5_000, condition: () -> Boolean) {
        withTimeout(timeoutMs) {
            while (!condition()) delay(10)
        }
    }

    /**
     * Enqueues the three GET responses loadData() issues in order: bookings, deliveries, returns,
     * runs loadData(), and blocks until the server has actually received all three requests.
     *
     * That last part matters: MockWebServer serves enqueued responses strictly in the order
     * requests *arrive*, not in the order a test enqueues them. loadData()'s three GETs and a
     * later updateDeliveryStatus() PATCH run as two independent coroutines on a real dispatcher, so
     * without this, the test thread could enqueue+trigger the PATCH before loadData()'s last GET
     * (returns) has actually reached the server — handing the PATCH the "returns" response (and the
     * later-arriving returns GET whatever was meant for the PATCH), scrambling the whole scenario.
     * Waiting for takeRequest() three times guarantees the server has seen every GET — and thus
     * that the client has no further GET left to send — before any PATCH response is queued.
     */
    private fun loadInitialDataAndAwaitReceipt() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("[]"))
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """[{"bookingId":3,"customerName":"Acme Co","startDate":"2026-08-12","siteAddress":"123 Site Rd","items":[{"assetName":"JLG 460SJ Boom Lift","serialNumber":"SN-3"}],"bookingStatus":"CONFIRMED"}]"""
            )
        )
        server.enqueue(MockResponse().setResponseCode(200).setBody("[]"))

        viewModel.loadData()
        repeat(3) { server.takeRequest(5, TimeUnit.SECONDS) }
    }

    @Test
    fun `server rejection leaves status unchanged with rejected-by-server message`() = runBlocking {
        loadInitialDataAndAwaitReceipt()
        awaitUntil { viewModel.deliveries.value.any { it.bookingId == 3L } }

        server.enqueue(MockResponse().setResponseCode(400).setBody("""{"error":"invalid transition"}"""))
        viewModel.updateDeliveryStatus(3L, BookingStatus.MOBILISED)
        awaitUntil { viewModel.networkError.value != null }

        assertEquals(BookingStatus.CONFIRMED, viewModel.deliveries.value.first { it.bookingId == 3L }.bookingStatus)
        assertEquals("Update rejected by server (400) — status unchanged.", viewModel.networkError.value)
    }

    @Test
    fun `unreachable host applies status locally with updated-locally-only message`() = runBlocking {
        loadInitialDataAndAwaitReceipt()
        awaitUntil { viewModel.deliveries.value.any { it.bookingId == 3L } }

        client.connectionPool.evictAll() // force a fresh connection so DISCONNECT_AT_START actually fires
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
        viewModel.updateDeliveryStatus(3L, BookingStatus.MOBILISED)
        awaitUntil { viewModel.networkError.value != null }

        assertTrue(
            "Unexpected message: ${viewModel.networkError.value}",
            viewModel.networkError.value!!.startsWith("Could not reach API — updated locally only.")
        )
        assertEquals(BookingStatus.MOBILISED, viewModel.deliveries.value.first { it.bookingId == 3L }.bookingStatus)
    }

    @Test
    fun `successful load leaves networkError null`() = runBlocking {
        loadInitialDataAndAwaitReceipt()
        awaitUntil { viewModel.deliveries.value.any { it.bookingId == 3L } }

        assertNull(viewModel.networkError.value)
    }
}
