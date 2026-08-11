package com.heavyrental.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.heavyrental.data.models.BookingStatus
import com.heavyrental.data.models.DeliveryItem
import com.heavyrental.ui.theme.*

private enum class DeliveryFilter { ALL, CONFIRMED, MOBILISED }

@Composable
fun DeliveryListScreen(
    deliveries: List<DeliveryItem>,
    onStatusUpdate: (id: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf(DeliveryFilter.ALL) }

    val visibleDeliveries = remember(deliveries, selectedFilter) {
        when (selectedFilter) {
            DeliveryFilter.ALL       -> deliveries
            DeliveryFilter.CONFIRMED -> deliveries.filter { it.bookingStatus == BookingStatus.CONFIRMED }
            DeliveryFilter.MOBILISED -> deliveries.filter { it.bookingStatus == BookingStatus.MOBILISED }
        }.sortedBy { it.customerName }
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Delivery List", style = MaterialTheme.typography.titleLarge, color = Foreground)
                Text(
                    "${visibleDeliveries.size} of ${deliveries.size} deliveries",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedForeground
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
            DeliveryFilterChip(
                label    = "Show All",
                count    = deliveries.size,
                selected = selectedFilter == DeliveryFilter.ALL,
                color    = Foreground,
                onClick  = { selectedFilter = DeliveryFilter.ALL }
            )
            DeliveryFilterChip(
                label    = "Confirmed",
                count    = deliveries.count { it.bookingStatus == BookingStatus.CONFIRMED },
                selected = selectedFilter == DeliveryFilter.CONFIRMED,
                color    = AmberAccent,
                onClick  = { selectedFilter = DeliveryFilter.CONFIRMED }
            )
            DeliveryFilterChip(
                label    = "Mobilised",
                count    = deliveries.count { it.bookingStatus == BookingStatus.MOBILISED },
                selected = selectedFilter == DeliveryFilter.MOBILISED,
                color    = BlueAccent,
                onClick  = { selectedFilter = DeliveryFilter.MOBILISED }
            )
        }

        HorizontalDivider(color = Border)

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(visibleDeliveries, key = { it.bookingId }) { item ->
                DeliveryCard(item, onStatusUpdate = onStatusUpdate)
            }
        }
    }
}

@Composable
private fun DeliveryCard(
    item: DeliveryItem,
    onStatusUpdate: (id: Long) -> Unit
) {
    val context = LocalContext.current
    var showConfirmDialog by remember { mutableStateOf(false) }

    val (statusColor, statusLabel) = when (item.bookingStatus) {
        BookingStatus.CONFIRMED -> Pair(AmberAccent, "Confirmed")
        BookingStatus.MOBILISED -> Pair(BlueAccent,  "Mobilised")
        else -> Color.Gray to "Unknown"
    }

    // Confirmation dialog before updating status
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            containerColor = Card,
            titleContentColor = Foreground,
            textContentColor = MutedForeground,
            title = { Text("Mark as Mobilised?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Update \"${item.assetName}\" for ${item.customerName} status from Confirmed to Mobilised. This cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onStatusUpdate(item.bookingId)
                        showConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Text("Confirm", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel", color = MutedForeground)
                }
            }
        )
    }

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
            Text("ID: ${item.bookingId}", style = MaterialTheme.typography.labelSmall, color = MutedForeground)
            StatusBadge(statusLabel, statusColor)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            item.assetName.ifBlank { "Asset not specified" },
            style = MaterialTheme.typography.titleLarge,
            color = if (item.assetName.isBlank()) MutedForeground else Foreground
        )
        Text(
            item.serialNumber.ifBlank { "No serial number" },
            style = MaterialTheme.typography.bodySmall,
            color = MutedForeground
        )

        if (item.deliveryNotes.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Note: ${item.deliveryNotes}",
                style = MaterialTheme.typography.bodySmall,
                color = Primary,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = Border)
        Spacer(modifier = Modifier.height(12.dp))

        InfoRow(icon = { Icon(Icons.Default.Person,     null, tint = MutedForeground, modifier = Modifier.size(15.dp)) }, text = item.customerName)
        Spacer(modifier = Modifier.height(4.dp))
        InfoRow(icon = { Icon(Icons.Default.LocationOn, null, tint = MutedForeground, modifier = Modifier.size(15.dp)) }, text = item.projectLocation)
        Spacer(modifier = Modifier.height(4.dp))

        Spacer(modifier = Modifier.height(12.dp))

        // Google Maps button
        OutlinedButton(
            onClick = {
                val uri = Uri.parse("geo:0,0?q=${Uri.encode(item.projectLocation)}")
                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                    setPackage("com.google.android.apps.maps")
                }
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                } else {
                    val fallback = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://maps.google.com/?q=${Uri.encode(item.projectLocation)}")
                    )
                    context.startActivity(fallback)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary),
            border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.5f))
        ) {
            Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Open in Google Maps", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }

        // Status update button — only visible when status is CONFIRMED
        if (item.bookingStatus == BookingStatus.CONFIRMED) {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { showConfirmDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BlueAccent)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Mark as Mobilised", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }
        }
    }
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
private fun DeliveryFilterChip(
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
        onClick      = onClick,
        color        = bgColor,
        shape        = RoundedCornerShape(20.dp),
        border       = androidx.compose.foundation.BorderStroke(1.dp, border)
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
