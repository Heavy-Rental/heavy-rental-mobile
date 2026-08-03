package com.heavyrental.data.repository

import com.heavyrental.network.RetrofitInstance
import com.heavyrental.network.dto.BookingDto
import com.heavyrental.data.models.Booking
import com.heavyrental.data.models.BookingStatus
import java.time.LocalDate
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

    suspend fun getBookings(): List<Booking> {
        return api.getBookings().map { dto ->
            Booking(
                bookingId = dto.bookingId,
                customerName = dto.customerName,
                assetName = dto.assetName,
                serialNumber = dto.serialNumber,
                quantity = dto.quantity,
                projectLocation = dto.projectLocation,
                startDate = LocalDate.parse(dto.startDate),
                endDate = LocalDate.parse(dto.endDate),
                bookingStatus = BookingStatus.valueOf(dto.bookingStatus)
            )
        }
    }
}
