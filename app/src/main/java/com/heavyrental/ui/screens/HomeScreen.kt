package com.heavyrental.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.heavyrental.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    adminName: String,
    deliveryCount: Int,
    returnCount: Int,
    confirmedCount: Int,
    mobilisedDeliveryCount: Int,
    mobilisedReturnCount: Int,
    completedCount: Int,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy"))

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text(
                    "Heavy Rental",
                    color = Primary,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    "Welcome, $adminName",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Foreground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(today, style = MaterialTheme.typography.bodySmall, color = MutedForeground)
            }
            IconButton(onClick = onLogout) {
                Icon(
                    Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "Logout",
                    tint = MutedForeground
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = Border)
        Spacer(modifier = Modifier.height(28.dp))

        // Today's overview totals
        Text(
            "Today's Overview",
            style = MaterialTheme.typography.titleLarge,
            color = MutedForeground
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OverviewTile(
                modifier   = Modifier.weight(1f),
                icon       = Icons.Default.LocalShipping,
                iconColor  = Primary,
                value      = "$deliveryCount",
                label      = "Deliveries",
                accentColor = Primary
            )
            OverviewTile(
                modifier   = Modifier.weight(1f),
                icon       = Icons.Default.Replay,
                iconColor  = BlueAccent,
                value      = "$returnCount",
                label      = "Returns",
                accentColor = BlueAccent
            )
        }

        Spacer(modifier = Modifier.height(28.dp))
        HorizontalDivider(color = Border)
        Spacer(modifier = Modifier.height(28.dp))

        // Delivery breakdown
        SectionHeader(
            icon       = Icons.Default.LocalShipping,
            iconColor  = Primary,
            title      = "Deliveries"
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatusTile(
                modifier    = Modifier.weight(1f),
                count       = confirmedCount,
                label       = "Confirmed",
                color       = AmberAccent
            )
            StatusTile(
                modifier    = Modifier.weight(1f),
                count       = mobilisedDeliveryCount,
                label       = "Mobilised",
                color       = BlueAccent
            )
        }

        Spacer(modifier = Modifier.height(28.dp))
        HorizontalDivider(color = Border)
        Spacer(modifier = Modifier.height(28.dp))

        // Return breakdown
        SectionHeader(
            icon      = Icons.Default.Replay,
            iconColor = BlueAccent,
            title     = "Returns"
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatusTile(
                modifier = Modifier.weight(1f),
                count    = mobilisedReturnCount,
                label    = "Mobilised",
                color    = BlueAccent
            )
            StatusTile(
                modifier = Modifier.weight(1f),
                count    = completedCount,
                label    = "Completed",
                color    = GreenAccent
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun OverviewTile(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconColor: Color,
    value: String,
    label: String,
    accentColor: Color
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Card)
            .border(1.dp, Border, RoundedCornerShape(14.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(accentColor.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            value,
            fontWeight = FontWeight.Black,
            fontSize = 34.sp,
            color = accentColor,
            lineHeight = 34.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = MutedForeground)
    }
}

@Composable
private fun StatusTile(
    modifier: Modifier = Modifier,
    count: Int,
    label: String,
    color: Color
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.08f))
            .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(
            "$count",
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            color = color,
            lineHeight = 26.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = color.copy(alpha = 0.7f))
    }
}

@Composable
private fun SectionHeader(icon: ImageVector, iconColor: Color, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = Foreground)
    }
}
