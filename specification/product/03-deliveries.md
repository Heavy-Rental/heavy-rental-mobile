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
    And startDate is today
    When the operator confirms mobilisation for that item
    Then the booking status becomes MOBILISED
    And the deliveries and returns derived lists are refreshed
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
| Load | Prefer `GET /api/bookings`; map to domain `Booking` |
| Derive list | Client applies delivery filter (`startDate` + status) via `toDeliveryItems()` |
| Update | `PATCH /api/deliveries/{bookingId}/status` with `{ "bookingStatus": "MOBILISED" }` |

Note: `GET /api/deliveries` exists in the API contract for backend/mocks but the **v1 client derives the list from bookings**. See [decisions/001-openapi-as-api-source.md](../decisions/001-openapi-as-api-source.md).

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
| Transition | `viewmodel/AppViewModel.kt` — `updateDeliveryStatus` |
| API | `data/repository/BookingRepository.kt` → `network/HeavyRentalApiService` |
| Derive | `data/models/Bookings.kt` — `List<Booking>.toDeliveryItems()` |
