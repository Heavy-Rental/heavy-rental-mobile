package com.heavyrental.data.models

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * Covers `Booking.toDeliveryItem()`/`toReturnItem()` in Bookings.kt: since HR-113, `items` is
 * carried through as-is rather than flattened to a single assetName/serialNumber pair — see K1 in
 * specification/product/03-deliveries.md.
 */
class BookingsTest {

    private fun booking(items: List<AssetLine>) = Booking(
        bookingId = 1L,
        customerName = "Acme Co",
        startDate = LocalDate.now(),
        endDate = LocalDate.now(),
        bookingStatus = BookingStatus.CONFIRMED,
        projectLocation = "123 Site Rd",
        items = items,
        deliveryNotes = "Handle with care"
    )

    @Test
    fun `toDeliveryItem passes multiple items through unchanged, in order`() {
        val items = listOf(AssetLine("JLG 460SJ Boom Lift", "SN-1A"), AssetLine("Toyota 8FD25 Forklift", "SN-1B"))

        val delivery = booking(items).toDeliveryItem()

        assertEquals(items, delivery.items)
    }

    @Test
    fun `toDeliveryItem passes an empty items list through unchanged`() {
        val delivery = booking(emptyList()).toDeliveryItem()

        assertEquals(emptyList<AssetLine>(), delivery.items)
    }

    @Test
    fun `toReturnItem passes multiple items through unchanged, in order`() {
        val items = listOf(AssetLine("JLG 460SJ Boom Lift", "SN-1A"), AssetLine("Toyota 8FD25 Forklift", "SN-1B"))

        val returnItem = booking(items).toReturnItem()

        assertEquals(items, returnItem.items)
        assertEquals("", returnItem.returnNotes)
    }

    @Test
    fun `toReturnItem passes an empty items list through unchanged`() {
        val returnItem = booking(emptyList()).toReturnItem()

        assertEquals(emptyList<AssetLine>(), returnItem.items)
    }
}
