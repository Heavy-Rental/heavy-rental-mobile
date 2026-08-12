package com.heavyrental.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
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
 * Covers K3 in specification/product/03-deliveries.md: blank assetName/serialNumber render as
 * "Asset not specified" / "No serial number" placeholder text instead of a blank heading.
 * Runs under Robolectric so it's a JVM `./gradlew test`, no emulator required.
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
        assetName = assetName,
        serialNumber = serialNumber,
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
}
