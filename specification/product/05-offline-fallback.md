# Feature: Offline / API failure fallback

**Status:** Implemented (v1)  
**Surfaces:** App shell error banner; local booking state  
**Code root:** `com.heavyrental`

---

## Summary

The mobile app must remain **usable for demos and field work** when the backend or mock server is unreachable. Failures are visible, but core status changes still apply **locally**.

---

## Principles

1. **Never blank the main lists** because the network failed.
2. **Always surface** a human-readable error when API sync fails.
3. **Optimistic domain updates** for allowed status transitions: update local state even if PATCH fails.
4. **Mock seed** provides initial data so the UI has content before the first successful API response.

---

## Behaviours

### Initial load

```gherkin
Feature: Offline fallback

  Scenario: Seed data shown before or without API
    Given the app starts
    Then bookings are initialised from MockDataRepository.bookingList
    And delivery and return lists are derived from that data

  Scenario: Successful API load replaces seed
    Given seed or previous data is shown
    When GET /api/bookings succeeds
    Then bookings are replaced with the API payload
    And derived lists refresh
    And the network error banner is cleared

  Scenario: Failed API load keeps seed and shows error
    Given seed data is shown
    When GET /api/bookings fails
    Then bookings remain the seed (or previous) data
    And networkError is set to a message indicating mock/local data is shown
    And the error banner is visible in the app shell
```

**Error copy (load)** — set in `AppViewModel.loadBookings`:

```text
Could not reach API — showing mock data. ({exception message})
```

### Status updates

```gherkin
  Scenario: Status PATCH fails but local transition applies
    Given an allowed status transition (e.g. CONFIRMED → MOBILISED)
    When the operator confirms the action
    And the corresponding PATCH fails
    Then the local booking status still updates
    And derived lists refresh
    And networkError explains that the update was local only

  Scenario: Status PATCH succeeds
    Given an allowed status transition
    When the PATCH succeeds
    Then local status updates
    And networkError is cleared
```

**Error copy (status)** — set in `AppViewModel.updateBookingStatus`:

```text
Could not sync status update to API — updated locally only. ({exception message})
```

**Ordering note (v1):** local state is updated **after** the API attempt completes (success or failure). The UI still ends in the new status either way; there is no rollback on failure.

### Disallowed transitions

Invalid transitions are **silently ignored** (no local change, no required error). See [domain/booking-status-machine.md](../domain/booking-status-machine.md).

### Error banner UI

When `networkError != null`, `HeavyRentalApp` shows a top banner using the Material3 error container colours, with the error string as body text. Banner is only shown for the authenticated shell (not on the login screen).

---

## Network configuration (dev)

| Context | Base URL |
|---------|----------|
| Android emulator → host | `http://10.0.2.2:8081/` |
| Host machine / curl | `http://localhost:8081/` or `http://127.0.0.1:8081/` |
| Physical device | Host LAN IP, e.g. `http://192.168.x.x:8081/` (must match `RetrofitInstance`) |

Configured in: `network/RetrofitInstance.kt` (`BASE_URL`).  
Cleartext HTTP is used in dev; see `res/xml/network_security_config.xml`.

Mock servers: [`mocks/README.md`](../../mocks/README.md) and [api/README.md](../api/README.md).

---

## Out of scope (v1)

- Persistent offline queue / retry when back online  
- Conflict resolution if server status diverges  
- Full offline database (Room)  
- Airplane-mode specific UX beyond the banner  
- Manual "retry" control (re-entry / relaunch triggers load again)

---

## Implementation notes

| Concern | Location |
|---------|----------|
| Seed | `data/repository/MockDataRepository.kt` — `bookingList` |
| Load fallback | `viewmodel/AppViewModel.kt` — `loadBookings` |
| Status fallback | `viewmodel/AppViewModel.kt` — `updateBookingStatus` |
| Banner | `MainActivity.kt` — `HeavyRentalApp` when `networkError != null` |
