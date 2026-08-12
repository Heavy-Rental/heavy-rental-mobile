package com.heavyrental.data.repository

import com.heavyrental.data.models.Booking
import com.heavyrental.data.models.BookingStatus
import com.heavyrental.data.models.DeliveryItem
import com.heavyrental.data.models.ReturnItem
import com.heavyrental.network.HeavyRentalApiService
import com.heavyrental.network.RetrofitInstance
import com.heavyrental.network.toBooking
import com.heavyrental.network.toDeliveryItem
import com.heavyrental.network.toReturnItem
import com.heavyrental.network.dto.ReturnStatusUpdateRequestDto
import com.heavyrental.network.dto.StatusUpdateRequest

class BookingRepository(private val api: HeavyRentalApiService = RetrofitInstance.api) {

    // PATCH /api/deliveries/{bookingId}/status — e.g. CONFIRMED → MOBILISED
    suspend fun updateDeliveryStatus(bookingId: Long, newStatus: BookingStatus) {
        api.updateDeliveryStatus(bookingId, StatusUpdateRequest(bookingStatus = newStatus.name))
    }

    // PATCH /api/returns/{bookingId}/status — e.g. MOBILISED → COMPLETED
    suspend fun updateReturnStatus(bookingId: Long, newStatus: BookingStatus, returnNotes: String) {
        api.updateReturnStatus(
            bookingId,
            ReturnStatusUpdateRequestDto(bookingStatus = newStatus.name, returnNotes = returnNotes)
        )
    }

    suspend fun getBookings(): List<Booking> =
        api.getBookings().map { it.toBooking() }

    /** GET /api/deliveries — server/mock already applies today's delivery membership. */
    suspend fun getTodaysDeliveries(): List<DeliveryItem> =
        api.getTodaysDeliveries().map { it.toDeliveryItem() }

    /** GET /api/returns — server/mock already applies today's return membership. */
    suspend fun getTodaysReturns(): List<ReturnItem> =
        api.getTodaysReturns().map { it.toReturnItem() }
}
