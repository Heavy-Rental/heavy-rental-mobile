package com.heavyrental.data.models

import java.time.LocalDate

data class DeliveryItem(
    val bookingId: String,
    val customerName: String,
    val startDate: LocalDate,
    val projectLocation: String,

    val assetName: String,
    val serialNumber: String,
    val quantity: Int,

    val bookingStatus: BookingStatus
)
