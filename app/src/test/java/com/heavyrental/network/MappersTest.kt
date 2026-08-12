package com.heavyrental.network

import com.heavyrental.data.models.AssetLine
import com.heavyrental.data.models.BookingStatus
import com.heavyrental.network.dto.AssetLineDto
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

    private fun deliveryDto(items: List<AssetLineDto> = listOf(AssetLineDto("JLG 460SJ Boom Lift", "SN-1"))) =
        DeliveryItemDto(
            bookingId = 1L,
            customerName = "Acme Co",
            startDate = "2026-08-12",
            siteAddress = "123 Site Rd",
            items = items,
            deliveryNotes = null,
            bookingStatus = "CONFIRMED"
        )

    private fun returnDto(items: List<AssetLineDto> = listOf(AssetLineDto("JLG 460SJ Boom Lift", "SN-1"))) =
        ReturnItemDto(
            bookingId = 1L,
            customerName = "Acme Co",
            endDate = "2026-08-12",
            siteAddress = "123 Site Rd",
            items = items,
            deliveryNotes = null,
            returnNotes = null,
            bookingStatus = "MOBILISED"
        )

    @Test
    fun `empty items list passes through unchanged on delivery items`() {
        val item = deliveryDto(items = emptyList()).toDeliveryItem()

        assertEquals(emptyList<AssetLine>(), item.items)
    }

    @Test
    fun `empty items list passes through unchanged on return items`() {
        val item = returnDto(items = emptyList()).toReturnItem()

        assertEquals(emptyList<AssetLine>(), item.items)
    }

    @Test
    fun `blank assetName and serialNumber inside an item pass through unchanged`() {
        val item = deliveryDto(items = listOf(AssetLineDto("", ""))).toDeliveryItem()

        assertEquals(listOf(AssetLine("", "")), item.items)
    }

    @Test
    fun `multiple items pass through unchanged, in order`() {
        val dto = listOf(AssetLineDto("JLG 460SJ Boom Lift", "SN-1"), AssetLineDto("Toyota 8FD25 Forklift", "SN-2"))
        val item = deliveryDto(items = dto).toDeliveryItem()

        assertEquals(
            listOf(AssetLine("JLG 460SJ Boom Lift", "SN-1"), AssetLine("Toyota 8FD25 Forklift", "SN-2")),
            item.items
        )
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
