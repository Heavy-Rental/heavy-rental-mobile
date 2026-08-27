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
| Asset name + serial number | Primary title lines — one per `AssetLine` in `items` (see Known issues K1); blank values show `"Asset not specified"` / `"No serial number"` instead of an empty heading (K3, fixed HR-93) |
| Delivery notes | Shown when `deliveryNotes` is non-blank, as `Note: {text}` |
| Customer name | Person icon row |
| Project location | Location icon row (`projectLocation`; wire field `siteAddress`) |
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
    Given a delivery item with bookingId 3 and status CONFIRMED
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

  Scenario: Unreachable API still updates locally (HR-93)
    Given a confirmed delivery
    When the operator mobilises it
    And the PATCH call fails with IOException (no HTTP response)
    Then the local status still becomes MOBILISED
    And a network error message is shown to the user

  Scenario: HTTP rejection does not update locally (HR-93)
    Given a confirmed delivery
    When the operator mobilises it
    And the PATCH call fails with HttpException (e.g. 400 or 403)
    Then the local status remains CONFIRMED
    And a network error message indicates the update was rejected
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

## Known issues

Recorded here rather than fixed. Each entry states a reproduction, a recommended fix, and its
status, following the same convention as Spring `booking-delivery-return` known-gaps notes.

### K1 — A multi-asset booking shows only one asset *(ticket: HR-113 — resolved)*

**Was:** delivery/return DTOs exposed a single `assetName`/`serialNumber`; Spring
`BookingMapper.primaryAsset()` discarded extra `BookingItem` rows.

**Now:** client `Booking` / `DeliveryItem` / `ReturnItem` carry `items: List<AssetLine>` and cards
render every entry (HR-113). Spring `booking-delivery-return` **FR-BDR-007** requires all
`BookingItem` rows in `items[]`. A single-asset payload is a backend defect against that spec,
not documented mobile behaviour.

**Reproduce (historical):** seed booking `1` has two items (JLG 460SJ Boom Lift, Toyota 8FD25
Forklift). After FR-BDR-007, `GET /api/deliveries` MUST return both.

### K2 — The `Qty: N` badge was removed *(ticket: HR-113, client-side only)*

Until HR-78 the card showed `Qty: N` when `quantity > 1`. `quantity` has no equivalent in the Spring
`BookingResponse`, so the field was dropped from the model and the badge with it. That badge was the
only UI that made **K1** visible to a driver.

**Status:** superseded by the K1 fix rather than literally restored. The old `Qty: N` badge meant
N identical units of the same asset — `items: List<AssetLine>` can't assume that (booking 1's seed
data has a boom lift *and* a forklift, not 2x the same machine), so a numeric badge with that old
meaning would misinform a driver. Instead, each `AssetLine` now renders as its own full name/serial
row unconditionally, so the count is visible directly from the card without a separate badge
element. K1's backend half is resolved (FR-BDR-007).

### K3 — Card rendering for out-of-range values *(ticket: TBD)*

Two cases where a card renders in a way that reads as broken rather than as data:

- **Unknown status → grey "Unknown" badge.** The four display-only statuses
  (`PENDING_DEPOSIT`, `PENDING_CONFIRMED`, `CANCELLED`, and any value added backend-side later)
  have no badge styling of their own beyond the grey fallback already built into the status `when`
  expression, so this already renders as legible data ("Unknown"), not a blank badge. No code
  change was needed for this half of K3.
- **Empty `items` → blank title.** Documented backend behaviour, not a client fault: a booking
  with no `BookingItem` rows maps to `items: []`
  (Spring empty `items` array). The card previously rendered an empty heading.

**Fix (HR-93):** an empty `items` list falls back to `"Asset not specified"` / `"No serial number"`;
a non-empty list falls back per-line via the same `ifBlank {}` pattern on each `AssetLine`'s
`assetName`/`serialNumber` — both rendered in `MutedForeground` so they read as placeholder data
rather than a normal value.

**Status:** fixed for `assetName`/`serialNumber` (HR-93), relocated to the per-`AssetLine` and
empty-`items` cases when `items` replaced the flat fields (K1). The status-badge fallback was
already correct and needed no change. Applies equally to the return list — `ReturnCard` uses the
identical `ifBlank {}` pattern.

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
