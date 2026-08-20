# Feature: Customer bookings (read-only)

**Status:** Implemented (v1)
**Screens:** `CustomerBookingsScreen`
**Navigation:** `AppScreen.LOGIN` → success (customer session) navigates to `AppScreen.CUSTOMER_BOOKINGS`
**Code root:** `com.heavyrental`
**API contract:** [`api/heavyrental-openapi.yaml`](../api/heavyrental-openapi.yaml) (Bookings tag — `GET /api/bookings`)

---

## Summary

A customer (`ROLE_USER`, the same role the login handshake calls `ROLE_USER` — see
[01-login.md](01-login.md) §L1) who signs in with email/password lands on a single,
**read-only** screen listing their own bookings and each booking's `bookingStatus`. There is
no way to edit a booking, change its status, or trigger a delivery/return action from this
screen — those affordances exist only on the staff Deliveries/Returns screens and are not
rendered here at all, regardless of role.

This is the resolution of what [01-login.md](01-login.md) previously documented as customer
sign-in being blocked client-side: `AppViewModel.onLoginSuccess` revoked the token and refused
entry to any session that wasn't `ROLE_ADMIN`/`ROLE_DRIVER`. Customers now get a session and a
screen of their own instead of a lockout — see **Role routing** below.

The backend required **no changes** for this feature: `POST /api/auth/login` already issues a
working access token for `ROLE_USER` accounts, and `GET /api/bookings` was already scoped
server-side to the caller's own bookings (`BookingService.getBookings`) before this screen
existed. Everything here is client-side routing and presentation.

---

## Role routing

`AppViewModel.onLoginSuccess` branches on the access token's `roles` claim
(`JwtClaims.isStaff` / `JwtClaims.isCustomer`) after a successful `POST /api/auth/login`:

| Token role | Destination | Notes |
|------------|-------------|-------|
| `ROLE_ADMIN` or `ROLE_DRIVER` | `AppScreen.HOME` | Unchanged staff flow — see [02-home-dashboard.md](02-home-dashboard.md) |
| `ROLE_USER` | `AppScreen.CUSTOMER_BOOKINGS` | This feature |
| Anything else / unrecognised | Login refused, token revoked | Unchanged from prior behaviour |

**Google Sign-In stays staff-only.** `loginWithGoogle` calls `onLoginSuccess` with
`allowCustomer = false`, so a `ROLE_USER` token obtained via `POST /api/auth/google` is
*still* treated as a lockout, not routed to `CUSTOMER_BOOKINGS`. This isn't a gap so much as
matching the backend's own provisioning rule — `AuthService.provisionGoogleUser` always
assigns a first-time Google sign-in `ROLE_DRIVER` (the mobile app is staff-only from the
backend's point of view), so a `ROLE_USER` token can only reach the Google path today if an
*existing* customer account happens to share an email with a Google account. Password login is
the only supported customer entry point; see [01-login.md](01-login.md) for the full
email/password vs. Google comparison.

```gherkin
Feature: Post-login role routing

  Scenario: Customer credentials route to the read-only bookings screen
    Given the user is on the login screen
    And the user enters the credentials of a ROLE_USER account
    When the user submits login
    Then POST /api/auth/login succeeds and returns an access token
    And the current screen is CUSTOMER_BOOKINGS
    And no login error is shown

  Scenario: Staff credentials route to the dashboard, unchanged
    Given the user is on the login screen
    And the user enters the credentials of a ROLE_ADMIN or ROLE_DRIVER account
    When the user submits login
    Then the current screen is HOME

  Scenario: A customer token obtained via Google sign-in is still refused
    Given the user is on the login screen
    And the user taps "Continue with Google"
    And the resulting access token carries only ROLE_USER
    Then the client revokes the token
    And the login error "This app is for staff use only — contact your admin if you believe this is an error." is shown
    And the current screen remains LOGIN
```

---

## Screen contents

Sourced entirely from **`GET /api/bookings`**, loaded once on entering `CUSTOMER_BOOKINGS`
(`AppViewModel.loadCustomerBookings`, invoked from the same `loadData()` staff and customer
sessions share — see [domain/list-filters.md](../domain/list-filters.md) "Customer booking
list filter"). Unlike the staff Deliveries/Returns lists, there is **no date-based "today"
membership filter** — every booking the server returns for this caller is shown, subject only
to the status filter chips below.

| Element | Behaviour |
|---------|-----------|
| Title bar | "My Bookings", "Welcome, {customerName}", logout icon button |
| Filter chips | All / Pending / Confirmed / Mobilised / Completed / Cancelled — see domain doc for the status grouping and count badges |
| Booking card | Booking #id, status badge, asset name + serial (one line per `AssetLine` in `items`), delivery note (if any), date range, site address |
| Empty state (no bookings at all) | "You don't have any bookings yet" |
| Empty state (filter matches nothing) | "No bookings match this filter" |
| Sort | `bookingId` descending (newest booking first) — see domain doc |

Every field displayed is read from the `Booking` the server returned; nothing on this screen
is computed from a transition rule, because there are no transitions to compute — see
**Out of scope** below.

---

## Acceptance criteria

```gherkin
Feature: Customer bookings screen

  Scenario: Customer sees their own bookings after login
    Given a logged-in customer session
    When CUSTOMER_BOOKINGS loads
    Then the client calls GET /api/bookings
    And every returned booking is rendered as a card
    And each card shows its bookingStatus as a coloured badge

  Scenario: No bookings for this customer
    Given a logged-in customer session
    And GET /api/bookings returns an empty list
    Then the screen shows "You don't have any bookings yet"

  Scenario: A filter chip narrows the visible list without a network call
    Given a logged-in customer session with bookings in multiple statuses
    When the customer selects a filter chip
    Then only bookings matching that chip's status predicate are shown
    And no additional API call is made

  Scenario: List load fails
    Given a logged-in customer session
    When GET /api/bookings fails
    Then the previously-held booking list is shown unchanged (seed data on first load)
    And the error "Could not reach API — showing mock data. (bookings: {message})" is shown

  Scenario: The screen never offers a mutating control
    Given a logged-in customer session with bookings in every status
    Then no button, menu, or gesture on this screen changes a booking's status
    And no button, menu, or gesture on this screen edits any booking field
```

### Known gap — seed fallback isn't scoped to the customer

The failure-path fallback in the last scenario above reuses `MockDataRepository.bookingList`
(the same generic seed staff screens fall back to), which is **not** filtered to any one
customer. A customer who loses connectivity right after login could transiently see seed rows
that aren't theirs. This mirrors the same seed-is-not-membership-aware caveat already recorded
for staff lists in [05-offline-fallback.md](05-offline-fallback.md), just newly relevant here
because this is the first customer-facing (as opposed to staff-facing) screen with the same
fallback wired up. Not fixed in v1 — flagging it here so it isn't mistaken for scoped data.

---

## Out of scope (v1)

- Editing any booking field
- Changing `bookingStatus` from this screen (mobilise/complete remain staff-only actions on
  Deliveries/Returns — see [booking-status-machine.md](../domain/booking-status-machine.md))
- Creating a new booking / requesting a quote
- Cancelling a booking
- Contacting support / messaging staff from within the app
- Push notifications on status change
- Google Sign-In as a customer entry point (see **Role routing** above)
- Payment / invoice / deposit status beyond what `bookingStatus` itself conveys
- Pagination — `GET /api/bookings` is loaded and rendered in full

---

## Implementation notes

| Concern | Location |
|---------|----------|
| Role branch after login | `viewmodel/AppViewModel.kt` — `onLoginSuccess(response, allowCustomer)` |
| Role detection from JWT | `network/JwtClaims.kt` — `isStaff`, `isCustomer` |
| Session role flag | `viewmodel/AppViewModel.kt` — `AppState.isCustomer` |
| Data load | `viewmodel/AppViewModel.kt` — `loadData()` → `loadCustomerBookings()` |
| Screen | `ui/screens/CustomerBookingsScreen.kt` |
| Navigation destination | `navigation/AppScreen.kt` — `CUSTOMER_BOOKINGS` |
| Shell wiring (skips staff bottom nav) | `MainActivity.kt` — `HeavyRentalApp` |
| Filter/sort domain rules | [domain/list-filters.md](../domain/list-filters.md) "Customer booking list filter" |
| Backend endpoint (unchanged) | `GET /api/bookings` — `BookingController` / `BookingService.getBookings` (Spring repo) |

---

## Related specs

- [01-login.md](01-login.md) — role routing origin, seeded accounts, Google Sign-In staff-only rule
- [domain/list-filters.md](../domain/list-filters.md) — filter chip grouping and sort order
- [domain/booking-status-machine.md](../domain/booking-status-machine.md) — what each status means; this screen drives none of the transitions it describes
- [05-offline-fallback.md](05-offline-fallback.md) — the seed-fallback pattern this screen reuses
