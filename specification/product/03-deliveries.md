# Feature: Today's deliveries

**Status:** Implemented (v1)  
**Screens:** `DeliveryListScreen`  
**Navigation:** `AppScreen.DELIVERIES`  
**Code root:** `com.heavyrental`

---

## Summary

Operators manage **equipment deliveries scheduled for today**. They can filter the list, open the project location on a map, and mark a **confirmed** booking as **mobilised** when the asset has been sent out.

---

## List membership

A booking appears on the delivery list when **all** of the following hold:

1. `startDate` is **today** (device local date)
2. `bookingStatus` is `CONFIRMED` **or** `MOBILISED`

Full rules: [domain/list-filters.md](../domain/list-filters.md).

---

## UI behaviour

### Header

- Title: **Delivery List**
- Subtitle: `{visible} of {total} deliveries` for the active filter

### Filters (client-side)

| Filter chip | Shows |
|-------------|--------|
| Show All | All delivery items |
| Confirmed | `bookingStatus == CONFIRMED` |
| Mobilised | `bookingStatus == MOBILISED` |

Default filter: **Show All**.  
Visible rows are sorted by `customerName` ascending.

### List item content

Each card exposes:

| Field / control | Notes |
|-----------------|--------|
| Booking id | Prefixed as `ID: {bookingId}` |
| Status badge | Confirmed (amber) / Mobilised (blue) |
| Asset name + serial number | Primary title lines |
| Quantity | Shown when `quantity > 1` as `Qty: N` |
| Customer name | Person icon row |
| Project location | Location icon row |
| Open in Google Maps | `geo:` intent; prefers Google Maps package, else web maps URL |
| Mark as Mobilised | Visible only when status is `CONFIRMED` |

### Confirmation dialog

Mobilise requires confirmation:

- Title: "Mark as Mobilised?"
- Body explains asset + customer and that the change cannot be undone
- Confirm applies `CONFIRMED` → `MOBILISED`
- Cancel dismisses without change

---

## Status update: mobilise

**Allowed transition:** `CONFIRMED` → `MOBILISED` only.

```gherkin
Feature: Deliveries

  Scenario: Operator mobilises a confirmed delivery
    Given a delivery item with bookingId "DLV-003" and status CONFIRMED
    When the operator confirms mobilisation for that item
    Then the delivery item status becomes MOBILISED
    And the in-memory deliveries list is updated for that bookingId
    And the app attempts PATCH /api/deliveries/{bookingId}/status
    And the request body includes bookingStatus "MOBILISED"

  Scenario: Invalid delivery transitions are ignored
    Given a delivery item with status MOBILISED
    When the operator attempts a status change other than CONFIRMED → MOBILISED
    Then the booking status is unchanged
    And no successful domain transition is applied

  Scenario: API failure still updates locally
    Given a confirmed delivery
    When the operator mobilises it
    And the PATCH call fails
    Then the local status still becomes MOBILISED
    And a network error message is shown to the user
```

Domain details: [domain/booking-status-machine.md](../domain/booking-status-machine.md).  
Offline behaviour: [product/05-offline-fallback.md](05-offline-fallback.md).

---

## Maps

```gherkin
  Scenario: Open project location
    Given a delivery item with a project location string
    When the operator chooses "Open in Google Maps"
    Then the device opens a geo/maps intent for that location query
    And if the Google Maps app is unavailable, a web maps URL is used as fallback
```

---

## Data source (v1)

| Step | Behaviour |
|------|-----------|
| Load list | **`GET /api/deliveries`** → map each `DeliveryItem` DTO to domain `DeliveryItem` |
| Display | Use the API payload as the Delivery List (server/mock already applies “today’s deliveries” membership) |
| UI chips | Client-side only: All / Confirmed / Mobilised on the loaded list |
| Seed / offline | Until the list API succeeds (or if it fails), seed from `MockDataRepository` via `toDeliveryItems()` — see [05-offline-fallback.md](05-offline-fallback.md) |
| Bookings | `GET /api/bookings` may still load for shared booking state; **must not** replace the delivery list by re-filtering bookings with device “today” |
| Update | `PATCH /api/deliveries/{bookingId}/status` with `{ "bookingStatus": "MOBILISED" }` |

```gherkin
  Scenario: Delivery list loads from the deliveries endpoint
    Given the auth session is ready
    When the app loads list data
    Then the client calls GET /api/deliveries
    And the Delivery List shows the returned items
    And the client does not drop rows solely because startDate differs from device LocalDate.now()

  Scenario: Deliveries API failure keeps seed
    Given seed delivery items are shown
    When GET /api/deliveries fails
    Then the Delivery List still shows seed data
    And a network error is surfaced
```

Membership rules the **server/mock** should apply when building the payload: [domain/list-filters.md](../domain/list-filters.md).  
Contract: [api/heavyrental-openapi.yaml](../api/heavyrental-openapi.yaml), examples: [api/examples/deliveries.json](../api/examples/deliveries.json).

---

## Out of scope (v1)

- Rescheduling `startDate`
- Partial quantity delivery
- Photo / signature capture on mobilise
- Assigning a truck or driver

---

## Implementation notes

| Concern | Location |
|---------|----------|
| UI + filters + maps + confirm dialog | `ui/screens/DeliveryListScreen.kt` |
| Load + transition | `viewmodel/AppViewModel.kt` — `loadData` / `updateDeliveryStatus` |
| API | `data/repository/BookingRepository.kt` — `getTodaysDeliveries`, `updateDeliveryStatus` |
| Paths | `network/dto/HeavyRentalApiService.kt` — `GET api/deliveries` |
| DTO map | `network/dto/Mappers.kt` — `DeliveryItemDto.toDeliveryItem()` |
| Seed derive only | `data/models/Bookings.kt` — `List<Booking>.toDeliveryItems()` |
