package com.heavyrental.network

import com.heavyrental.network.dto.BookingDto
import com.heavyrental.network.dto.DeliveryItemDto
import com.heavyrental.network.dto.ReturnItemDto
import com.heavyrental.network.dto.StatusUpdateRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.PUT
import retrofit2.http.Path

interface HeavyRentalApiService {

    // ── Bookings ─────────────────────────────────────────────────────────
    @GET("api/bookings")
    suspend fun getBookings(): List<BookingDto>

    @GET("api/bookings/{bookingId}")
    suspend fun getBooking(@Path("bookingId") bookingId: String): BookingDto

    @PUT("api/bookings/{bookingId}")
    suspend fun updateBooking(@Path("bookingId") bookingId: String, @Body booking: BookingDto): BookingDto

    // ── Deliveries ───────────────────────────────────────────────────────
    @GET("api/deliveries")
    suspend fun getTodaysDeliveries(): List<DeliveryItemDto>

    @PATCH("api/deliveries/{bookingId}/status")
    suspend fun updateDeliveryStatus(
        @Path("bookingId") bookingId: String,
        @Body request: StatusUpdateRequest
    ): DeliveryItemDto

    // ── Returns ──────────────────────────────────────────────────────────
    @GET("api/returns")
    suspend fun getTodaysReturns(): List<ReturnItemDto>

    @PATCH("api/returns/{bookingId}/status")
    suspend fun updateReturnStatus(
        @Path("bookingId") bookingId: String,
        @Body request: StatusUpdateRequest
    ): ReturnItemDto
}

