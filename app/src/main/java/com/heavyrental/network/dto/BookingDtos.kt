package com.heavyrental.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class BookingDto(
    val bookingId: String,
    val customerName: String,
    val startDate: String,   // ISO-8601, e.g. "2026-08-03" — parsed to LocalDate in the mapper
    val endDate: String,
    val bookingStatus: String,
    val projectLocation: String,
    val assetName: String,
    val serialNumber: String,
    val quantity: Int
)

@Serializable
data class DeliveryItemDto(
    val bookingId: String,
    val customerName: String,
    val startDate: String,
    val projectLocation: String,
    val assetName: String,
    val serialNumber: String,
    val quantity: Int,
    val bookingStatus: String
)

@Serializable
data class ReturnItemDto(
    val bookingId: String,
    val customerName: String,
    val endDate: String,
    val projectLocation: String,
    val assetName: String,
    val serialNumber: String,
    val quantity: Int,
    val bookingStatus: String
)

@Serializable
data class StatusUpdateRequest(
    val bookingStatus: String
)