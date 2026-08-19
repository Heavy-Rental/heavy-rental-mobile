package com.heavyrental.viewmodel

import com.heavyrental.MainDispatcherRule
import com.heavyrental.data.models.AssetLine
import com.heavyrental.data.models.Booking
import com.heavyrental.data.models.BookingStatus
import com.heavyrental.data.models.DeliveryItem
import com.heavyrental.data.models.ReturnItem
import com.heavyrental.data.repository.AuthRepository
import com.heavyrental.data.repository.BookingRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.time.LocalDate

/**
 * Covers the domain transition table in specification/domain/booking-status-machine.md
 * ("Test cases (domain)") and the failure-type split (O1) from
 * specification/product/05-offline-fallback.md — success/IOException apply locally,
 * HttpException/other Exception leave the booking unchanged.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val bookingRepository = mockk<BookingRepository>()
    private val authRepository = mockk<AuthRepository>()
    private lateinit var viewModel: AppViewModel

    private val today: LocalDate = LocalDate.now()

    private fun booking(
        id: Long,
        status: BookingStatus?,
        assetName: String = "JLG 460SJ Boom Lift"
    ) = Booking(
        bookingId = id,
        customerName = "Acme Co",
        startDate = today,
        endDate = today,
        bookingStatus = status,
        projectLocation = "123 Site Rd",
        items = listOf(AssetLine(assetName = assetName, serialNumber = "SN-$id")),
        deliveryNotes = ""
    )

    private fun deliveryItem(id: Long, status: BookingStatus?) = DeliveryItem(
        bookingId = id,
        customerName = "Acme Co",
        startDate = today,
        projectLocation = "123 Site Rd",
        items = listOf(AssetLine(assetName = "JLG 460SJ Boom Lift", serialNumber = "SN-$id")),
        deliveryNotes = "",
        bookingStatus = status
    )

    private fun returnItem(id: Long, status: BookingStatus?, returnNotes: String = "") = ReturnItem(
        bookingId = id,
        customerName = "Acme Co",
        endDate = today,
        projectLocation = "123 Site Rd",
        items = listOf(AssetLine(assetName = "JLG 460SJ Boom Lift", serialNumber = "SN-$id")),
        deliveryNotes = "",
        returnNotes = returnNotes,
        bookingStatus = status
    )

    private fun httpException(code: Int) = HttpException(
        Response.error<Any>(code, "".toResponseBody("application/json".toMediaType()))
    )

    /** Seeds `_bookings`/`_deliveries`/`_returns` deterministically via loadData(), bypassing MockDataRepository. */
    private fun seed(
        bookings: List<Booking> = emptyList(),
        deliveries: List<DeliveryItem> = emptyList(),
        returns: List<ReturnItem> = emptyList()
    ) {
        coEvery { bookingRepository.getBookings() } returns bookings
        coEvery { bookingRepository.getTodaysDeliveries() } returns deliveries
        coEvery { bookingRepository.getTodaysReturns() } returns returns
        viewModel.loadData()
    }

    @Before
    fun setUp() {
        viewModel = AppViewModel(authRepository, bookingRepository)
    }

    // ── Domain transition table (booking-status-machine.md) ──────────────

    @Test
    fun `1 - delivery CONFIRMED to MOBILISED is accepted`() = runTest {
        coEvery { bookingRepository.updateDeliveryStatus(3L, BookingStatus.MOBILISED) } returns Unit
        seed(deliveries = listOf(deliveryItem(3L, BookingStatus.CONFIRMED)))

        viewModel.updateDeliveryStatus(3L, BookingStatus.MOBILISED)
        advanceUntilIdle()

        assertEquals(BookingStatus.MOBILISED, viewModel.deliveries.value.first { it.bookingId == 3L }.bookingStatus)
        assertNull(viewModel.networkError.value)
        coVerify(exactly = 1) { bookingRepository.updateDeliveryStatus(3L, BookingStatus.MOBILISED) }
    }

    @Test
    fun `2 - delivery CONFIRMED to COMPLETED is rejected, no API call`() = runTest {
        seed(deliveries = listOf(deliveryItem(3L, BookingStatus.CONFIRMED)))

        // updateDeliveryStatus's only valid target is MOBILISED, so requesting COMPLETED
        // is not the expected target and must be a no-op.
        viewModel.updateDeliveryStatus(3L, BookingStatus.COMPLETED)
        advanceUntilIdle()

        assertEquals(BookingStatus.CONFIRMED, viewModel.deliveries.value.first { it.bookingId == 3L }.bookingStatus)
        coVerify(exactly = 0) { bookingRepository.updateDeliveryStatus(any(), any()) }
    }

    @Test
    fun `3 - return MOBILISED to COMPLETED is accepted with returnNotes applied`() = runTest {
        coEvery { bookingRepository.updateReturnStatus(5L, BookingStatus.COMPLETED, "Returned in good condition") } returns Unit
        seed(returns = listOf(returnItem(5L, BookingStatus.MOBILISED)))

        viewModel.updateReturnStatus(5L, BookingStatus.COMPLETED, "Returned in good condition")
        advanceUntilIdle()

        val updated = viewModel.returns.value.first { it.bookingId == 5L }
        assertEquals(BookingStatus.COMPLETED, updated.bookingStatus)
        assertEquals("Returned in good condition", updated.returnNotes)
        assertNull(viewModel.networkError.value)
    }

    @Test
    fun `4 - return MOBILISED to CONFIRMED is rejected`() = runTest {
        seed(returns = listOf(returnItem(5L, BookingStatus.MOBILISED)))

        viewModel.updateReturnStatus(5L, BookingStatus.CONFIRMED, "")
        advanceUntilIdle()

        assertEquals(BookingStatus.MOBILISED, viewModel.returns.value.first { it.bookingId == 5L }.bookingStatus)
        coVerify(exactly = 0) { bookingRepository.updateReturnStatus(any(), any(), any()) }
    }

    @Test
    fun `5 - return COMPLETED to MOBILISED is rejected (wrong current status)`() = runTest {
        seed(returns = listOf(returnItem(5L, BookingStatus.COMPLETED)))

        // MOBILISED is not the expected target for the return path, so this is a no-op
        // regardless of current status.
        viewModel.updateReturnStatus(5L, BookingStatus.MOBILISED, "")
        advanceUntilIdle()

        assertEquals(BookingStatus.COMPLETED, viewModel.returns.value.first { it.bookingId == 5L }.bookingStatus)
        coVerify(exactly = 0) { bookingRepository.updateReturnStatus(any(), any(), any()) }
    }

    @Test
    fun `6 - missing booking id is rejected, no API call`() = runTest {
        seed(deliveries = listOf(deliveryItem(3L, BookingStatus.CONFIRMED)))

        viewModel.updateDeliveryStatus(999L, BookingStatus.MOBILISED)
        advanceUntilIdle()

        coVerify(exactly = 0) { bookingRepository.updateDeliveryStatus(any(), any()) }
        assertNull(viewModel.networkError.value)
    }

    @Test
    fun `wrong current status for delivery is rejected`() = runTest {
        // Item is already MOBILISED; requesting MOBILISED again requires expectedCurrent
        // CONFIRMED, which no longer holds.
        seed(deliveries = listOf(deliveryItem(3L, BookingStatus.MOBILISED)))

        viewModel.updateDeliveryStatus(3L, BookingStatus.MOBILISED)
        advanceUntilIdle()

        coVerify(exactly = 0) { bookingRepository.updateDeliveryStatus(any(), any()) }
    }

    // ── Failure-type split (O1 / offline fallback) ────────────────────────

    @Test
    fun `success clears networkError and applies status`() = runTest {
        coEvery { bookingRepository.updateDeliveryStatus(3L, BookingStatus.MOBILISED) } returns Unit
        seed(deliveries = listOf(deliveryItem(3L, BookingStatus.CONFIRMED)))

        viewModel.updateDeliveryStatus(3L, BookingStatus.MOBILISED)
        advanceUntilIdle()

        assertEquals(BookingStatus.MOBILISED, viewModel.deliveries.value.first { it.bookingId == 3L }.bookingStatus)
        assertNull(viewModel.networkError.value)
    }

    @Test
    fun `IOException still applies status locally with updated-locally-only message`() = runTest {
        coEvery { bookingRepository.updateDeliveryStatus(3L, BookingStatus.MOBILISED) } throws IOException("host unreachable")
        seed(deliveries = listOf(deliveryItem(3L, BookingStatus.CONFIRMED)))

        viewModel.updateDeliveryStatus(3L, BookingStatus.MOBILISED)
        advanceUntilIdle()

        assertEquals(BookingStatus.MOBILISED, viewModel.deliveries.value.first { it.bookingId == 3L }.bookingStatus)
        assertEquals("Could not reach API — updated locally only. (host unreachable)", viewModel.networkError.value)
    }

    @Test
    fun `HttpException 400 leaves status unchanged with rejected-by-server message`() = runTest {
        coEvery { bookingRepository.updateDeliveryStatus(3L, BookingStatus.MOBILISED) } throws httpException(400)
        seed(deliveries = listOf(deliveryItem(3L, BookingStatus.CONFIRMED)))

        viewModel.updateDeliveryStatus(3L, BookingStatus.MOBILISED)
        advanceUntilIdle()

        assertEquals(BookingStatus.CONFIRMED, viewModel.deliveries.value.first { it.bookingId == 3L }.bookingStatus)
        assertEquals("Update rejected by server (400) — status unchanged.", viewModel.networkError.value)
    }

    @Test
    fun `HttpException 403 leaves status unchanged with rejected-by-server message`() = runTest {
        coEvery { bookingRepository.updateDeliveryStatus(3L, BookingStatus.MOBILISED) } throws httpException(403)
        seed(deliveries = listOf(deliveryItem(3L, BookingStatus.CONFIRMED)))

        viewModel.updateDeliveryStatus(3L, BookingStatus.MOBILISED)
        advanceUntilIdle()

        assertEquals(BookingStatus.CONFIRMED, viewModel.deliveries.value.first { it.bookingId == 3L }.bookingStatus)
        assertEquals("Update rejected by server (403) — status unchanged.", viewModel.networkError.value)
    }

    @Test
    fun `unrecognised exception leaves status unchanged with could-not-sync message`() = runTest {
        coEvery { bookingRepository.updateDeliveryStatus(3L, BookingStatus.MOBILISED) } throws IllegalStateException("boom")
        seed(deliveries = listOf(deliveryItem(3L, BookingStatus.CONFIRMED)))

        viewModel.updateDeliveryStatus(3L, BookingStatus.MOBILISED)
        advanceUntilIdle()

        assertEquals(BookingStatus.CONFIRMED, viewModel.deliveries.value.first { it.bookingId == 3L }.bookingStatus)
        assertEquals("Could not sync status update — status unchanged. (boom)", viewModel.networkError.value)
    }
}
