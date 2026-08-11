package com.heavyrental.data.repository

import com.heavyrental.data.models.*
import java.time.LocalDate

object MockDataRepository {

    private val today: LocalDate = LocalDate.now()

    val bookingList: List<Booking> = listOf(
        Booking(
            bookingId = 1L,
            customerName = "Lim Construction Pte Ltd",
            assetName = "CAT 320 Hydraulic Excavator",
            serialNumber = "CAT 320 GC",
            deliveryNotes = "",
            projectLocation = "18 Tuas South Ave 14, Singapore 637471",
            startDate = today,
            endDate = today.plusDays(7),
            bookingStatus = BookingStatus.MOBILISED
        ),
        Booking(
            bookingId = 2L,
            customerName = "Sembcorp Marine Ltd",
            assetName = "Komatsu D65 Bulldozer",
            serialNumber = "D65EX-18",
            deliveryNotes = "",
            projectLocation = "80 Tuas South Blvd, Singapore 637051",
            startDate = today,
            endDate = today.plusDays(7),
            bookingStatus = BookingStatus.MOBILISED
        ),
        Booking(
            bookingId = 3L,
            customerName = "Jurong Port Pte Ltd",
            assetName = "JLG 1350SJP Telescopic Boom",
            serialNumber = "1350SJP",
            deliveryNotes = "Two units on this booking — coordinate offload with site security",
            projectLocation = "Jurong Port Road, Singapore 619110",
            startDate = today,
            endDate = today.plusDays(7),
            bookingStatus = BookingStatus.CONFIRMED
        ),
        Booking(
            bookingId = 4L,
            customerName = "Jurong Port Pte Ltd",
            assetName = "JLG 1350SJP Telescopic Boom",
            serialNumber = "1350SJP",
            deliveryNotes = "",
            projectLocation = "Jurong Port Road, Singapore 619110",
            startDate = today,
            endDate = today.plusDays(7),
            bookingStatus = BookingStatus.CONFIRMED
        ),
        Booking(
            bookingId = 5L,
            customerName = "Sembcorp Marine Ltd",
            assetName = "Komatsu D65 Bulldozer",
            serialNumber = "D65EX-18",
            deliveryNotes = "",
            projectLocation = "80 Tuas South Blvd, Singapore 637051",
            startDate = today,
            endDate = today.plusDays(7),
            bookingStatus = BookingStatus.MOBILISED
        ),
        Booking(
            bookingId = 6L,
            customerName = "Tiong Seng Contractors",
            assetName = "Volvo EC480E Excavator",
            serialNumber = "EC480E L",
            deliveryNotes = "",
            projectLocation = "1 Woodlands Industrial Park E1, Singapore 757700",
            startDate = today,
            endDate = today.plusDays(7),
            bookingStatus = BookingStatus.CONFIRMED
        ),
        Booking(
            bookingId = 7L,
            customerName = "Keppel Infrastructure",
            assetName = "Liebherr LTM 1100 Mobile Crane",
            serialNumber = "LTM 1100-5.2",
            deliveryNotes = "",
            projectLocation = "1 Maritime Square, Harbourfront, Singapore 099253",
            startDate = today.minusDays(7),
            endDate = today,
            bookingStatus = BookingStatus.COMPLETED
        ),
        Booking(
            bookingId = 8L,
            customerName = "Changi Airport Group",
            assetName = "Toyota 8FBE15 Electric Forklift",
            serialNumber = "8FBE15",
            deliveryNotes = "Airside access pass required — confirm with site ops before arrival",
            projectLocation = "Airport Logistics Park, 11 Airport Cargo Road, Singapore 819466",
            startDate = today.minusDays(7),
            endDate = today,
            bookingStatus = BookingStatus.MOBILISED
        ),
        Booking(
            bookingId = 9L,
            customerName = "Changi Airport Group",
            assetName = "Toyota 8FBE15 Electric Forklift",
            serialNumber = "8FBE15",
            deliveryNotes = "",
            projectLocation = "Airport Logistics Park, 11 Airport Cargo Road, Singapore 819466",
            startDate = today.minusDays(7),
            endDate = today,
            bookingStatus = BookingStatus.MOBILISED
        ),
        Booking(
            bookingId = 10L,
            customerName = "Poh Tiong Choon Logistics",
            assetName = "CAT 320 Hydraulic Excavator",
            serialNumber = "CAT 320 GC",
            deliveryNotes = "",
            projectLocation = "5 Pandan Road, Singapore 609254",
            startDate = today.minusDays(7),
            endDate = today,
            bookingStatus = BookingStatus.MOBILISED
        )
    )
}
