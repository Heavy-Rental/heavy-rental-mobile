package com.heavyrental.data.models

/**
 * One physical asset on a booking. A Booking may have more than one AssetLine (e.g. a boom lift
 * and a forklift on the same job) — see specification/product/03-deliveries.md K1.
 */
data class AssetLine(
    val assetName: String,
    val serialNumber: String,
)
