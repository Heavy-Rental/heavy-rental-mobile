package com.heavyrental.network

import com.heavyrental.data.models.Booking
import com.heavyrental.data.models.BookingStatus
import com.heavyrental.data.models.DeliveryItem
import com.heavyrental.data.models.ReturnItem
import com.heavyrental.network.dto.BookingDto
import com.heavyrental.network.dto.DeliveryItemDto
import com.heavyrental.network.dto.ReturnItemDto
import java.time.LocalDate

fun BookingDto.toBooking(): Booking = Booking(
    bookingId = bookingId,
    customerName = customerName,
    startDate = LocalDate.parse(startDate),
    endDate = LocalDate.parse(endDate),
    bookingStatus = BookingStatus.valueOf(bookingStatus),
    siteAddress = siteAddress,
    assetName = assetName,
    serialNumber = serialNumber,
    deliveryNotes = deliveryNotes
)

fun DeliveryItemDto.toDeliveryItem(): DeliveryItem = DeliveryItem(
    bookingId = bookingId,
    customerName = customerName,
    startDate = LocalDate.parse(startDate),
    siteAddress = siteAddress,
    assetName = assetName,
    serialNumber = serialNumber,
    deliveryNotes = deliveryNotes,
    bookingStatus = BookingStatus.valueOf(bookingStatus)
)

fun ReturnItemDto.toReturnItem(): ReturnItem = ReturnItem(
    bookingId = bookingId,
    customerName = customerName,
    endDate = LocalDate.parse(endDate),
    siteAddress = siteAddress,
    assetName = assetName,
    serialNumber = serialNumber,
    deliveryNotes = deliveryNotes,
    bookingStatus = BookingStatus.valueOf(bookingStatus)
)