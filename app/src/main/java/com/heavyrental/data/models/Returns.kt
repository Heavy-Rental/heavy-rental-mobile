package com.heavyrental.data.models

import java.time.LocalDate

data class ReturnItem(
    val bookingId: String,
    val customerName: String,
    val endDate: LocalDate,
    val projectLocation: String,

    val assetName: String,
    val serialNumber: String,
    val quantity: Int,

    val bookingStatus: BookingStatus
)
