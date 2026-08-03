package com.heavyrental.data.models

import java.time.LocalDate

data class Booking(
    val bookingId: String,
    val customerName: String,

    val startDate: LocalDate,
    val endDate: LocalDate,
    val bookingStatus: BookingStatus,
    val projectLocation: String,

    // Asset information
    val assetName: String,
    val serialNumber: String,
    val quantity: Int,
)

// ── Derived views ─────

fun Booking.toDeliveryItem(): DeliveryItem = DeliveryItem(
    bookingId = bookingId,
    customerName = customerName,
    startDate = startDate,
    projectLocation = projectLocation,
    assetName = assetName,
    serialNumber = serialNumber,
    quantity = quantity,
    bookingStatus = bookingStatus
)

fun Booking.toReturnItem(): ReturnItem = ReturnItem(
    bookingId = bookingId,
    customerName = customerName,
    endDate = endDate,
    projectLocation = projectLocation,
    assetName = assetName,
    serialNumber = serialNumber,
    quantity = quantity,
    bookingStatus = bookingStatus
)

/** Bookings relevant to the Deliveries screen: confirmed or mobilised. */
fun List<Booking>.toDeliveryItems(): List<DeliveryItem> =
    filter { it.startDate == LocalDate.now() && (it.bookingStatus == BookingStatus.CONFIRMED || it.bookingStatus == BookingStatus.MOBILISED) }
        .map { it.toDeliveryItem() }

/** Bookings relevant to the Returns screen: mobilised and awaiting return, or already completed. */
fun List<Booking>.toReturnItems(): List<ReturnItem> =
    filter { it.endDate == LocalDate.now() && (it.bookingStatus == BookingStatus.MOBILISED || it.bookingStatus == BookingStatus.COMPLETED) }
        .map { it.toReturnItem() }

