# Feature: Today's returns

**Status:** Implemented (v1)  
**Screens:** `ReturnListScreen`  
**Navigation:** `AppScreen.RETURNS`  
**Code root:** `com.heavyrental`

---

## Summary

Operators manage **equipment returns due today**. They can filter the list, open the project location on a map, and mark a **mobilised** booking as **completed** when the asset has been returned.

---

## List membership

A booking appears on the return list when **all** of the following hold:

1. `endDate` is **today** (device local date)
2. `bookingStatus` is `MOBILISED` **or** `COMPLETED`

Full rules: [domain/list-filters.md](../domain/list-filters.md).

---

## UI behaviour

### Header

- Title: **Return List**
- Subtitle: `{visible} of {total} returns` for the active filter

### Filters (client-side)

| Filter chip | Shows |
|-------------|--------|
| Show All | All return items |
| Mobilised | `bookingStatus == MOBILISED` (awaiting return) |
| Completed | `bookingStatus == COMPLETED` |

Default filter: **Show All**.  
Visible rows are sorted by `customerName` ascending.

### List item content

Each card exposes (aligned with delivery list pattern):

| Field / control | Notes |
|-----------------|--------|
| Booking id | Prefixed as `ID: {bookingId}` |
| Status badge | Mobilised / Completed |
| Asset name + serial number | Primary title lines |
| Quantity | Shown when `quantity > 1` as `Qty: N` |
| Customer name | Person icon row |
| Project location | Location icon row |
| Open in Google Maps | Same geo / web fallback as deliveries |
| Complete action | Visible only when status is `MOBILISED` |

### Confirmation dialog

Completing a return requires confirmation (same pattern as mobilise):

- Confirm applies `MOBILISED` → `COMPLETED`
- Cancel dismisses without change

---

## Status update: complete return

**Allowed transition:** `MOBILISED` → `COMPLETED` only.

```gherkin
Feature: Returns

  Scenario: Operator completes a mobilised return
    Given a return item with bookingId "RET-002" and status MOBILISED
    And endDate is today
    When the operator marks the return as completed
    Then the booking status becomes COMPLETED
    And the deliveries and returns derived lists are refreshed
    And the app attempts PATCH /api/returns/{bookingId}/status
    And the request body includes bookingStatus "COMPLETED"

  Scenario: Invalid return transitions are ignored
    Given a return item with status COMPLETED
    When the operator attempts a status change other than MOBILISED → COMPLETED
    Then the booking status is unchanged

  Scenario: API failure still updates locally
    Given a mobilised return due today
    When the operator completes it
    And the PATCH call fails
    Then the local status still becomes COMPLETED
    And a network error message is shown to the user
```

Domain details: [domain/booking-status-machine.md](../domain/booking-status-machine.md).  
Offline behaviour: [product/05-offline-fallback.md](05-offline-fallback.md).

---

## Maps

```gherkin
  Scenario: Open project location
    Given a return item with a project location string
    When the operator chooses "Open in Google Maps"
    Then the device opens a geo/maps intent for that location query
    And if the Google Maps app is unavailable, a web maps URL is used as fallback
```

---

## Data source (v1)

| Step | Behaviour |
|------|-----------|
| Load | Prefer `GET /api/bookings`; map to domain `Booking` |
| Derive list | Client applies return filter (`endDate` + status) via `toReturnItems()` |
| Update | `PATCH /api/returns/{bookingId}/status` with `{ "bookingStatus": "COMPLETED" }` |

Note: `GET /api/returns` exists in the API contract for backend/mocks but the **v1 client derives the list from bookings**.

---

## Out of scope (v1)

- Damage / inspection checklist
- Late fees or overhire calculation
- Partial returns
- Changing `endDate`

---

## Implementation notes

| Concern | Location |
|---------|----------|
| UI + filters + maps | `ui/screens/ReturnListScreen.kt` |
| Transition | `viewmodel/AppViewModel.kt` — `updateReturnStatus` |
| API | `data/repository/BookingRepository.kt` → `network/HeavyRentalApiService` |
| Derive | `data/models/Bookings.kt` — `List<Booking>.toReturnItems()` |
