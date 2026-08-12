package com.heavyrental.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.heavyrental.data.models.BookingStatus
import com.heavyrental.data.models.ReturnItem
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

/**
 * Covers K3 in specification/product/03-deliveries.md ("Applies equally to the return list —
 * ReturnCard uses the identical ifBlank {} pattern"). Runs under Robolectric so it's a JVM
 * `./gradlew test`, no emulator required.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33])
class ReturnListScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun item(id: Long, assetName: String, serialNumber: String) = ReturnItem(
        bookingId = id,
        customerName = "Acme Co",
        endDate = LocalDate.now(),
        projectLocation = "123 Site Rd",
        assetName = assetName,
        serialNumber = serialNumber,
        deliveryNotes = "",
        returnNotes = "",
        bookingStatus = BookingStatus.MOBILISED
    )

    @Test
    fun `blank asset name and serial number render placeholder text`() {
        composeTestRule.setContent {
            ReturnListScreen(
                returns = listOf(item(1L, assetName = "", serialNumber = "")),
                onStatusUpdate = { _, _ -> }
            )
        }

        composeTestRule.onNodeWithText("Asset not specified").assertIsDisplayed()
        composeTestRule.onNodeWithText("No serial number").assertIsDisplayed()
    }

    @Test
    fun `non-blank asset name and serial number render the real values, not placeholders`() {
        composeTestRule.setContent {
            ReturnListScreen(
                returns = listOf(item(1L, assetName = "Toyota 8FD25 Forklift", serialNumber = "SN-77")),
                onStatusUpdate = { _, _ -> }
            )
        }

        composeTestRule.onNodeWithText("Toyota 8FD25 Forklift").assertIsDisplayed()
        composeTestRule.onNodeWithText("SN-77").assertIsDisplayed()
    }
}
