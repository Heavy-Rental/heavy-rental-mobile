package com.heavyrental.ui.screens

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
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
// Unlike DeliveryListScreenTest/ReturnListScreenTest (which only ever render a single list
// item), several tests here render 3-6 booking cards at once. Robolectric's default virtual
// screen (no qualifiers set) is too short for that many cards under the top bar + six-chip
// FlowRow, so LazyColumn never composes the ones that fall outside it — they're simply absent
// from the semantics tree, not merely off-screen. h891dp (a normal phone height) was enough
// for the 3-4 card tests but still too short for the 6-status test, so this uses a much taller
// virtual screen — purely a test fixture, not a real device size, so there's no downside to
// making it generous enough that card count will never again be the constraint.
@Config(sdk = [33], qualifiers = "w411dp-h2000dp")
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
        // "Confirmed" matches both the filter chip and this booking's status badge —
        // .onLast() is the badge (chips render before the list in the tree).
        composeTestRule.onAllNodesWithText("Confirmed").onLast().assertIsDisplayed()
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

        // Six cards is enough that whether they all fit one screen's worth of LazyColumn
        // composition is not portable — it depends on exact text/line-height measurement,
        // which Robolectric's NATIVE graphics mode doesn't guarantee identically across
        // host platforms (this passed locally on Windows but failed in CI on Linux at a
        // fixed virtual screen size). Scrolling the list to each node before asserting on
        // it sidesteps that entirely: it works regardless of viewport size or how many
        // cards happen to already be composed.
        val list = composeTestRule.onNode(hasScrollAction())

        list.performScrollToNode(hasText("Pending Deposit"))
        composeTestRule.onNodeWithText("Pending Deposit").assertIsDisplayed()

        list.performScrollToNode(hasText("Pending Confirmation"))
        composeTestRule.onNodeWithText("Pending Confirmation").assertIsDisplayed()

        // "Confirmed", "Mobilised", "Completed", and "Cancelled" also label filter chips, so
        // two nodes match each — .onLast() is the card's status badge (chips render before
        // the list in the tree). "Pending Deposit"/"Pending Confirmation" have no such
        // collision: the chip that covers both is labelled just "Pending". performScrollToNode
        // only searches descendants of the list, so it can't be confused by the chip either.
        list.performScrollToNode(hasText("Confirmed"))
        composeTestRule.onAllNodesWithText("Confirmed").onLast().assertIsDisplayed()

        list.performScrollToNode(hasText("Mobilised"))
        composeTestRule.onAllNodesWithText("Mobilised").onLast().assertIsDisplayed()

        list.performScrollToNode(hasText("Completed"))
        composeTestRule.onAllNodesWithText("Completed").onLast().assertIsDisplayed()

        list.performScrollToNode(hasText("Cancelled"))
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
    fun `Pending filter groups PENDING_DEPOSIT and PENDING_CONFIRMED, excludes everything else`() {
        composeTestRule.setContent {
            CustomerBookingsScreen(
                customerName = "Alex",
                bookings = listOf(
                    booking(1L, BookingStatus.PENDING_DEPOSIT),
                    booking(2L, BookingStatus.PENDING_CONFIRMED),
                    booking(3L, BookingStatus.CONFIRMED),
                    booking(4L, BookingStatus.MOBILISED)
                ),
                onLogout = {}
            )
        }

        composeTestRule.onNodeWithText("Pending").performClick()

        composeTestRule.onNodeWithText("Booking #1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Booking #2").assertIsDisplayed()
        composeTestRule.onNodeWithText("Booking #3").assertDoesNotExist()
        composeTestRule.onNodeWithText("Booking #4").assertDoesNotExist()
    }

    @Test
    fun `Confirmed filter excludes Mobilised, and vice versa`() {
        composeTestRule.setContent {
            CustomerBookingsScreen(
                customerName = "Alex",
                bookings = listOf(
                    booking(1L, BookingStatus.CONFIRMED),
                    booking(2L, BookingStatus.MOBILISED)
                ),
                onLogout = {}
            )
        }

        // "Confirmed" matches the chip and booking #1's badge — .onFirst() is the chip.
        composeTestRule.onAllNodesWithText("Confirmed").onFirst().performClick()
        composeTestRule.onNodeWithText("Booking #1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Booking #2").assertDoesNotExist()

        composeTestRule.onAllNodesWithText("Mobilised").onFirst().performClick()
        composeTestRule.onNodeWithText("Booking #2").assertIsDisplayed()
        composeTestRule.onNodeWithText("Booking #1").assertDoesNotExist()
    }

    @Test
    fun `bookings render in descending bookingId order, regardless of input order`() {
        composeTestRule.setContent {
            CustomerBookingsScreen(
                customerName = "Alex",
                // Deliberately unsorted input — the screen is responsible for ordering,
                // not whatever order GET /api/bookings happens to return.
                bookings = listOf(
                    booking(5L, BookingStatus.CONFIRMED),
                    booking(80L, BookingStatus.PENDING_CONFIRMED),
                    booking(23L, BookingStatus.MOBILISED)
                ),
                onLogout = {}
            )
        }

        val cards = composeTestRule.onAllNodesWithText("Booking #", substring = true)
        cards.assertCountEquals(3)
        cards[0].assertTextEquals("Booking #80")
        cards[1].assertTextEquals("Booking #23")
        cards[2].assertTextEquals("Booking #5")
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
