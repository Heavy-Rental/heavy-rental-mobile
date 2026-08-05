package com.heavyrental.data.repository

import com.heavyrental.data.models.Booking
import com.heavyrental.data.models.BookingStatus
import com.heavyrental.data.models.DeliveryItem
import com.heavyrental.data.models.ReturnItem
import com.heavyrental.network.RetrofitInstance
import com.heavyrental.network.toBooking
import com.heavyrental.network.toDeliveryItem
import com.heavyrental.network.toReturnItem
import com.heavyrental.network.dto.StatusUpdateRequest

class BookingRepository {

    private val api = RetrofitInstance.api

    // PATCH /api/deliveries/{bookingId}/status — e.g. CONFIRMED → MOBILISED
    suspend fun updateDeliveryStatus(bookingId: String, newStatus: BookingStatus) {
        api.updateDeliveryStatus(bookingId, StatusUpdateRequest(bookingStatus = newStatus.name))
    }

    // PATCH /api/returns/{bookingId}/status — e.g. MOBILISED → COMPLETED
    suspend fun updateReturnStatus(bookingId: String, newStatus: BookingStatus) {
        api.updateReturnStatus(bookingId, StatusUpdateRequest(bookingStatus = newStatus.name))
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
