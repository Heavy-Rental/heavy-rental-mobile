# Domain: Delivery and return list filters

**Status:** Implemented (v1)  
**Code:** `data/models/Bookings.kt` — `toDeliveryItems()`, `toReturnItems()`  
**Date basis:** `java.time.LocalDate.now()` (device local timezone)

---

## Core entity: Booking

| Field | Type | Notes |
|-------|------|--------|
| `bookingId` | string | Stable id (e.g. `DLV-001`, `RET-002`) |
| `customerName` | string | Customer / site owner |
| `startDate` | date (ISO-8601) | Hire / delivery start |
| `endDate` | date (ISO-8601) | Hire / return end |
| `bookingStatus` | enum | See [booking-status-machine.md](booking-status-machine.md) |
| `projectLocation` | string | Address or site description |
| `assetName` | string | Equipment description |
| `serialNumber` | string | Model / serial label |
| `quantity` | int | Units on the booking |

Kotlin: `data class Booking` in `data/models/Bookings.kt`.  
Wire: OpenAPI `Booking` / DTO `BookingDto`.

---

## Derived views

### DeliveryItem

Projection of a booking for the Deliveries screen (`data/models/Deliveries.kt`):

- `bookingId`, `customerName`, `startDate`, `projectLocation`
- `assetName`, `serialNumber`, `quantity`, `bookingStatus`

### ReturnItem

Projection of a booking for the Returns screen (`data/models/Returns.kt`):

- `bookingId`, `customerName`, `endDate`, `projectLocation`
- `assetName`, `serialNumber`, `quantity`, `bookingStatus`

---

## Delivery list filter

**Include** booking `b` in today’s deliveries iff:

```text
b.startDate == today
AND b.bookingStatus ∈ { CONFIRMED, MOBILISED }
```

**Exclude:**

- Any booking with `startDate ≠ today`
- Status `COMPLETED` (even if start is today)

```gherkin
Scenario Outline: Delivery membership
  Given today is <today>
  And a booking with startDate <start> and status <status>
  Then it <membership> the delivery list

  Examples:
    | today      | start      | status    | membership |
    | 2026-08-03 | 2026-08-03 | CONFIRMED | is in      |
    | 2026-08-03 | 2026-08-03 | MOBILISED | is in      |
    | 2026-08-03 | 2026-08-03 | COMPLETED | is not in  |
    | 2026-08-03 | 2026-08-04 | CONFIRMED | is not in  |
    | 2026-08-03 | 2026-08-02 | MOBILISED | is not in  |
```

### UI sub-filters (Deliveries screen only)

Applied **after** domain membership:

| UI filter | Extra predicate |
|-----------|-----------------|
| All | none |
| Confirmed | `bookingStatus == CONFIRMED` |
| Mobilised | `bookingStatus == MOBILISED` |

Sort: `customerName` ascending (v1 UI).

---

## Return list filter

**Include** booking `b` in today’s returns iff:

```text
b.endDate == today
AND b.bookingStatus ∈ { MOBILISED, COMPLETED }
```

**Exclude:**

- Any booking with `endDate ≠ today`
- Status `CONFIRMED` (even if end is today — not yet mobilised)

```gherkin
Scenario Outline: Return membership
  Given today is <today>
  And a booking with endDate <end> and status <status>
  Then it <membership> the return list

  Examples:
    | today      | end        | status    | membership |
    | 2026-08-03 | 2026-08-03 | MOBILISED | is in      |
    | 2026-08-03 | 2026-08-03 | COMPLETED | is in      |
    | 2026-08-03 | 2026-08-03 | CONFIRMED | is not in  |
    | 2026-08-03 | 2026-08-04 | MOBILISED | is not in  |
    | 2026-08-03 | 2026-08-02 | COMPLETED | is not in  |
```

### UI sub-filters (Returns screen)

| UI filter | Extra predicate |
|-----------|-----------------|
| All | none |
| Mobilised | `bookingStatus == MOBILISED` |
| Completed | `bookingStatus == COMPLETED` |

Sort: `customerName` ascending (v1 UI).

---

## Home dashboard counts

Reuse the **domain** lists (not UI sub-filters):

| Count | Source |
|-------|--------|
| Delivery total | `toDeliveryItems().size` |
| Confirmed deliveries | delivery items where `CONFIRMED` |
| Mobilised deliveries | delivery items where `MOBILISED` |
| Return total | `toReturnItems().size` |
| Mobilised returns | return items where `MOBILISED` |
| Completed returns | return items where `COMPLETED` |

---

## Client vs API (v1)

| Concern | Owner |
|---------|--------|
| Membership filters above | **Client** after `GET /api/bookings` |
| `GET /api/deliveries` / `GET /api/returns` | Defined in OpenAPI for mocks/backend; **not required** for v1 client list rendering |

Mocks that implement list endpoints should still apply the same filter semantics so contract examples stay consistent.

---

## Example seed alignment

`MockDataRepository` uses `LocalDate.now()` for “today” so seed rows always match filters:

| Pattern | Dates | Statuses used |
|---------|-------|----------------|
| `DLV-*` | `startDate = today`, `endDate = today + 7` | `CONFIRMED` or `MOBILISED` |
| `RET-*` | `startDate = today - 7`, `endDate = today` | `MOBILISED` or `COMPLETED` |

JSON fixtures under `specification/api/examples/` use a fixed calendar day (`2026-08-03`) for stable fixtures. When testing list filters against those fixtures, treat `2026-08-03` as “today” or rewrite dates in the test setup.
