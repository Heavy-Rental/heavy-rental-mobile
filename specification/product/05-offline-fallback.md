# Feature: Offline / API failure fallback

**Status:** Implemented (v1)  
**Surfaces:** App shell error banner; local booking state  
**Code root:** `com.heavyrental`

---

## Summary

The mobile app must remain **usable for demos and field work** when the backend or mock server is unreachable **after** the operator has authenticated. Failures are visible, but core status changes still apply **locally** — except when the server has explicitly rejected the request (see Principle 3, updated HR-93).

**Boundary:** this feature covers **bookings load** and **status PATCH** fallback. **Login** requires a reachable auth API and surfaces errors on the login screen (`loginError`), not the shell banner. See [01-login.md](01-login.md).

---

## Principles

1. **Never blank the main lists** because the network failed.
2. **Always surface** a human-readable error when list/status API sync fails.
3. **Status update local state depends on failure type (HR-93):** applied optimistically only when
   the API is genuinely unreachable (`IOException`); withheld when the server explicitly rejects the
   request (`HttpException`, e.g. `400`/`403`) or on any unrecognised failure, both treated
   conservatively as "no change." See **O1** below — resolved.
4. **Mock seed** provides initial booking data so the UI has content before the first successful API response (and when load fails).
5. **Login is not offline** — entry requires a successful interim → access handshake against Mockoon/Prism/Spring.

---

## Behaviours

### Initial load

```gherkin
Feature: Offline fallback

  Scenario: Seed data shown before or without API
    Given the app starts
    Then bookings are initialised from MockDataRepository.bookingList
    And delivery and return lists are derived from that seed via toDeliveryItems / toReturnItems

  Scenario: Successful list API load replaces seed lists
    Given seed or previous data is shown
    When GET /api/deliveries succeeds
    Then the deliveries list is replaced with the API payload
    When GET /api/returns succeeds
    Then the returns list is replaced with the API payload
    And the network error banner is cleared for successful loads

  Scenario: Successful bookings load replaces booking seed only
    Given seed bookings are shown
    When GET /api/bookings succeeds
    Then bookings are replaced with the API payload
    And the deliveries and returns lists are not rebuilt solely by re-filtering bookings with device today

  Scenario: Failed list API load keeps seed and shows error
    Given seed delivery and return lists are shown
    When GET /api/deliveries or GET /api/returns fails
    Then the corresponding list remains the seed (or previous) data
    And networkError is set to a message indicating mock/local data is shown
    And the error banner is visible in the app shell
```

**Error copy (load)** — set in `AppViewModel.loadData` (or equivalent):

```text
Could not reach API — showing mock data. ({exception message})
```

### Status updates

```gherkin
  Scenario: Status PATCH succeeds
    Given an allowed status transition
    When the PATCH succeeds
    Then local status updates
    And networkError is cleared

  Scenario: Status PATCH fails with no response reached — local transition still applies (HR-93)
    Given an allowed status transition (e.g. CONFIRMED → MOBILISED)
    When the operator confirms the action
    And the PATCH call throws IOException (host unreachable, no response received at all)
    Then the local booking status still updates
    And derived lists refresh
    And networkError reads "Could not reach API — updated locally only. ({message})"

  Scenario: Status PATCH is explicitly rejected by the server — no local change (HR-93)
    Given an allowed status transition per the client's own guards
    When the operator confirms the action
    And the server responds with a non-2xx status (HttpException — e.g. 400 invalid transition, 403 unauthorised)
    Then the local booking status does NOT change
    And networkError reads "Update rejected by server ({code}) — status unchanged."

  Scenario: Status PATCH fails with an unrecognised error — no local change (HR-93)
    Given an allowed status transition
    When the operator confirms the action
    And the PATCH call throws an exception that is neither HttpException nor IOException
    Then the local booking status does NOT change, treated conservatively as a rejection
    And networkError reads "Could not sync status update — status unchanged. ({message})"
```

**Error copy (status)** — set in `AppViewModel.updateBookingStatus`, one of three depending on
failure type (HR-93):

```text
Update rejected by server ({code}) — status unchanged.                    // HttpException
Could not reach API — updated locally only. ({exception message})         // IOException
Could not sync status update — status unchanged. ({exception message})    // any other Exception
```

**Ordering note (v1, updated HR-93):** local state is updated **after** the API attempt completes,
and only on success or `IOException` — never after an `HttpException` or an unrecognised exception,
both of which leave state unchanged. Still no rollback: once local state is applied there is no
reconciliation against a later authoritative response, other than the next successful `loadData()`.

### Disallowed transitions

Invalid transitions are **silently ignored** (no local change, no required error). See [domain/booking-status-machine.md](../domain/booking-status-machine.md).

### Error banner UI

When `networkError != null`, `HeavyRentalApp` shows a top banner using the Material3 error container colours, with the error string as body text. Banner is only shown for the authenticated shell (not on the login screen).

### Login vs offline (explicit)

```gherkin
  Scenario: Unreachable mock during login does not use seed-as-auth
    Given the user is on the login screen
    When the user submits login
    And the auth API is unreachable
    Then the user remains logged out
    And loginError is shown on the login screen
    And the shell networkError banner is not shown
```

---

## Network configuration (dev)

Default since HR-78 is the **real Spring Boot backend on port 8080**. Mockoon/Prism on 8081 remains
available behind a compile-time flag.

| Target | Emulator → host | Host machine / curl |
|--------|-----------------|---------------------|
| Spring Boot (**default**) | `http://10.0.2.2:8080/` | `http://localhost:8080/` |
| Mockoon / Prism | `http://10.0.2.2:8081/` | `http://localhost:8081/` |

Physical device: substitute the host LAN IP (e.g. `http://192.168.x.x:8080/`) and keep it in sync
with `RetrofitInstance`.

Configured in `network/dto/RetrofitInstance.kt`:

```kotlin
private const val USE_MOCK_SERVER = false   // true = Mockoon 8081, false = Spring Boot 8080
```

Cleartext HTTP is used in dev; see `res/xml/network_security_config.xml`.

> **Backend availability.** The seven booking/delivery/return routes exist only on the backend branch
> `HR-80`, not on its `develop` (`SPEC-api-index.md` §2.2). Against a `develop` backend they return
> `404`, which this app currently reports as a connectivity failure — see **O2** below.

> **Environment selection is a tracked source edit** *(ticket: TBD)*. `USE_MOCK_SERVER` is a
> committed constant, so the wrong value will eventually be pushed. A properties-based approach
> (`app/api.properties` + a gitignored `local.properties` override → `BuildConfig`) was built and
> merged as PR #7 (commit `8bafd09`), then reverted whole by PR #9 — recover it from git rather
> than rebuilding. Note `8bafd09` and HR-78 rewrite this same file incompatibly, so it must be
> rebased onto HR-78.

Mock servers: [`mocks/README.md`](../../mocks/README.md) and [api/README.md](../api/README.md).

---

## Open questions raised by HR-78

HR-78 changed the default backend from Mockoon to Spring Boot. Two v1 principles in this document
were written against a mock that cannot fail in these ways, and their behaviour had inverted once a
real backend was on the other end. **O1 is now resolved (HR-93)**; O2 and O3 remain open and are
recorded here so their decisions are made deliberately rather than by default.

### O1 — Optimistic status updates vs. server-enforced transitions — RESOLVED (HR-93)

**Decision:** split by failure type, implemented in `AppViewModel.updateBookingStatus`.

- `HttpException` (server explicitly responded — e.g. `400` invalid transition, `403`
  unauthorised): local state is **not** changed. `networkError` reads
  `"Update rejected by server ({code}) — status unchanged."`
- `IOException` (no response reached the client — genuinely unreachable): local state **is**
  applied optimistically, preserving offline field use. `networkError` reads
  `"Could not reach API — updated locally only. ({message})"`
- Any other `Exception` (unrecognised failure shape): treated conservatively like a rejection —
  local state is **not** changed. `networkError` reads
  `"Could not sync status update — status unchanged. ({message})"`

Mirrored in [domain/booking-status-machine.md](../domain/booking-status-machine.md) transition
guards. Principle 3 and the "Ordering note" above reflect this decision.

**Why it was written the old way originally:** Mockoon returns `200` to any request and cannot
reject a transition, so the client's own preconditions were the only guard in existence. Applying
locally after a failed PATCH could only ever mean "the network was down", never "the server said
no" — and keeping the app usable in the field was the point of this feature.

**What changed:** the Spring backend enforces the same two transitions server-side and returns
`400` on anything else (`SPEC-booking-delivery-return-api.md` §4, Requirements 4.2 and 6), and
`ROLE_DRIVER` is excluded from every protected route today (`SPEC-api-index.md` §4), so a driver's
status update returns `403`. Both are now `HttpException`s and are correctly rejected rather than
silently applied as they were pre-HR-93.

**Residual gap — depends on O2:** the split is by exception type, not by HTTP status code, so a
driver's `403` and a genuine `400` invalid-transition both read as "rejected by server" with only
the `{code}` differing in the message. Tailored copy per status code is O2's remaining scope, not
re-opened here.

**Options that were considered (kept for record):**

| Option | Effect |
|--------|--------|
| Keep as specified (pre-HR-93) | Offline field use preserved; invalid transitions and `403`s remained invisible |
| Server-authoritative | Local state changes only on a successful PATCH; load fallback (seed data) unaffected |
| **Split by failure type — chosen (HR-93)** | Apply locally on `IOException` (genuinely offline), reject on `HttpException` (server said no) |

### O2 — All failures report as connectivity failures *(ticket: TBD)*

`loadData()` and the status-update path catch bare `Exception`, so an `HttpException` (`400`,
`403`, `404`) is indistinguishable from an `IOException` (host unreachable). Both render the
"Could not reach API — showing mock data" copy specified above. A `404` from a backend running
`develop` instead of `HR-80` has already been misdiagnosed as a Docker networking problem.

`AppViewModel.login()` already distinguishes the two correctly and maps status codes to specific
copy — the same pattern applied to `loadData()` would resolve this.

**Note:** the error copy specified in this document is only correct for the `IOException` case.
Whatever wording replaces it for HTTP failures should be added here in the same change.

**Note (HR-93):** `updateBookingStatus` now distinguishes `HttpException` from `IOException`
(see O1, resolved) but still does not distinguish HTTP status codes from one another within the
`HttpException` branch — a `403` and a `400` both read as "Update rejected by server ({code})."
`loadData()` is unaffected by HR-93 and still catches bare `Exception` as described above.

**Status:** pre-existing (present on `develop` before HR-78); surfaced by the switch to a real
backend. Was the prerequisite for O1's "split by failure type" option, which is now implemented;
remaining scope is per-status-code copy, in both `loadData()` and the `HttpException` branch of
`updateBookingStatus`.

### O3 — `GET /api/bookings` has no observable effect *(ticket: TBD)*

`loadData()` calls `GET /api/bookings` and assigns the result to `_bookings`, per the "Successful
bookings load replaces booking seed only" scenario above. But **no screen collects that state** —
`HeavyRentalApp` collects `deliveries`, `returns`, `state`, and `networkError` only. Its sole
remaining effect is a third-choice lookup in `updateBookingStatus`, reached only when a booking is
absent from both list states, which cannot happen for any row the operator can actually tap.

So the call's success or failure is visible **only in the OkHttp log**, and its payload never
reaches the UI.

**Consequence for testing:** the acceptance step covering `GET /api/bookings` cannot be verified
from the app. Either accept logcat as the evidence and say so explicitly in the review, or record
that step as not mobile-testable — but decide, rather than leaving it ambiguous at sign-off.

**Recommended fix:** either drop the call from `loadData()` until a screen needs it, or give it a
consumer (a booking detail screen is already unbuilt — `PUT`/`GET`-by-id exist in
`HeavyRentalApiService` with no repository method or UI behind them).

**Status:** pre-existing on `develop`, not a regression from HR-78. Untouched by HR-93.

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
| Load fallback | `viewmodel/AppViewModel.kt` — `loadData` |
| Status fallback (split by failure type, HR-93) | `viewmodel/AppViewModel.kt` — `updateBookingStatus` |
| Banner | `MainActivity.kt` — `HeavyRentalApp` when `networkError != null` |
