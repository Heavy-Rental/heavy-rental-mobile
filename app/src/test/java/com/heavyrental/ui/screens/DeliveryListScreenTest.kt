package com.heavyrental.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.heavyrental.data.models.AssetLine
import com.heavyrental.data.models.BookingStatus
import com.heavyrental.data.models.DeliveryItem
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

/**
 * Covers K3 in specification/product/03-deliveries.md: a blank `AssetLine.assetName`/
 * `serialNumber` renders as "Asset not specified" / "No serial number" placeholder text instead
 * of a blank heading. Also covers K1: a multi-item `items` list renders every asset, not just
 * the first. Runs under Robolectric so it's a JVM `./gradlew test`, no emulator required.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33])
class DeliveryListScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun item(id: Long, assetName: String, serialNumber: String) = DeliveryItem(
        bookingId = id,
        customerName = "Acme Co",
        startDate = LocalDate.now(),
        projectLocation = "123 Site Rd",
        items = listOf(AssetLine(assetName, serialNumber)),
        deliveryNotes = "",
        bookingStatus = BookingStatus.CONFIRMED
    )

    @Test
    fun `blank asset name and serial number render placeholder text`() {
        composeTestRule.setContent {
            DeliveryListScreen(
                deliveries = listOf(item(1L, assetName = "", serialNumber = "")),
                onStatusUpdate = {}
            )
        }

        composeTestRule.onNodeWithText("Asset not specified").assertIsDisplayed()
        composeTestRule.onNodeWithText("No serial number").assertIsDisplayed()
    }

    @Test
    fun `non-blank asset name and serial number render the real values, not placeholders`() {
        composeTestRule.setContent {
            DeliveryListScreen(
                deliveries = listOf(item(1L, assetName = "JLG 460SJ Boom Lift", serialNumber = "SN-42")),
                onStatusUpdate = {}
            )
        }

        composeTestRule.onNodeWithText("JLG 460SJ Boom Lift").assertIsDisplayed()
        composeTestRule.onNodeWithText("SN-42").assertIsDisplayed()
    }

    @Test
    fun `multiple assets on one card render every asset name and serial number`() {
        val multiItem = DeliveryItem(
            bookingId = 1L,
            customerName = "Acme Co",
            startDate = LocalDate.now(),
            projectLocation = "123 Site Rd",
            items = listOf(
                AssetLine("JLG 460SJ Boom Lift", "SN-1A"),
                AssetLine("Toyota 8FD25 Forklift", "SN-1B")
            ),
            deliveryNotes = "",
            bookingStatus = BookingStatus.CONFIRMED
        )

        composeTestRule.setContent {
            DeliveryListScreen(deliveries = listOf(multiItem), onStatusUpdate = {})
        }

        composeTestRule.onNodeWithText("JLG 460SJ Boom Lift").assertIsDisplayed()
        composeTestRule.onNodeWithText("SN-1A").assertIsDisplayed()
        composeTestRule.onNodeWithText("Toyota 8FD25 Forklift").assertIsDisplayed()
        composeTestRule.onNodeWithText("SN-1B").assertIsDisplayed()
    }

    @Test
    fun `empty items list renders placeholder text`() {
        val noItems = DeliveryItem(
            bookingId = 1L,
            customerName = "Acme Co",
            startDate = LocalDate.now(),
            projectLocation = "123 Site Rd",
            items = emptyList(),
            deliveryNotes = "",
            bookingStatus = BookingStatus.CONFIRMED
        )

        composeTestRule.setContent {
            DeliveryListScreen(deliveries = listOf(noItems), onStatusUpdate = {})
        }

        composeTestRule.onNodeWithText("Asset not specified").assertIsDisplayed()
        composeTestRule.onNodeWithText("No serial number").assertIsDisplayed()
    }

    @Test
    fun `confirmation dialog quotes the single asset name when there is one item`() {
        composeTestRule.setContent {
            DeliveryListScreen(
                deliveries = listOf(item(1L, assetName = "JLG 460SJ Boom Lift", serialNumber = "SN-42")),
                onStatusUpdate = {}
            )
        }

        composeTestRule.onNodeWithText("Mark as Mobilised").performScrollTo().performClick()

        composeTestRule.onNodeWithText("\"JLG 460SJ Boom Lift\"", substring = true).assertIsDisplayed()
    }

    @Test
    fun `confirmation dialog says all N assets when there are multiple items`() {
        val multiItem = DeliveryItem(
            bookingId = 1L,
            customerName = "Acme Co",
            startDate = LocalDate.now(),
            projectLocation = "123 Site Rd",
            items = listOf(
                AssetLine("JLG 460SJ Boom Lift", "SN-1A"),
                AssetLine("Toyota 8FD25 Forklift", "SN-1B")
            ),
            deliveryNotes = "",
            bookingStatus = BookingStatus.CONFIRMED
        )

        composeTestRule.setContent {
            DeliveryListScreen(deliveries = listOf(multiItem), onStatusUpdate = {})
        }

        composeTestRule.onNodeWithText("Mark as Mobilised").performScrollTo().performClick()

        composeTestRule.onNodeWithText("all 2 assets", substring = true).assertIsDisplayed()
    }
}
