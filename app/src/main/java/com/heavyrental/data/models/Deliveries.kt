package com.heavyrental.data.models

import java.time.LocalDate

data class DeliveryItem(
    val bookingId: Long,
    val customerName: String,
    val startDate: LocalDate,
    val projectLocation: String,

    val items: List<AssetLine>,
    val deliveryNotes: String,

    val bookingStatus: BookingStatus?
)
