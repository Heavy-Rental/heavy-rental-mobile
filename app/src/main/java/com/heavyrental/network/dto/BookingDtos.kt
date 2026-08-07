package com.heavyrental.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class BookingDto(
    val bookingId: Long,
    val customerName: String,
    val startDate: String,   // ISO-8601, e.g. "2026-08-03" — parsed to LocalDate in the mapper
    val endDate: String,
    val bookingStatus: String,
    val siteAddress: String,
    val assetName: String,
    val serialNumber: String,
    val deliveryNotes: String
)

@Serializable
data class DeliveryItemDto(
    val bookingId: Long,
    val customerName: String,
    val startDate: String,
    val siteAddress: String,
    val assetName: String,
    val serialNumber: String,
    val deliveryNotes: String,
    val bookingStatus: String
)

@Serializable
data class ReturnItemDto(
    val bookingId: Long,
    val customerName: String,
    val endDate: String,
    val siteAddress: String,
    val assetName: String,
    val serialNumber: String,
    val deliveryNotes: String,
    val bookingStatus: String
)

@Serializable
data class StatusUpdateRequest(
    val bookingStatus: String
)