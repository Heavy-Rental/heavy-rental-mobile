package com.heavyrental.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class BookingDto(
    val bookingId: Long,
    val customerName: String? = null,
    val startDate: String? = null,   // ISO-8601, e.g. "2026-08-03" — parsed to LocalDate in the mapper
    val endDate: String? = null,
    val bookingStatus: String? = null,
    val siteAddress: String? = null,
    val assetName: String,
    val serialNumber: String,
    val deliveryNotes: String? = null
)

@Serializable
data class DeliveryItemDto(
    val bookingId: Long,
    val customerName: String? = null,
    val startDate: String? = null,
    val siteAddress: String? = null,
    val assetName: String,
    val serialNumber: String,
    val deliveryNotes: String? = null,
    val bookingStatus: String? = null
)

@Serializable
data class ReturnItemDto(
    val bookingId: Long,
    val customerName: String? = null,
    val endDate: String? = null,
    val siteAddress: String? = null,
    val assetName: String,
    val serialNumber: String,
    val deliveryNotes: String? = null,
    val returnNotes: String? = null,
    val bookingStatus: String? = null
)

@Serializable
data class StatusUpdateRequest(
    val bookingStatus: String
)

@Serializable
data class ReturnStatusUpdateRequestDto(
    val bookingStatus: String,
    val returnNotes: String
)

@Serializable
data class BookingUpdateRequestDto(
    val startDate: String,
    val endDate: String,
    val siteAddress: String,
    val deliveryNotes: String
)
