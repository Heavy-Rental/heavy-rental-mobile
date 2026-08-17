package com.heavyrental

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.heavyrental.data.models.BookingStatus
import com.heavyrental.navigation.AppScreen
import com.heavyrental.ui.screens.DeliveryListScreen
import com.heavyrental.ui.screens.HomeScreen
import com.heavyrental.ui.screens.LoginScreen
import com.heavyrental.ui.screens.ReturnListScreen
import com.heavyrental.ui.theme.HeavyRentalTheme
import com.heavyrental.ui.theme.MutedForeground
import com.heavyrental.ui.theme.Primary
import com.heavyrental.ui.theme.Surface
import com.heavyrental.viewmodel.AppViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HeavyRentalTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    HeavyRentalApp()
                }
            }
        }
    }
}

@Composable
fun HeavyRentalApp() {
    val vm: AppViewModel = viewModel()
    val state      by vm.state.collectAsState()
    val deliveries by vm.deliveries.collectAsState()
    val returns    by vm.returns.collectAsState()
    val networkError by vm.networkError.collectAsState()

    // Re-fetch whenever login state flips to true (covers first login AND
    // any later re-login after logout) -- not just once at process start.
    LaunchedEffect(state.isLoggedIn) {
        if (state.isLoggedIn) {
            vm.loadData()
        }
    }

    if (!state.isLoggedIn) {
        LoginScreen(
            onLogin             = { email, password -> vm.login(email, password) },
            onGoogleLogin       = { idToken -> vm.loginWithGoogle(idToken) },
            onGoogleLoginFailed = { message -> vm.setLoginError(message) },
            loginError          = state.loginError,
            isLoggingIn         = state.isLoggingIn
        )
        return
    }

    Scaffold(
        topBar = {
            if (networkError != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(12.dp)
                ) {
                    Text(
                        text = networkError ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = Surface, tonalElevation = 0.dp) {
                NavigationBarItem(
                    selected = state.currentScreen == AppScreen.HOME,
                    onClick  = { vm.navigate(AppScreen.HOME) },
                    icon     = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label    = { Text("Home") },
                    colors   = tabColors()
                )
                NavigationBarItem(
                    selected = state.currentScreen == AppScreen.DELIVERIES,
                    onClick  = { vm.navigate(AppScreen.DELIVERIES) },
                    icon     = { Icon(Icons.Default.LocalShipping, contentDescription = "Deliveries") },
                    label    = { Text("Deliveries") },
                    colors   = tabColors()
                )
                NavigationBarItem(
                    selected = state.currentScreen == AppScreen.RETURNS,
                    onClick  = { vm.navigate(AppScreen.RETURNS) },
                    icon     = { Icon(Icons.Default.Replay, contentDescription = "Returns") },
                    label    = { Text("Returns") },
                    colors   = tabColors()
                )
            }
        }
    ) { innerPadding ->
        when (state.currentScreen) {
            AppScreen.HOME -> HomeScreen(
                adminName              = state.adminName,
                deliveryCount          = deliveries.size,
                returnCount            = returns.size,
                confirmedCount         = deliveries.count { it.bookingStatus == BookingStatus.CONFIRMED },
                mobilisedDeliveryCount = deliveries.count { it.bookingStatus == BookingStatus.MOBILISED },
                mobilisedReturnCount   = returns.count { it.bookingStatus == BookingStatus.MOBILISED },
                completedCount         = returns.count { it.bookingStatus == BookingStatus.COMPLETED },
                onLogout               = { vm.logout() },
                modifier               = Modifier.padding(innerPadding)
            )
            AppScreen.DELIVERIES -> DeliveryListScreen(
                deliveries     = deliveries,
                onStatusUpdate = { id -> vm.updateDeliveryStatus(id, BookingStatus.MOBILISED) },
                modifier       = Modifier.padding(innerPadding)
            )
            AppScreen.RETURNS -> ReturnListScreen(
                returns        = returns,
                onStatusUpdate = { id, notes -> vm.updateReturnStatus(id, BookingStatus.COMPLETED, notes) },
                modifier       = Modifier.padding(innerPadding)
            )
            AppScreen.LOGIN -> { /* unreachable */ }
        }
    }
}

@Composable
private fun tabColors() = NavigationBarItemDefaults.colors(
    selectedIconColor   = Primary,
    selectedTextColor   = Primary,
    unselectedIconColor = MutedForeground,
    unselectedTextColor = MutedForeground
)