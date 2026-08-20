package com.heavyrental.ui.screens

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.heavyrental.data.models.AssetLine
import com.heavyrental.data.models.Booking
import com.heavyrental.data.models.BookingStatus
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

/**
 * Covers the customer-login-bookings-view feature: a read-only booking list for a ROLE_USER
 * session, sourced from GET /api/bookings. The defining property this suite checks is the
 * *absence* of any edit/status-update affordance — unlike DeliveryListScreen/ReturnListScreen,
 * this screen must never offer a way to mutate a booking.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33])
class CustomerBookingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun booking(
        id: Long,
        status: BookingStatus?,
        assetName: String = "JLG 460SJ Boom Lift",
        serialNumber: String = "SN-$id"
    ) = Booking(
        bookingId = id,
        customerName = "Alex Tan",
        startDate = LocalDate.now(),
        endDate = LocalDate.now().plusDays(3),
        bookingStatus = status,
        projectLocation = "20 Jurong Port Road, Singapore 619094",
        items = listOf(AssetLine(assetName, serialNumber)),
        deliveryNotes = ""
    )

    @Test
    fun `booking card renders asset, status, and site address`() {
        composeTestRule.setContent {
            CustomerBookingsScreen(
                customerName = "Alex",
                bookings = listOf(booking(1L, BookingStatus.CONFIRMED)),
                onLogout = {}
            )
        }

        composeTestRule.onNodeWithText("Booking #1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Confirmed").assertIsDisplayed()
        composeTestRule.onNodeWithText("JLG 460SJ Boom Lift").assertIsDisplayed()
        composeTestRule.onNodeWithText("20 Jurong Port Road, Singapore 619094").assertIsDisplayed()
    }

    @Test
    fun `no edit or status-update controls are ever rendered`() {
        composeTestRule.setContent {
            CustomerBookingsScreen(
                customerName = "Alex",
                bookings = listOf(
                    booking(1L, BookingStatus.CONFIRMED),
                    booking(2L, BookingStatus.MOBILISED),
                    booking(3L, BookingStatus.PENDING_DEPOSIT)
                ),
                onLogout = {}
            )
        }

        // These are the exact button labels DeliveryListScreen/ReturnListScreen use for
        // their mutating actions — this screen must not offer any of them. (Deliberately
        // not asserting on bare "Complete"/"Completed" substrings — this screen's own
        // "Completed" filter chip and status badge legitimately contain that text.)
        composeTestRule.onNodeWithText("Mark as Mobilised", substring = true).assertDoesNotExist()
        composeTestRule.onNodeWithText("Mark as Completed", substring = true).assertDoesNotExist()
        composeTestRule.onNodeWithText("Return note", substring = true).assertDoesNotExist()
        composeTestRule.onNodeWithText("Save", substring = true).assertDoesNotExist()
    }

    @Test
    fun `every booking status renders a distinct, non-blank badge`() {
        val statuses = listOf(
            BookingStatus.PENDING_DEPOSIT,
            BookingStatus.PENDING_CONFIRMED,
            BookingStatus.CONFIRMED,
            BookingStatus.MOBILISED,
            BookingStatus.COMPLETED,
            BookingStatus.CANCELLED
        )

        composeTestRule.setContent {
            CustomerBookingsScreen(
                customerName = "Alex",
                bookings = statuses.mapIndexed { index, status -> booking(index.toLong(), status) },
                onLogout = {}
            )
        }

        composeTestRule.onNodeWithText("Pending Deposit").assertIsDisplayed()
        composeTestRule.onNodeWithText("Pending Confirmation").assertIsDisplayed()
        composeTestRule.onNodeWithText("Confirmed").assertIsDisplayed()
        composeTestRule.onNodeWithText("Mobilised").assertIsDisplayed()
        // "Completed" and "Cancelled" also label filter chips, so two nodes match each —
        // .onLast() is the card's status badge (chips render before the list in the tree).
        composeTestRule.onAllNodesWithText("Completed").onLast().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Cancelled").onLast().assertIsDisplayed()
    }

    @Test
    fun `null status renders grey Unknown badge, matching the app-wide convention`() {
        composeTestRule.setContent {
            CustomerBookingsScreen(
                customerName = "Alex",
                bookings = listOf(booking(1L, status = null)),
                onLogout = {}
            )
        }

        composeTestRule.onNodeWithText("Unknown").assertIsDisplayed()
    }

    @Test
    fun `empty booking list shows a friendly empty state, not a blank screen`() {
        composeTestRule.setContent {
            CustomerBookingsScreen(customerName = "Alex", bookings = emptyList(), onLogout = {})
        }

        composeTestRule.onNodeWithText("You don't have any bookings yet").assertIsDisplayed()
    }

    @Test
    fun `Cancelled filter narrows the list to only cancelled bookings`() {
        composeTestRule.setContent {
            CustomerBookingsScreen(
                customerName = "Alex",
                bookings = listOf(
                    booking(1L, BookingStatus.CONFIRMED),
                    booking(2L, BookingStatus.CANCELLED)
                ),
                onLogout = {}
            )
        }

        // "Cancelled" matches both the filter chip and booking #2's status badge —
        // .onFirst() is the chip (it renders before the list in the tree).
        composeTestRule.onAllNodesWithText("Cancelled").onFirst().performClick()

        composeTestRule.onNodeWithText("Booking #2").assertIsDisplayed()
        composeTestRule.onNodeWithText("Booking #1").assertDoesNotExist()
    }

    @Test
    fun `logout button invokes the callback`() {
        var loggedOut = false

        composeTestRule.setContent {
            CustomerBookingsScreen(
                customerName = "Alex",
                bookings = emptyList(),
                onLogout = { loggedOut = true }
            )
        }

        composeTestRule.onNodeWithContentDescription("Logout").performClick()

        assertTrue(loggedOut)
    }
}
