package com.heavyrental.data.repository

import com.heavyrental.data.models.*
import java.time.LocalDate

object MockDataRepository {

    private val today: LocalDate = LocalDate.now()

    val bookingList: List<Booking> = listOf(
        Booking(
            bookingId = "DLV-001",
            customerName = "Lim Construction Pte Ltd",
            assetName = "CAT 320 Hydraulic Excavator",
            serialNumber = "CAT 320 GC",
            quantity = 1,
            projectLocation = "18 Tuas South Ave 14, Singapore 637471",
            startDate = today,
            endDate = today.plusDays(7),
            bookingStatus = BookingStatus.MOBILISED
        ),
        Booking(
            bookingId = "DLV-002",
            customerName = "Sembcorp Marine Ltd",
            assetName = "Komatsu D65 Bulldozer",
            serialNumber = "D65EX-18",
            quantity = 1,
            projectLocation = "80 Tuas South Blvd, Singapore 637051",
            startDate = today,
            endDate = today.plusDays(7),
            bookingStatus = BookingStatus.MOBILISED
        ),
        Booking(
            bookingId = "DLV-003",
            customerName = "Jurong Port Pte Ltd",
            assetName = "JLG 1350SJP Telescopic Boom",
            serialNumber = "1350SJP",
            quantity = 2,
            projectLocation = "Jurong Port Road, Singapore 619110",
            startDate = today,
            endDate = today.plusDays(7),
            bookingStatus = BookingStatus.CONFIRMED
        ),
        Booking(
            bookingId = "DLV-004",
            customerName = "Jurong Port Pte Ltd",
            assetName = "JLG 1350SJP Telescopic Boom",
            serialNumber = "1350SJP",
            quantity = 3,
            projectLocation = "Jurong Port Road, Singapore 619110",
            startDate = today,
            endDate = today.plusDays(7),
            bookingStatus = BookingStatus.CONFIRMED
        ),
        Booking(
            bookingId = "DLV-005",
            customerName = "Sembcorp Marine Ltd",
            assetName = "Komatsu D65 Bulldozer",
            serialNumber = "D65EX-18",
            quantity = 3,
            projectLocation = "80 Tuas South Blvd, Singapore 637051",
            startDate = today,
            endDate = today.plusDays(7),
            bookingStatus = BookingStatus.MOBILISED
        ),
        Booking(
            bookingId = "DLV-006",
            customerName = "Tiong Seng Contractors",
            assetName = "Volvo EC480E Excavator",
            serialNumber = "EC480E L",
            quantity = 1,
            projectLocation = "1 Woodlands Industrial Park E1, Singapore 757700",
            startDate = today,
            endDate = today.plusDays(7),
            bookingStatus = BookingStatus.CONFIRMED
        ),
        Booking(
            bookingId = "RET-001",
            customerName = "Keppel Infrastructure",
            assetName = "Liebherr LTM 1100 Mobile Crane",
            serialNumber = "LTM 1100-5.2",
            quantity = 1,
            projectLocation = "1 Maritime Square, Harbourfront, Singapore 099253",
            startDate = today.minusDays(7),
            endDate = today,
            bookingStatus = BookingStatus.COMPLETED
        ),
        Booking(
            bookingId = "RET-002",
            customerName = "Changi Airport Group",
            assetName = "Toyota 8FBE15 Electric Forklift",
            serialNumber = "8FBE15",
            quantity = 3,
            projectLocation = "Airport Logistics Park, 11 Airport Cargo Road, Singapore 819466",
            startDate = today.minusDays(7),
            endDate = today,
            bookingStatus = BookingStatus.MOBILISED
        ),
        Booking(
            bookingId = "RET-003",
            customerName = "Changi Airport Group",
            assetName = "Toyota 8FBE15 Electric Forklift",
            serialNumber = "8FBE15",
            quantity = 5,
            projectLocation = "Airport Logistics Park, 11 Airport Cargo Road, Singapore 819466",
            startDate = today.minusDays(7),
            endDate = today,
            bookingStatus = BookingStatus.MOBILISED
        ),
        Booking(
            bookingId = "RET-004",
            customerName = "Poh Tiong Choon Logistics",
            assetName = "CAT 320 Hydraulic Excavator",
            serialNumber = "CAT 320 GC",
            quantity = 1,
            projectLocation = "5 Pandan Road, Singapore 609254",
            startDate = today.minusDays(7),
            endDate = today,
            bookingStatus = BookingStatus.MOBILISED
        )
    )
}
