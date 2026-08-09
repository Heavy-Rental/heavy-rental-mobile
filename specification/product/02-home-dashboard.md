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
  Scenario: List data loads after the operator authenticates
    Given the user completes login
    When isLoggedIn becomes true
    Then loadData is requested
    And the client calls GET /api/deliveries and GET /api/returns (and GET /api/bookings)
    And if the list APIs succeed, dashboard counts use those payloads
    And if a list API fails, the corresponding counts use mock seed data and an error banner may show
```

`loadData()` runs in `LaunchedEffect(state.isLoggedIn)` inside `HeavyRentalApp`, gated on
`isLoggedIn` — seed lists are available immediately from ViewModel init. It previously ran in
`LaunchedEffect(Unit)` at app launch, which fired **before** login and therefore before any access
token existed; corrected by HR-78 (commit `221e1b2`).

**Consequence:** the load fires exactly once per login transition. Re-verifying a change requires
logging out and back in — see Known issues below.

See [05-offline-fallback.md](05-offline-fallback.md), [03-deliveries.md](03-deliveries.md), [04-returns.md](04-returns.md).

---

## Known issues

### H1 — No loading state; seed data is indistinguishable from live data *(ticket: TBD)*

`loadData()` exposes no in-flight signal. Seed lists are present from ViewModel init, so on a slow
or failed call the dashboard shows **mock counts that look exactly like real ones**. The error
banner appears only after a failure resolves; during the request there is no cue at all.

This matters most during acceptance testing against a real backend: a tester can read seed counts,
believe they came from the API, and pass a check the server never answered.

**Recommended fix:** an `isLoading` flag on `AppState`, and a visual distinction (skeleton, or an
explicit "showing sample data" marker) whenever displayed lists are seed rather than API data.

**Status:** pre-existing, not a regression from HR-78. Affects Home, Deliveries, and Returns alike.

### H2 — No manual refresh *(ticket: TBD)*

`loadData()` fires only on the login transition, so re-checking a change means logging out and back
in. [05-offline-fallback.md](05-offline-fallback.md) lists *"Manual 'retry' control"* under Out of
scope (v1) — a deliberate v1 decision, recorded here because it is a repeated friction point when
running the acceptance checklist in [testing-guide.md](../testing-guide.md), not because it is a bug.

**Status:** documented non-goal. Revisit if testing cost outweighs the scope saving.

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
| State | `viewmodel/AppViewModel.kt` — `deliveries` / `returns` from `GET /api/deliveries` and `GET /api/returns` (seed via `toDeliveryItems()` / `toReturnItems()` until then) |
