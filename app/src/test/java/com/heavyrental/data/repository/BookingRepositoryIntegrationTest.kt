package com.heavyrental.data.repository

import com.heavyrental.data.models.BookingStatus
import com.heavyrental.network.HeavyRentalApiService
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.IOException

/**
 * Exercises BookingRepository against a real Retrofit + real kotlinx.serialization stack, wired to
 * MockWebServer instead of the app's real backend — no mocking of the network layer itself, so
 * this proves the wire contract in specification/domain/booking-status-machine.md ("Request
 * payload") and the exception-type split assumed by AppViewModel (HttpException vs IOException)
 * actually happens at the OkHttp/Retrofit boundary, not just in a mocked unit test.
 */
class BookingRepositoryIntegrationTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: BookingRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()

        val json = Json { ignoreUnknownKeys = true; isLenient = true }
        // retryOnConnectionFailure = false: keeps the DISCONNECT_AT_START test deterministic —
        // otherwise OkHttp transparently retries on a fresh connection, which would either mask
        // the failure or hang forever waiting on a MockWebServer response we never enqueue.
        val client = OkHttpClient.Builder().retryOnConnectionFailure(false).build()
        val api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(HeavyRentalApiService::class.java)

        repository = BookingRepository(api)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `updateDeliveryStatus sends bookingStatus-only payload`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"bookingId":3,"assetName":"a","serialNumber":"s"}"""))

        repository.updateDeliveryStatus(3L, BookingStatus.MOBILISED)

        val recorded = server.takeRequest()
        assertEquals("PATCH", recorded.method)
        assertEquals("/api/deliveries/3/status", recorded.path)
        assertEquals("""{"bookingStatus":"MOBILISED"}""", recorded.body.readUtf8())
    }

    @Test
    fun `updateReturnStatus sends bookingStatus and returnNotes as its own schema`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"bookingId":5,"assetName":"a","serialNumber":"s"}"""))

        repository.updateReturnStatus(5L, BookingStatus.COMPLETED, "Returned in good condition")

        val recorded = server.takeRequest()
        assertEquals("PATCH", recorded.method)
        assertEquals("/api/returns/5/status", recorded.path)
        assertEquals(
            """{"bookingStatus":"COMPLETED","returnNotes":"Returned in good condition"}""",
            recorded.body.readUtf8()
        )
    }

    @Test
    fun `400 response surfaces as HttpException with code 400`() = runTest {
        server.enqueue(MockResponse().setResponseCode(400).setBody("""{"error":"invalid transition"}"""))

        try {
            repository.updateDeliveryStatus(3L, BookingStatus.MOBILISED)
            fail("Expected HttpException")
        } catch (e: HttpException) {
            assertEquals(400, e.code())
        }
    }

    @Test
    fun `403 response surfaces as HttpException with code 403`() = runTest {
        server.enqueue(MockResponse().setResponseCode(403).setBody("""{"error":"forbidden"}"""))

        try {
            repository.updateReturnStatus(5L, BookingStatus.COMPLETED, "")
            fail("Expected HttpException")
        } catch (e: HttpException) {
            assertEquals(403, e.code())
        }
    }

    @Test
    fun `connection dropped before a response surfaces as IOException`() = runTest {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

        try {
            repository.updateDeliveryStatus(3L, BookingStatus.MOBILISED)
            fail("Expected IOException")
        } catch (e: IOException) {
            // expected
        }
    }

    @Test
    fun `getTodaysDeliveries maps the JSON array end-to-end`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """[{"bookingId":3,"customerName":"Acme Co","startDate":"2026-08-12","siteAddress":"123 Site Rd","assetName":"JLG 460SJ Boom Lift","serialNumber":"SN-3","bookingStatus":"CONFIRMED"}]"""
            )
        )

        val deliveries = repository.getTodaysDeliveries()

        assertEquals(1, deliveries.size)
        assertEquals(3L, deliveries[0].bookingId)
        assertEquals("JLG 460SJ Boom Lift", deliveries[0].assetName)
        assertEquals(BookingStatus.CONFIRMED, deliveries[0].bookingStatus)
    }

    @Test
    fun `getTodaysReturns maps the JSON array end-to-end`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """[{"bookingId":5,"customerName":"Acme Co","endDate":"2026-08-12","siteAddress":"123 Site Rd","assetName":"Toyota 8FD25 Forklift","serialNumber":"SN-5","returnNotes":"","bookingStatus":"MOBILISED"}]"""
            )
        )

        val returns = repository.getTodaysReturns()

        assertEquals(1, returns.size)
        assertEquals(5L, returns[0].bookingId)
        assertEquals("Toyota 8FD25 Forklift", returns[0].assetName)
        assertEquals(BookingStatus.MOBILISED, returns[0].bookingStatus)
    }
}
