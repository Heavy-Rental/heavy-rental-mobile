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

class AppViewModel @JvmOverloads constructor(
    private val authRepository: AuthRepository = AuthRepository(),
    private val bookingRepository: BookingRepository = BookingRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    // Seed so the UI always shows something immediately.
    // loadData() replaces lists from GET /api/deliveries and GET /api/returns when they succeed;
    // on failure seed remains (see product/05-offline-fallback.md).
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

    fun updateDeliveryStatus(id: Long, newStatus: BookingStatus) {
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
    fun updateReturnStatus(id: Long, newStatus: BookingStatus, returnNotes: String) {
        updateBookingStatus(
            id,
            BookingStatus.MOBILISED,
            BookingStatus.COMPLETED,
            newStatus,
            returnNotes = returnNotes
        ) {
            bookingRepository.updateReturnStatus(id, newStatus, returnNotes)
        }
    }

    private fun updateBookingStatus(
        id: Long,
        expectedCurrent: BookingStatus,
        expectedNew: BookingStatus,
        newStatus: BookingStatus,
        returnNotes: String? = null,
        apiCall: suspend () -> Unit
    ) {
        if (newStatus != expectedNew) return

        // Prefer list state (from GET /api/deliveries|returns); fall back to bookings.
        val currentStatus = _deliveries.value.find { it.bookingId == id }?.bookingStatus
            ?: _returns.value.find { it.bookingId == id }?.bookingStatus
            ?: _bookings.value.find { it.bookingId == id }?.bookingStatus
        if (currentStatus == null || currentStatus != expectedCurrent) return

        viewModelScope.launch {
            // Server-authoritative on explicit rejection (e.g. 400 — invalid transition
            // per the backend's state-machine guard): do NOT apply locally, so the UI
            // doesn't show a status change the server actually refused.
            try {
                apiCall()
                _networkError.value = null
            } catch (e: HttpException) {
                Log.e("API_ERROR", "Status update rejected by server (${e.code()})", e)
                _networkError.value = "Update rejected by server (${e.code()}) — status unchanged."
                return@launch
            } catch (e: IOException) {
                // No response reached us at all — can't confirm or deny server state.
                // Apply optimistically so the app stays usable offline (see
                // product/05-offline-fallback.md); reconciles on next successful loadData().
                Log.e("API_ERROR", e.message ?: "Network error during status update", e)
                _networkError.value = "Could not reach API — updated locally only. (${e.message})"
            } catch (e: Exception) {
                // Unknown failure shape — treat conservatively like a rejection rather
                // than risk showing a false success.
                Log.e("API_ERROR", e.message ?: "Unknown error", e)
                _networkError.value = "Could not sync status update — status unchanged. (${e.message})"
                return@launch
            }

            // Reached only on success or a network/IO failure (optimistic offline case).
            // Update list rows in place — do NOT re-derive with toDeliveryItems()/toReturnItems()
            // (that re-applies device "today" and empties Mockoon fixtures with fixed dates).
            _bookings.value = _bookings.value.map { booking ->
                if (booking.bookingId == id && booking.bookingStatus == expectedCurrent) {
                    booking.copy(bookingStatus = newStatus)
                } else {
                    booking
                }
            }

            _deliveries.value = _deliveries.value.map { item ->
                if (item.bookingId == id && item.bookingStatus == expectedCurrent) {
                    item.copy(bookingStatus = newStatus)
                } else {
                    item
                }
            }

            _returns.value = _returns.value.map { item ->
                if (item.bookingId == id && item.bookingStatus == expectedCurrent) {
                    item.copy(
                        bookingStatus = newStatus,
                        returnNotes = returnNotes ?: item.returnNotes
                    )
                } else {
                    item
                }
            }
        }
    }

    /**
     * Loads list screens from dedicated endpoints (GET /api/deliveries, GET /api/returns)
     * and optionally GET /api/bookings. Seed data remains if a call fails.
     * See specification/product/03-deliveries.md and 05-offline-fallback.md.
     */
    fun loadData() {
        viewModelScope.launch {
            val errors = mutableListOf<String>()

            try {
                _bookings.value = bookingRepository.getBookings()
            } catch (e: Exception) {
                Log.e("API_ERROR", e.message ?: "Bookings load failed", e)
                errors += "bookings: ${e.message}"
            }

            try {
                _deliveries.value = bookingRepository.getTodaysDeliveries()
            } catch (e: Exception) {
                Log.e("API_ERROR", e.message ?: "Deliveries load failed", e)
                errors += "deliveries: ${e.message}"
            }

            try {
                _returns.value = bookingRepository.getTodaysReturns()
            } catch (e: Exception) {
                Log.e("API_ERROR", e.message ?: "Returns load failed", e)
                errors += "returns: ${e.message}"
            }

            _networkError.value = if (errors.isEmpty()) {
                null
            } else {
                "Could not reach API — showing mock data. (${errors.joinToString("; ")})"
            }
        }
    }

    /** @deprecated Use [loadData]; kept name for any external call sites. */
    fun loadBookings() = loadData()

}
