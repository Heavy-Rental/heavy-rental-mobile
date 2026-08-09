package com.heavyrental.network

import com.heavyrental.network.dto.BookingDto
import com.heavyrental.network.dto.BookingUpdateRequestDto
import com.heavyrental.network.dto.DeliveryItemDto
import com.heavyrental.network.dto.LoginRequest
import com.heavyrental.network.dto.LoginResponse
import com.heavyrental.network.dto.MessageResponse
import com.heavyrental.network.dto.ReturnItemDto
import com.heavyrental.network.dto.StatusUpdateRequest
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface HeavyRentalApiService {

    // ── Auth ─────────────────────────────────────────────────────────────
    // Flow: getBearerToken (interim) → login (interim Bearer, upgrades to access) →
    // business calls (access Bearer, added by AuthInterceptor) → logout (access Bearer).
    // See heavy-rental-spring-rest-api/specification/SPEC-auth-login-logout.md.

    @GET("api/auth/getBearerToken")
    suspend fun getBearerToken(): ResponseBody

    @POST("api/auth/login")
    suspend fun login(
        @Header("Authorization") interimBearer: String,
        @Body request: LoginRequest
    ): LoginResponse

    @POST("api/auth/logout")
    suspend fun logout(@Header("Authorization") accessBearer: String): MessageResponse

    // ── Bookings ─────────────────────────────────────────────────────────
    @GET("api/bookings")
    suspend fun getBookings(): List<BookingDto>

    @GET("api/bookings/{bookingId}")
    suspend fun getBooking(@Path("bookingId") bookingId: Long): BookingDto

    @PUT("api/bookings/{bookingId}")
    suspend fun updateBooking(
        @Path("bookingId") bookingId: Long,
        @Body booking: BookingUpdateRequestDto
    ): BookingDto

    // ── Deliveries ───────────────────────────────────────────────────────
    @GET("api/deliveries")
    suspend fun getTodaysDeliveries(): List<DeliveryItemDto>

    @PATCH("api/deliveries/{bookingId}/status")
    suspend fun updateDeliveryStatus(
        @Path("bookingId") bookingId: Long,
        @Body request: StatusUpdateRequest
    ): DeliveryItemDto

    // ── Returns ──────────────────────────────────────────────────────────
    @GET("api/returns")
    suspend fun getTodaysReturns(): List<ReturnItemDto>

    @PATCH("api/returns/{bookingId}/status")
    suspend fun updateReturnStatus(
        @Path("bookingId") bookingId: Long,
        @Body request: StatusUpdateRequest
    ): ReturnItemDto
}
