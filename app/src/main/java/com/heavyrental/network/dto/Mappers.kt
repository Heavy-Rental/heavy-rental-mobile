package com.heavyrental.network

import com.heavyrental.data.models.AssetLine
import com.heavyrental.data.models.Booking
import com.heavyrental.data.models.BookingStatus
import com.heavyrental.data.models.DeliveryItem
import com.heavyrental.data.models.ReturnItem
import com.heavyrental.network.dto.AssetLineDto
import com.heavyrental.network.dto.BookingDto
import com.heavyrental.network.dto.DeliveryItemDto
import com.heavyrental.network.dto.ReturnItemDto
import java.time.LocalDate

fun AssetLineDto.toAssetLine(): AssetLine = AssetLine(
    assetName = assetName,
    serialNumber = serialNumber
)

fun BookingDto.toBooking(): Booking = Booking(
    bookingId = bookingId,
    customerName = customerName.orEmpty(),
    startDate = startDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: LocalDate.now(),
    endDate = endDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: LocalDate.now(),
    bookingStatus = bookingStatus?.let { runCatching { BookingStatus.valueOf(it) }.getOrNull() },
    projectLocation = siteAddress.orEmpty(),
    items = items.map { it.toAssetLine() },
    deliveryNotes = deliveryNotes.orEmpty()
)

fun DeliveryItemDto.toDeliveryItem(): DeliveryItem = DeliveryItem(
    bookingId = bookingId,
    customerName = customerName.orEmpty(),
    startDate = startDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: LocalDate.now(),
    projectLocation = siteAddress.orEmpty(),
    items = items.map { it.toAssetLine() },
    deliveryNotes = deliveryNotes.orEmpty(),
    bookingStatus = bookingStatus?.let { runCatching { BookingStatus.valueOf(it) }.getOrNull() }
)

fun ReturnItemDto.toReturnItem(): ReturnItem = ReturnItem(
    bookingId = bookingId,
    customerName = customerName.orEmpty(),
    endDate = endDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: LocalDate.now(),
    projectLocation = siteAddress.orEmpty(),
    items = items.map { it.toAssetLine() },
    deliveryNotes = deliveryNotes.orEmpty(),
    returnNotes = returnNotes.orEmpty(),
    bookingStatus = bookingStatus?.let { runCatching { BookingStatus.valueOf(it) }.getOrNull() }
)
