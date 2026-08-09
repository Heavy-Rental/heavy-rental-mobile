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
| Asset name + serial number | Primary title lines — **one asset only**, see [03-deliveries.md](03-deliveries.md) K1 |
| Delivery notes | Shown when `deliveryNotes` is non-blank, as `Note: {text}` |
| Customer name | Person icon row |
| Site address | Location icon row (`siteAddress`) |
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
    Given a return item with bookingId 8 and status MOBILISED
    When the operator marks the return as completed
    Then the return item status becomes COMPLETED
    And the in-memory returns list is updated for that bookingId
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
| Load list | **`GET /api/returns`** → map each `ReturnItem` DTO to domain `ReturnItem` |
| Display | Use the API payload as the Return List (server/mock already applies “today’s returns” membership) |
| UI chips | Client-side only: All / Mobilised / Completed on the loaded list |
| Seed / offline | Until the list API succeeds (or if it fails), seed from `MockDataRepository` via `toReturnItems()` — see [05-offline-fallback.md](05-offline-fallback.md) |
| Bookings | `GET /api/bookings` may still load for shared booking state; **must not** replace the return list by re-filtering bookings with device “today” |
| Update | `PATCH /api/returns/{bookingId}/status` with `{ "bookingStatus": "COMPLETED" }` |

```gherkin
  Scenario: Return list loads from the returns endpoint
    Given the auth session is ready
    When the app loads list data
    Then the client calls GET /api/returns
    And the Return List shows the returned items
    And the client does not drop rows solely because endDate differs from device LocalDate.now()

  Scenario: Returns API failure keeps seed
    Given seed return items are shown
    When GET /api/returns fails
    Then the Return List still shows seed data
    And a network error is surfaced
```

Membership rules: [domain/list-filters.md](../domain/list-filters.md).  
Contract examples: [api/examples/returns.json](../api/examples/returns.json).

---

## Known issues

Both apply identically to this screen; the primary write-up lives with the delivery list so the two
don't drift apart. See [03-deliveries.md](03-deliveries.md) — Known issues.

| Ref | Summary | Status |
|-----|---------|--------|
| K1 | A multi-asset booking shows only one asset — `ReturnItemResponse` carries a single `assetName`/`serialNumber` and the server discards the rest | Blocked on a backend contract decision |
| K2 | The `Qty: N` badge was removed by HR-78 (`quantity` has no backend equivalent) | Deliberate; restore with K1 |
| K3 | Unknown status renders a grey "Unknown" badge; empty `assetName` renders a blank card title | Pre-existing |

For returns specifically, K1 means a driver can mark a booking `COMPLETED` while a second asset is
still on site, with nothing on the card indicating it exists.

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
| Load + transition | `viewmodel/AppViewModel.kt` — `loadData` / `updateReturnStatus` |
| API | `data/repository/BookingRepository.kt` — `getTodaysReturns`, `updateReturnStatus` |
| Paths | `network/dto/HeavyRentalApiService.kt` — `GET api/returns` |
| DTO map | `network/dto/Mappers.kt` — `ReturnItemDto.toReturnItem()` |
| Seed derive only | `data/models/Bookings.kt` — `List<Booking>.toReturnItems()` |
