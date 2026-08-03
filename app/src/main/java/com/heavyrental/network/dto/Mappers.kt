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
    projectLocation = projectLocation,
    assetName = assetName,
    serialNumber = serialNumber,
    quantity = quantity
)

fun DeliveryItemDto.toDeliveryItem(): DeliveryItem = DeliveryItem(
    bookingId = bookingId,
    customerName = customerName,
    startDate = LocalDate.parse(startDate),
    projectLocation = projectLocation,
    assetName = assetName,
    serialNumber = serialNumber,
    quantity = quantity,
    bookingStatus = BookingStatus.valueOf(bookingStatus)
)

fun ReturnItemDto.toReturnItem(): ReturnItem = ReturnItem(
    bookingId = bookingId,
    customerName = customerName,
    endDate = LocalDate.parse(endDate),
    projectLocation = projectLocation,
    assetName = assetName,
    serialNumber = serialNumber,
    quantity = quantity,
    bookingStatus = BookingStatus.valueOf(bookingStatus)
)

