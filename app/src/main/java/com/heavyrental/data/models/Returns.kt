package com.heavyrental.data.models

import java.time.LocalDate

data class ReturnItem(
    val bookingId: Long,
    val customerName: String,
    val endDate: LocalDate,
    val projectLocation: String,

    val items: List<AssetLine>,
    val deliveryNotes: String,
    val returnNotes: String,

    val bookingStatus: BookingStatus?
)
