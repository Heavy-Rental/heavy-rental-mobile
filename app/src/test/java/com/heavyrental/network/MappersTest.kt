package com.heavyrental.network

import com.heavyrental.data.models.BookingStatus
import com.heavyrental.network.dto.DeliveryItemDto
import com.heavyrental.network.dto.ReturnItemDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Confirms the split documented in specification/domain/booking-status-machine.md and
 * specification/product/03-deliveries.md K3: the mapper passes wire values straight through
 * (including blanks) rather than applying UI placeholder fallback text — that's a card-composable
 * concern, not a mapping concern.
 */
class MappersTest {

    private fun deliveryDto(assetName: String = "JLG 460SJ Boom Lift", serialNumber: String = "SN-1") =
        DeliveryItemDto(
            bookingId = 1L,
            customerName = "Acme Co",
            startDate = "2026-08-12",
            siteAddress = "123 Site Rd",
            assetName = assetName,
            serialNumber = serialNumber,
            deliveryNotes = null,
            bookingStatus = "CONFIRMED"
        )

    private fun returnDto(assetName: String = "JLG 460SJ Boom Lift", serialNumber: String = "SN-1") =
        ReturnItemDto(
            bookingId = 1L,
            customerName = "Acme Co",
            endDate = "2026-08-12",
            siteAddress = "123 Site Rd",
            assetName = assetName,
            serialNumber = serialNumber,
            deliveryNotes = null,
            returnNotes = null,
            bookingStatus = "MOBILISED"
        )

    @Test
    fun `blank assetName and serialNumber pass through unchanged on delivery items`() {
        val item = deliveryDto(assetName = "", serialNumber = "").toDeliveryItem()

        assertEquals("", item.assetName)
        assertEquals("", item.serialNumber)
    }

    @Test
    fun `blank assetName and serialNumber pass through unchanged on return items`() {
        val item = returnDto(assetName = "", serialNumber = "").toReturnItem()

        assertEquals("", item.assetName)
        assertEquals("", item.serialNumber)
    }

    @Test
    fun `non-blank assetName and serialNumber pass through unchanged`() {
        val item = deliveryDto().toDeliveryItem()

        assertEquals("JLG 460SJ Boom Lift", item.assetName)
        assertEquals("SN-1", item.serialNumber)
    }

    @Test
    fun `null wire status maps to null domain status`() {
        val item = deliveryDto().copy(bookingStatus = null).toDeliveryItem()

        assertNull(item.bookingStatus)
    }

    @Test
    fun `unrecognised wire status maps to null domain status rather than throwing`() {
        val item = deliveryDto().copy(bookingStatus = "SOME_FUTURE_STATUS").toDeliveryItem()

        assertNull(item.bookingStatus)
    }

    @Test
    fun `recognised wire status maps to the matching enum value`() {
        val item = returnDto().copy(bookingStatus = "COMPLETED").toReturnItem()

        assertEquals(BookingStatus.COMPLETED, item.bookingStatus)
    }
}
