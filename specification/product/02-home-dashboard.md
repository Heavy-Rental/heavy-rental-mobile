# Feature: Home dashboard

**Status:** Implemented (v1)  
**Screens:** `HomeScreen`  
**Navigation:** `AppScreen.HOME` (default after login)  
**Code root:** `com.heavyrental`

---

## Summary

After login, the operator sees a dashboard for **today’s** work: how many deliveries and returns are due, broken down by booking status. Navigation to Deliveries and Returns is available via the bottom bar (app shell).

---

## Display content

| Element | Description |
|---------|-------------|
| Brand | "Heavy Rental" |
| Welcome | "Welcome, {adminName}" |
| Date | Today, formatted as `EEEE, d MMMM yyyy` (device local, e.g. `Monday, 3 August 2026`) |
| Section | "Today's Overview" |
| Delivery summary | Total + Confirmed + Mobilised counts |
| Return summary | Total + Mobilised + Completed counts |
| Logout | Exit icon in header; returns to login (see [01-login.md](01-login.md)) |

---

## Counts (acceptance)

Counts are computed from the same derived lists as the Deliveries and Returns screens (see [domain/list-filters.md](../domain/list-filters.md)).

Passed into `HomeScreen` from `HeavyRentalApp` using `vm.deliveries` / `vm.returns`.

### Deliveries (bookings with `startDate == today`)

| Metric | Definition |
|--------|------------|
| `deliveryCount` | `deliveries.size` (CONFIRMED + MOBILISED for today) |
| `confirmedCount` | Delivery items with status `CONFIRMED` |
| `mobilisedDeliveryCount` | Delivery items with status `MOBILISED` |

### Returns (bookings with `endDate == today`)

| Metric | Definition |
|--------|------------|
| `returnCount` | `returns.size` (MOBILISED + COMPLETED for today) |
| `mobilisedReturnCount` | Return items with status `MOBILISED` |
| `completedCount` | Return items with status `COMPLETED` |

```gherkin
Feature: Home dashboard

  Scenario: Dashboard reflects today's derived lists
    Given the user is logged in
    And bookings have been loaded (or mock seed is shown)
    When the user views the home screen
    Then delivery and return counts match the domain list filters for today
    And the welcome line shows the logged-in admin name
    And the date line shows today in "EEEE, d MMMM yyyy" form
```

---

## Navigation

| From Home | To | Mechanism |
|-----------|-----|-----------|
| App shell bottom nav | Home | `AppScreen.HOME` |
| App shell bottom nav | Deliveries | `AppScreen.DELIVERIES` |
| App shell bottom nav | Returns | `AppScreen.RETURNS` |
| Logout control | Login | `logout()` |

Shell navigation is required. Home does not own its own tab routing beyond logout.

---

## Data loading

```gherkin
  Scenario: Bookings load when the app shell starts
    Given the user opens the app
    When the main shell is composed
    Then loadBookings is requested
    And if the API succeeds, dashboard counts use API data
    And if the API fails, dashboard counts use mock seed data and an error banner may show
```

`loadBookings()` runs in `LaunchedEffect(Unit)` inside `HeavyRentalApp` (after login gate still seeds/loads when shell composes; seed is available immediately in ViewModel init).

See [05-offline-fallback.md](05-offline-fallback.md).

---

## Out of scope (v1)

- Historical (non-today) dashboards
- Charts / analytics
- Push notifications for overdue items
- Multi-yard or multi-company switching
- Deep links from overview tiles into filtered lists

---

## Implementation notes

| Concern | Location |
|---------|----------|
| UI | `ui/screens/HomeScreen.kt` |
| Counts passed from shell | `MainActivity.kt` — `HeavyRentalApp` |
| State | `viewmodel/AppViewModel.kt` — bookings → `toDeliveryItems()` / `toReturnItems()` |
