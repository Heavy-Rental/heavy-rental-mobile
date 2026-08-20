package com.heavyrental.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.heavyrental.data.models.Booking
import com.heavyrental.data.models.BookingStatus
import com.heavyrental.ui.theme.*
import java.time.format.DateTimeFormatter

private enum class CustomerBookingFilter { ALL, ACTIVE, COMPLETED, CANCELLED }

/**
 * Statuses that count as "active" for a customer's own filter — mirrors the backend's
 * Booking.ACTIVE_STATUSES (SPEC-entity-repository.md) so "Active" here means the same thing
 * it means server-side: a booking that still holds equipment.
 */
private val ACTIVE_STATUSES = setOf(
    BookingStatus.PENDING_DEPOSIT,
    BookingStatus.PENDING_CONFIRMED,
    BookingStatus.CONFIRMED,
    BookingStatus.MOBILISED
)

/**
 * Read-only booking list for a logged-in customer (ROLE_USER). Sourced from
 * GET /api/bookings, which the backend already scopes to the caller's own bookings
 * (BookingService.getBookings) — this screen never offers a way to edit, mobilise, or
 * complete a booking; it only ever displays bookingStatus.
 */
@Composable
fun CustomerBookingsScreen(
    customerName: String,
    bookings: List<Booking>,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf(CustomerBookingFilter.ALL) }

    val visibleBookings = remember(bookings, selectedFilter) {
        when (selectedFilter) {
            CustomerBookingFilter.ALL -> bookings
            CustomerBookingFilter.ACTIVE -> bookings.filter { it.bookingStatus in ACTIVE_STATUSES }
            CustomerBookingFilter.COMPLETED -> bookings.filter { it.bookingStatus == BookingStatus.COMPLETED }
            CustomerBookingFilter.CANCELLED -> bookings.filter { it.bookingStatus == BookingStatus.CANCELLED }
        }.sortedByDescending { it.startDate }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Surface)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("My Bookings", style = MaterialTheme.typography.titleLarge, color = Foreground)
                Text(
                    "Welcome, $customerName",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedForeground
                )
            }
            IconButton(onClick = onLogout) {
                Icon(
                    Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "Logout",
                    tint = MutedForeground
                )
            }
        }

        HorizontalDivider(color = Border)

        // Filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CustomerFilterChip(
                label    = "All",
                count    = bookings.size,
                selected = selectedFilter == CustomerBookingFilter.ALL,
                color    = Foreground,
                onClick  = { selectedFilter = CustomerBookingFilter.ALL }
            )
            CustomerFilterChip(
                label    = "Active",
                count    = bookings.count { it.bookingStatus in ACTIVE_STATUSES },
                selected = selectedFilter == CustomerBookingFilter.ACTIVE,
                color    = BlueAccent,
                onClick  = { selectedFilter = CustomerBookingFilter.ACTIVE }
            )
            CustomerFilterChip(
                label    = "Completed",
                count    = bookings.count { it.bookingStatus == BookingStatus.COMPLETED },
                selected = selectedFilter == CustomerBookingFilter.COMPLETED,
                color    = GreenAccent,
                onClick  = { selectedFilter = CustomerBookingFilter.COMPLETED }
            )
            CustomerFilterChip(
                label    = "Cancelled",
                count    = bookings.count { it.bookingStatus == BookingStatus.CANCELLED },
                selected = selectedFilter == CustomerBookingFilter.CANCELLED,
                color    = RedAccent,
                onClick  = { selectedFilter = CustomerBookingFilter.CANCELLED }
            )
        }

        HorizontalDivider(color = Border)

        if (visibleBookings.isEmpty()) {
            EmptyState(hasAnyBookings = bookings.isNotEmpty())
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(visibleBookings, key = { it.bookingId }) { booking ->
                    CustomerBookingCard(booking)
                }
            }
        }
    }
}

@Composable
private fun EmptyState(hasAnyBookings: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            if (hasAnyBookings) "No bookings match this filter" else "You don't have any bookings yet",
            style = MaterialTheme.typography.bodyMedium,
            color = MutedForeground
        )
    }
}

@Composable
private fun CustomerBookingCard(booking: Booking) {
    val (statusColor, statusLabel) = statusPresentation(booking.bookingStatus)
    val dateFormatter = remember { DateTimeFormatter.ofPattern("d MMM yyyy") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Card)
            .border(1.dp, Border, RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        // Booking ref + status badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Booking #${booking.bookingId}", style = MaterialTheme.typography.labelSmall, color = MutedForeground)
            StatusBadge(statusLabel, statusColor)
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (booking.items.isEmpty()) {
            Text(
                "Asset not specified",
                style = MaterialTheme.typography.titleLarge,
                color = MutedForeground
            )
        } else {
            booking.items.forEachIndexed { index, line ->
                if (index > 0) Spacer(modifier = Modifier.height(6.dp))
                Text(
                    line.assetName.ifBlank { "Asset not specified" },
                    style = MaterialTheme.typography.titleLarge,
                    color = if (line.assetName.isBlank()) MutedForeground else Foreground
                )
                Text(
                    line.serialNumber.ifBlank { "No serial number" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedForeground
                )
            }
        }

        if (booking.deliveryNotes.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Note: ${booking.deliveryNotes}",
                style = MaterialTheme.typography.bodySmall,
                color = Primary,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = Border)
        Spacer(modifier = Modifier.height(12.dp))

        InfoRow(
            icon = { Icon(Icons.Default.CalendarMonth, null, tint = MutedForeground, modifier = Modifier.size(15.dp)) },
            text = "${booking.startDate.format(dateFormatter)} – ${booking.endDate.format(dateFormatter)}"
        )
        Spacer(modifier = Modifier.height(4.dp))
        InfoRow(
            icon = { Icon(Icons.Default.LocationOn, null, tint = MutedForeground, modifier = Modifier.size(15.dp)) },
            text = booking.projectLocation.ifBlank { "Site address not specified" }
        )
    }
}

/**
 * Colour/label per status. Matches the "grey Unknown" convention for a null status documented
 * in specification/domain/booking-status-machine.md; the other five values each get a distinct
 * colour purely for at-a-glance scanning — this screen has no transition logic, so there's no
 * source/target pairing to keep consistent with the staff Deliveries/Returns screens.
 */
@Composable
private fun statusPresentation(status: BookingStatus?): Pair<Color, String> = when (status) {
    BookingStatus.PENDING_DEPOSIT -> MutedForeground to "Pending Deposit"
    BookingStatus.PENDING_CONFIRMED -> AmberAccent to "Pending Confirmation"
    BookingStatus.CONFIRMED -> BlueAccent to "Confirmed"
    BookingStatus.MOBILISED -> Primary to "Mobilised"
    BookingStatus.COMPLETED -> GreenAccent to "Completed"
    BookingStatus.CANCELLED -> RedAccent to "Cancelled"
    null -> Color.Gray to "Unknown"
}

@Composable
private fun InfoRow(
    icon: @Composable () -> Unit,
    text: String,
    textColor: Color = MutedForeground
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        icon()
        Spacer(modifier = Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = textColor)
    }
}

@Composable
private fun StatusBadge(label: String, color: Color) {
    Surface(color = color.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun CustomerFilterChip(
    label: String,
    count: Int,
    selected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    val bgColor  = if (selected) color.copy(alpha = 0.18f) else Color.Transparent
    val border   = if (selected) color else Border
    val txtColor = if (selected) color else MutedForeground

    Surface(
        onClick = onClick,
        color   = bgColor,
        shape   = RoundedCornerShape(20.dp),
        border  = androidx.compose.foundation.BorderStroke(1.dp, border)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(label, color = txtColor, fontSize = 12.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
            Surface(color = txtColor.copy(alpha = 0.15f), shape = RoundedCornerShape(10.dp)) {
                Text(
                    "$count",
                    color = txtColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                )
            }
        }
    }
}
