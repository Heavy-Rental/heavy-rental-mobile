package com.heavyrental.navigation

enum class AppScreen {
    LOGIN,
    HOME,
    DELIVERIES,
    RETURNS,

    /** Customer-only destination: read-only list of the logged-in customer's own bookings. */
    CUSTOMER_BOOKINGS
}