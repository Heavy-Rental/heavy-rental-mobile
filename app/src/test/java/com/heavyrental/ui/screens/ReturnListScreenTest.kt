package com.heavyrental.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.heavyrental.data.models.AssetLine
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
        items = listOf(AssetLine(assetName, serialNumber)),
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

    @Test
    fun `multiple assets on one card render every asset name and serial number`() {
        val multiItem = ReturnItem(
            bookingId = 1L,
            customerName = "Acme Co",
            endDate = LocalDate.now(),
            projectLocation = "123 Site Rd",
            items = listOf(
                AssetLine("JLG 460SJ Boom Lift", "SN-1A"),
                AssetLine("Toyota 8FD25 Forklift", "SN-1B")
            ),
            deliveryNotes = "",
            returnNotes = "",
            bookingStatus = BookingStatus.MOBILISED
        )

        composeTestRule.setContent {
            ReturnListScreen(returns = listOf(multiItem), onStatusUpdate = { _, _ -> })
        }

        composeTestRule.onNodeWithText("JLG 460SJ Boom Lift").assertIsDisplayed()
        composeTestRule.onNodeWithText("SN-1A").assertIsDisplayed()
        composeTestRule.onNodeWithText("Toyota 8FD25 Forklift").assertIsDisplayed()
        composeTestRule.onNodeWithText("SN-1B").assertIsDisplayed()
    }

    @Test
    fun `empty items list renders placeholder text`() {
        val noItems = ReturnItem(
            bookingId = 1L,
            customerName = "Acme Co",
            endDate = LocalDate.now(),
            projectLocation = "123 Site Rd",
            items = emptyList(),
            deliveryNotes = "",
            returnNotes = "",
            bookingStatus = BookingStatus.MOBILISED
        )

        composeTestRule.setContent {
            ReturnListScreen(returns = listOf(noItems), onStatusUpdate = { _, _ -> })
        }

        composeTestRule.onNodeWithText("Asset not specified").assertIsDisplayed()
        composeTestRule.onNodeWithText("No serial number").assertIsDisplayed()
    }

    @Test
    fun `confirmation dialog quotes the single asset name when there is one item`() {
        composeTestRule.setContent {
            ReturnListScreen(
                returns = listOf(item(1L, assetName = "Toyota 8FD25 Forklift", serialNumber = "SN-77")),
                onStatusUpdate = { _, _ -> }
            )
        }

        composeTestRule.onNodeWithText("Mark as Completed").performScrollTo().performClick()

        composeTestRule.onNodeWithText("\"Toyota 8FD25 Forklift\"", substring = true).assertIsDisplayed()
    }

    @Test
    fun `confirmation dialog says all N assets when there are multiple items`() {
        val multiItem = ReturnItem(
            bookingId = 1L,
            customerName = "Acme Co",
            endDate = LocalDate.now(),
            projectLocation = "123 Site Rd",
            items = listOf(
                AssetLine("JLG 460SJ Boom Lift", "SN-1A"),
                AssetLine("Toyota 8FD25 Forklift", "SN-1B")
            ),
            deliveryNotes = "",
            returnNotes = "",
            bookingStatus = BookingStatus.MOBILISED
        )

        composeTestRule.setContent {
            ReturnListScreen(returns = listOf(multiItem), onStatusUpdate = { _, _ -> })
        }

        composeTestRule.onNodeWithText("Mark as Completed").performScrollTo().performClick()

        composeTestRule.onNodeWithText("all 2 assets", substring = true).assertIsDisplayed()
    }
}
