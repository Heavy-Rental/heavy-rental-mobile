package com.heavyrental.viewmodel

import androidx.lifecycle.ViewModel
import com.heavyrental.data.models.BookingStatus
import com.heavyrental.navigation.AppScreen
import com.heavyrental.data.models.DeliveryItem
import com.heavyrental.data.models.ReturnItem
import com.heavyrental.data.repository.MockDataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.heavyrental.data.models.Booking
import com.heavyrental.data.models.toDeliveryItems
import com.heavyrental.data.models.toReturnItems
import com.heavyrental.data.repository.AuthRepository
import com.heavyrental.data.repository.BookingRepository
import androidx.lifecycle.viewModelScope
import android.util.Log
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException


data class AppState(
    val isLoggedIn: Boolean = false,
    val adminName: String = "",
    val currentScreen: AppScreen = AppScreen.LOGIN,
    val loginError: String? = null,
    val isLoggingIn: Boolean = false
)

class AppViewModel : ViewModel() {

    private val authRepository = AuthRepository()
    private val bookingRepository = BookingRepository()
    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    // Seed with mock data so the UI always shows something immediately.
    // loadBookings() below will overwrite this with real API data once the network call succeeds
    // if it fails, the mock data stays visible instead of leaving the screen blank
    private val _bookings = MutableStateFlow(MockDataRepository.bookingList)
    val bookings: StateFlow<List<Booking>> = _bookings.asStateFlow()

    private val _deliveries = MutableStateFlow(_bookings.value.toDeliveryItems())
    val deliveries: StateFlow<List<DeliveryItem>> = _deliveries.asStateFlow()

    private val _returns = MutableStateFlow(_bookings.value.toReturnItems())
    val returns: StateFlow<List<ReturnItem>> = _returns.asStateFlow()

    // Surfaces API failures instead of only logging them,
    // so it's obvious in the UI
    private val _networkError = MutableStateFlow<String?>(null)
    val networkError: StateFlow<String?> = _networkError.asStateFlow()

    // ────────── Auth ────────────
    // Interim → access Bearer flow (getBearerToken → login → logout).
    // See specification/product/01-login.md and AuthRepository.

    fun login(email: String, password: String) {
        if (_state.value.isLoggingIn) return

        _state.value = _state.value.copy(isLoggingIn = true, loginError = null)

        viewModelScope.launch {
            try {
                val response = authRepository.login(email, password)
                _state.value = _state.value.copy(
                    isLoggedIn = true,
                    adminName = response.username.substringBefore("@").replaceFirstChar { it.uppercase() },
                    currentScreen = AppScreen.HOME,
                    loginError = null,
                    isLoggingIn = false
                )
            } catch (e: HttpException) {
                Log.e("AUTH_ERROR", e.message ?: "Login failed", e)
                val message = when (e.code()) {
                    400 -> "Email and password are required."
                    401 -> "Invalid email or password."
                    403 -> "Unable to sign in — please try again."
                    else -> "Login failed (${e.code()}). Please try again."
                }
                _state.value = _state.value.copy(loginError = message, isLoggingIn = false)
            } catch (e: IOException) {
                Log.e("AUTH_ERROR", e.message ?: "Network error during login", e)
                _state.value = _state.value.copy(
                    loginError = "Could not reach the server. Please try again.",
                    isLoggingIn = false
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                authRepository.logout()
            } catch (e: Exception) {
                // Best-effort revoke — session is cleared locally regardless (see AuthRepository.logout).
                Log.e("AUTH_ERROR", e.message ?: "Logout call failed", e)
            }
            _state.value = AppState()
        }
    }

    fun navigate(screen: AppScreen) {
        _state.value = _state.value.copy(currentScreen = screen)
    }

    // ───── Status updates ──────

    fun updateDeliveryStatus(id: String, newStatus: BookingStatus) {
        updateBookingStatus(
            id,
            BookingStatus.CONFIRMED,
            BookingStatus.MOBILISED,
            newStatus
        ) {
            bookingRepository.updateDeliveryStatus(id, newStatus)
        }
    }

    // Mobilised → Completed only. Silently ignored for any other transition.
    fun updateReturnStatus(id: String, newStatus: BookingStatus) {
        updateBookingStatus(
            id,
            BookingStatus.MOBILISED,
            BookingStatus.COMPLETED,
            newStatus
        ) {
            bookingRepository.updateReturnStatus(id, newStatus)
        }
    }

    private fun updateBookingStatus(
        id: String,
        expectedCurrent: BookingStatus,
        expectedNew: BookingStatus,
        newStatus: BookingStatus,
        apiCall: suspend () -> Unit
    ) {
        if (newStatus != expectedNew) return

        val current = _bookings.value.find { it.bookingId == id }
        if (current == null || current.bookingStatus != expectedCurrent) return

        viewModelScope.launch {
            try {
                apiCall()
                _networkError.value = null
            } catch (e: Exception) {
                Log.e("API_ERROR", e.message ?: "Unknown error", e)
                _networkError.value = "Could not sync status update to API — updated locally only. (${e.message})"
            }

            // Applied locally either way so the app stays usable if the API is unreachable
            // same fallback pattern as loadBookings()
            _bookings.value = _bookings.value.map { booking ->
                if (booking.bookingId == id && booking.bookingStatus == expectedCurrent) {
                    booking.copy(bookingStatus = newStatus)
                } else {
                    booking
                }
            }

            _deliveries.value = _bookings.value.toDeliveryItems()
            _returns.value = _bookings.value.toReturnItems()
        }
    }

    fun loadBookings() {
        viewModelScope.launch {
            try {
                val result = bookingRepository.getBookings()

                _bookings.value = result
                _deliveries.value = result.toDeliveryItems()
                _returns.value = result.toReturnItems()
                _networkError.value = null

            } catch (e: Exception) {
                Log.e("API_ERROR", e.message ?: "Unknown error", e)
                _networkError.value = "Could not reach API — showing mock data. (${e.message})"
            }
        }
    }

}