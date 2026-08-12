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
import com.heavyrental.data.models.ReturnItem
import com.heavyrental.ui.theme.*

private enum class ReturnFilter { ALL, MOBILISED, COMPLETED }

@Composable
fun ReturnListScreen(
    returns: List<ReturnItem>,
    onStatusUpdate: (id: Long, returnNotes: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf(ReturnFilter.ALL) }

    val visibleReturns = remember(returns, selectedFilter) {
        when (selectedFilter) {
            ReturnFilter.ALL       -> returns
            ReturnFilter.MOBILISED -> returns.filter { it.bookingStatus == BookingStatus.MOBILISED }
            ReturnFilter.COMPLETED -> returns.filter { it.bookingStatus == BookingStatus.COMPLETED }
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
                Text("Return List", style = MaterialTheme.typography.titleLarge, color = Foreground)
                Text(
                    "${visibleReturns.size} of ${returns.size} returns",
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
            ReturnFilterChip(
                label    = "Show All",
                count    = returns.size,
                selected = selectedFilter == ReturnFilter.ALL,
                color    = Foreground,
                onClick  = { selectedFilter = ReturnFilter.ALL }
            )
            ReturnFilterChip(
                label    = "Mobilised",
                count    = returns.count { it.bookingStatus == BookingStatus.MOBILISED },
                selected = selectedFilter == ReturnFilter.MOBILISED,
                color    = BlueAccent,
                onClick  = { selectedFilter = ReturnFilter.MOBILISED }
            )
            ReturnFilterChip(
                label    = "Completed",
                count    = returns.count { it.bookingStatus == BookingStatus.COMPLETED },
                selected = selectedFilter == ReturnFilter.COMPLETED,
                color    = GreenAccent,
                onClick  = { selectedFilter = ReturnFilter.COMPLETED }
            )
        }

        HorizontalDivider(color = Border)

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(visibleReturns, key = { it.bookingId }) { item ->
                ReturnCard(item, onStatusUpdate = onStatusUpdate)
            }
        }
    }
}

@Composable
private fun ReturnCard(
    item: ReturnItem,
    onStatusUpdate: (id: Long, returnNotes: String) -> Unit
) {
    val context = LocalContext.current
    var showConfirmDialog by remember { mutableStateOf(false) }
    // Draft note the driver is typing before confirming completion. Keyed on bookingId so it
    // resets correctly if the underlying list recomposes with a different item at this slot.
    var returnNoteInput by remember(item.bookingId) { mutableStateOf(item.returnNotes) }

    val (statusColor, statusLabel) = when (item.bookingStatus) {
        BookingStatus.MOBILISED -> Pair(BlueAccent,  "Mobilised")
        BookingStatus.COMPLETED -> Pair(GreenAccent, "Completed")
        else -> Color.Gray to "Unknown"
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            containerColor = Card,
            titleContentColor = Foreground,
            textContentColor = MutedForeground,
            title = { Text("Mark as Completed?", fontWeight = FontWeight.Bold) },
            text = {
                val assetDescription = if (item.items.size > 1) {
                    "all ${item.items.size} assets"
                } else {
                    "\"${item.items.firstOrNull()?.assetName.orEmpty()}\""
                }
                Text(
                    "Update $assetDescription status for ${item.customerName} from Mobilised to Completed. This cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onStatusUpdate(item.bookingId, returnNoteInput.trim())
                        showConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenAccent)
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("ID: ${item.bookingId}", style = MaterialTheme.typography.labelSmall, color = MutedForeground)
            StatusBadge(statusLabel, statusColor)
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (item.items.isEmpty()) {
            Text(
                "Asset not specified",
                style = MaterialTheme.typography.titleLarge,
                color = MutedForeground
            )
            Text(
                "No serial number",
                style = MaterialTheme.typography.bodySmall,
                color = MutedForeground
            )
        } else {
            item.items.forEachIndexed { index, line ->
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

        // Delivery-time note, kept for context (e.g. "airside access pass required").
        if (item.deliveryNotes.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Delivery note: ${item.deliveryNotes}",
                style = MaterialTheme.typography.bodySmall,
                color = BlueAccent,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Return-time note, only meaningful once one has actually been recorded (i.e. after
        // completion) — the editable draft while MOBILISED lives in the text field below.
        if (item.bookingStatus == BookingStatus.COMPLETED && item.returnNotes.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Return note: ${item.returnNotes}",
                style = MaterialTheme.typography.bodySmall,
                color = GreenAccent,
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
                val resolved = intent.resolveActivity(context.packageManager)
                if (resolved != null) {
                    context.startActivity(intent)
                } else {
                    // Fallback: open in browser
                    val fallback = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://maps.google.com/?q=${Uri.encode(item.projectLocation)}")
                    )
                    context.startActivity(fallback)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = BlueAccent),
            border = androidx.compose.foundation.BorderStroke(1.dp, BlueAccent.copy(alpha = 0.5f))
        ) {
            Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Open in Google Maps", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }

        // Return note input + status update button — only visible when status is MOBILISED
        if (item.bookingStatus == BookingStatus.MOBILISED) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = returnNoteInput,
                onValueChange = { returnNoteInput = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Return note (optional)") },
                placeholder = { Text("e.g. condition on collection, missing parts") },
                minLines = 2,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GreenAccent,
                    focusedLabelColor = GreenAccent
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { showConfirmDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GreenAccent)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Mark as Completed", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black)
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
private fun ReturnFilterChip(
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
