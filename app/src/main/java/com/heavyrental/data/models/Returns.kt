package com.heavyrental.data.models

import java.time.LocalDate

data class ReturnItem(
    val bookingId: Long,
    val customerName: String,
    val endDate: LocalDate,
    val siteAddress: String,

    val assetName: String,
    val serialNumber: String,
    val deliveryNotes: String,

    val bookingStatus: BookingStatus
)
