# Feature: Admin login

**Status:** Implemented (v1)  
**Screens:** `LoginScreen`  
**Navigation:** `AppScreen.LOGIN` → success navigates to `AppScreen.HOME` (staff) or `AppScreen.CUSTOMER_BOOKINGS` (customer) — see **Role routing** below  
**Code root:** `com.heavyrental`  
**API contract:** [`api/heavyrental-openapi.yaml`](../api/heavyrental-openapi.yaml) (Auth tag)

---

## Summary

Every user — staff or customer — authenticates through this same screen. Two paths are
supported:

**A. Email/password**, an HTTP interim → access JWT handshake defined in OpenAPI:

1. `GET /api/auth/getBearerToken` — mint a short-lived interim JWT (`text/plain`)
2. `POST /api/auth/login` — upgrade interim JWT + email/password to an access (session) JWT
3. Business calls use the access token as `Authorization: Bearer …` (via `AuthInterceptor`)
4. `POST /api/auth/logout` — revoke the access token (best-effort from the client)

**B. Google Sign-In** (HR-152), an alternative to step 2 above using a Google-issued ID token instead of a password:

1. `GET /api/auth/getBearerToken` — same interim JWT as path A
2. Android Credential Manager (`GetGoogleIdOption`) returns a Google ID token from the on-device account picker
3. `POST /api/auth/google` — upgrade the interim JWT + Google ID token to the same shape of access (session) JWT as `POST /api/auth/login`
4. Steps 3–4 of path A are unchanged (business calls, logout)

Both paths converge on the same `LoginResponse` shape and the same in-memory session (`TokenSession`) — the rest of the app does not distinguish how a session was established.

Tokens are held **in memory only** (`TokenSession`). There is no secure storage or automatic refresh in v1.

## Role routing

**After either path succeeds, the client routes by role** (`AppViewModel.onLoginSuccess`,
decoding the access token's `roles` claim via `JwtClaims`):

| Role | Destination |
|------|-------------|
| `ROLE_ADMIN` / `ROLE_DRIVER` (staff) | `AppScreen.HOME` |
| `ROLE_USER` (customer) — **password path only** | `AppScreen.CUSTOMER_BOOKINGS`, a read-only view of the caller's own bookings — see [06-customer-bookings.md](06-customer-bookings.md) |
| `ROLE_USER` via Google, or anything unrecognised | Login refused, access token revoked immediately |

Customer routing applies **only** to path A (email/password). Path B (Google Sign-In) still
treats a `ROLE_USER` token as a lockout — see **L2** and
[06-customer-bookings.md](06-customer-bookings.md) "Role routing" for why.

**Default dev server (since HR-78):** the real Spring Boot backend on port **8080** — emulator base URL `http://10.0.2.2:8080/`. Mockoon/Prism on **8081** remains available via `USE_MOCK_SERVER = true` in `RetrofitInstance`. **Google Sign-In requires the real backend** — see Mockoon caveat below. See [05-offline-fallback.md](05-offline-fallback.md) for the full table, [project-environment.md](../project-environment.md) and [`mocks/README.md`](../../mocks/README.md).

**Seeded accounts** (backend `data.sql`; Spring OpenSpec `seed-data` / `DOCUMENTATION.md`):

| Email | Password | Role | Routes to |
|-------|----------|------|-----------|
| `admin@localhost` | `admin1234` | ADMIN | `HOME` |
| `alex.tan@example.sg` | `customer123` | USER | `CUSTOMER_BOOKINGS` |
| `ravi.kumar@example.sg` | `admin123` | ADMIN | `HOME` |
| `ah.tan@example.sg` | `driver123` | DRIVER | `HOME` |

A seeded login returning `invalid_credentials` means the backend hasn't restarted since `data.sql` last changed — it upserts `users` on every boot.

**Google test accounts:** the backend project (an OAuth "External" app in **Testing** publishing status) only authorizes accounts listed under Google Cloud Console → **Google Auth Platform → Audience → Test users**. Any other Google account is rejected during sign-in, not by this app but by Google itself, before an ID token is ever issued.

---

## Actors

- **Admin / operator** — field or office staff managing mobilisation and returns (`ROLE_ADMIN`/`ROLE_DRIVER`) — routes to `HOME`
- **Customer** — a booking's customer, viewing their own bookings only (`ROLE_USER`, password login only) — routes to `CUSTOMER_BOOKINGS`, see [06-customer-bookings.md](06-customer-bookings.md)

Beyond the staff/customer routing split above, authorisation is still out of scope for the
client — neither actor's session grants any further client-side permission model (e.g. staff
authorisation still relies entirely on the backend's own `403`s, per **L1**).

> **Correction (HR-78 / HR-242).** The backend has a role model: `User.role` is `USER` / `ADMIN` /
> `DRIVER`, and the access JWT carries a `roles` claim (Spring `auth-login-logout`). `LoginResponse`
> still exposes only `accessToken`, `tokenType`, `expiresIn`, and `username`. The client decodes
> `roles` from the JWT (`JwtClaims`, ADR-0008). See **L1** below.

### L1 — Role visibility and driver access *(partially resolved)*

The app varies UI by role for the staff vs customer split (`JwtClaims` + `onLoginSuccess`).
`ROLE_ADMIN` and `ROLE_DRIVER` both route to `HOME` (`isStaff()`). That is intentional.

**As-built Spring (do not restore the old lock-out note):** `ROLE_DRIVER` MAY call
`/api/bookings/**`, `/api/deliveries/**`, and `/api/returns/**` (`booking-delivery-return`,
HR-189). `ah.tan@example.sg` is a valid ops login. Other `/api/**` routes remain USER/ADMIN.

A list/status `403` is **not** applied as a local success: HR-93 withholds the local transition
on `HttpException` ([05-offline-fallback.md](05-offline-fallback.md) **O1**). Load failures still
use generic copy (**O2**).

### L2 — Google sign-in auto-provisions `ROLE_DRIVER` *(HR-152, corrected HR-242)*

A first-time Google sign-in auto-creates a backend `User` row with **`role = DRIVER`**
(Spring FR-AUTH-L-001b — this endpoint is the mobile ops sign-in path). It never auto-provisions
`ROLE_ADMIN`. If a Google account's email matches an **existing** user (any role), Google sign-in
links to that account and **does not change** its role.

An informal mobile ADR previously said first-time Google accounts were `ROLE_USER`. That is
**false** against the Spring SoT; canonical record is [ADR-0006](../../adr/0006-google-sign-in-credential-manager.md).

---

## Credentials and environments

| Environment | Credential behaviour | Display name after login |
|-------------|----------------------|---------------------------|
| **Mockoon / Prism (port 8081)** | Static canned responses — **does not** verify password or interim JWT. Typical requests receive 200. **`/api/auth/google` is not implemented on Mockoon/Prism** — see Google Sign-In section below. | From `LoginResponse.username` (fixture: `admin@localhost` → UI shows `Admin`) |
| **Real Spring Boot backend** (optional; often host port `8080`) | Validates interim JWT + email/password (see backend auth specs referenced from OpenAPI). Also the only environment that verifies real Google ID tokens. | From server `username` |
| **Login UI dev hint** | Shows all three seeded accounts (Customer/Driver/Admin) — see the dev seed panel row in UI notes below | — |

| Field | Dev seed (OpenAPI + UI hint) |
|-------|------------------------------|
| Email | `admin@localhost` |
| Password | `admin1234` |
| Display name derivation | Local-part of `username` before `@`, first character uppercased (e.g. `admin@localhost` → `Admin`) |

**Source of truth for request/response shapes:** OpenAPI schemas `LoginRequest`, `GoogleLoginRequest`, `LoginResponse`, and examples under [`api/examples/`](../api/examples/).  
**Do not** treat `MockDataRepository` as an auth credential store — it only seeds booking list data for offline fallback.

---

## Google Sign-In (HR-152)

Google Sign-In is offered as a **"Continue with Google"** button on `LoginScreen`, alongside the existing email/password form. It uses Android's **Credential Manager** (`androidx.credentials`) with `GetGoogleIdOption` to obtain a Google-issued ID token from an on-device Google account, then exchanges it for an access JWT the same way the password flow exchanges credentials.

### Requirements

- Android device/emulator must have **Google Play Services** (a Google Play–enabled system image on emulators; a bare "Google APIs" or AOSP image does not support the account picker).
- At least one Google account must be added on-device (**Settings → Passwords & accounts → Add account → Google**), and that account must be authorized as a test user in Google Cloud Console while the backend's OAuth app is in Testing status.
- `WEB_CLIENT_ID` in `LoginScreen.kt` must match the **Web application** OAuth client configured in Google Cloud Console (this is the audience the backend's `GoogleTokenVerifier` checks against — a separate **Android** OAuth client, keyed by package name + signing-key SHA-1, authorizes the app itself but is never referenced in code).

### Acceptance criteria

```gherkin
Feature: Google Sign-In

  Scenario: Successful Google sign-in opens the home screen
    Given the user is on the login screen
    And the user is not logged in
    And the auth API is reachable
    And a Google account is available via Credential Manager
    When the user taps "Continue with Google"
    And the user selects a Google account from the picker
    Then the client calls GET /api/auth/getBearerToken
    And the client calls POST /api/auth/google with the interim Bearer and the Google ID token
    And the access token is stored in the in-memory session
    And the user is marked as logged in
    And the current screen is HOME
    And no login error is shown

  Scenario: No Google account available on-device
    Given the user is on the login screen
    And no Google account is registered on the device (or Play Services rejects the request)
    When the user taps "Continue with Google"
    Then Credential Manager raises a GetCredentialException
    And the login error "Google sign-in was cancelled or failed. Please try again." is shown
    And no network call is made

  Scenario: Backend rejects the Google ID token
    Given the user is on the login screen
    And Credential Manager returns a Google ID token
    When the client calls POST /api/auth/google
    And the auth API returns HTTP 401
    Then the login error "Google sign-in was rejected. Please try again." is shown

  Scenario: Google sign-in requires a real backend
    Given USE_MOCK_SERVER is true (Mockoon/Prism target)
    When the user taps "Continue with Google" and completes the account picker
    Then POST /api/auth/google returns 404 (route not defined on the mock)
    And this is expected — Google Sign-In is not exercisable against Mockoon/Prism, only against the real Spring Boot backend
```

### Mockoon / Prism caveat

`POST /api/auth/google` is **not implemented** on Mockoon/Prism — unlike `getBearerToken`/`login`/`logout`, there is no canned response for it, and Google ID token verification cannot be meaningfully mocked without either a real Google-issued token or a stubbed verifier. Test Google Sign-In only against the real Spring Boot backend (`USE_MOCK_SERVER = false`).

---

## Acceptance criteria (email/password)

### Successful login (API available)

```gherkin
Feature: Admin login

  Scenario: Valid staff login against a reachable auth API opens the home screen
    Given the user is on the login screen
    And the user is not logged in
    And the auth API is reachable
    And the account is ROLE_ADMIN or ROLE_DRIVER
    When the user enters email and password
    And the user submits login
    Then the client calls GET /api/auth/getBearerToken
    And the client calls POST /api/auth/login with the interim Bearer and the credentials body
    And the access token is stored in the in-memory session
    And the user is marked as logged in
    And the display name is derived from LoginResponse.username
    And the current screen is HOME
    And no login error is shown

  Scenario: Valid customer login opens the read-only bookings screen instead
    Given the same steps as above, but the account is ROLE_USER
    Then the current screen is CUSTOMER_BOOKINGS, not HOME
```

The second scenario is the customer-login-bookings-view feature; see the full role-routing
table above and [06-customer-bookings.md](06-customer-bookings.md) for its own acceptance
criteria — it isn't repeated in full here to avoid the two documents drifting apart.

Against **Mockoon**, any non-empty body that the mock accepts still returns the canned success response (see mock caveat below).

### Network failure during login

```gherkin
  Scenario: Unreachable server keeps the user on login
    Given the user is on the login screen
    When the user submits login
    And the auth API is unreachable
    Then the user remains logged out
    And the login error "Could not reach the server. Please try again." is shown
    And the current screen remains LOGIN
```

Login **requires** a reachable auth API. Offline seed data does **not** allow entry without a successful login handshake. List/status offline behaviour is separate — see [05-offline-fallback.md](05-offline-fallback.md).

### HTTP error responses

```gherkin
  Scenario: Auth API returns a client error
    Given the user is on the login screen
    When the user submits login
    And the auth API returns HTTP 400
    Then the login error "Email and password are required." is shown

  Scenario: Invalid credentials or bad interim token
    Given the user is on the login screen
    When the user submits login
    And the auth API returns HTTP 401
    Then the login error "Invalid email or password." is shown

  Scenario: Wrong token type used for login
    Given the user is on the login screen
    When the user submits login
    And the auth API returns HTTP 403
    Then the login error "Unable to sign in — please try again." is shown
```

Other HTTP failures map to: `Login failed ({code}). Please try again.`

**Mockoon caveat:** the local mock typically returns **200** for auth routes and does **not** exercise 400/401/403. Use a real backend (or contract tests) to verify negative credential paths.

### Email handling (client)

```gherkin
  Scenario: Email is trimmed before the login request
    Given the user is on the login screen
    When the user enters email "  admin@localhost  "
    And the user enters password "admin1234"
    And the user submits login
    Then the LoginRequest email sent to the API is "admin@localhost"
```

Password is sent as entered (no trim). Email **case sensitivity** is defined by the server, not by client-side comparison.

### Logout

```gherkin
  Scenario: Logout returns to login and clears session state
    Given the user is logged in
    And an access token is present in the session
    When the user chooses logout
    Then the client attempts POST /api/auth/logout with the access Bearer
    And the in-memory session is cleared even if the logout call fails
    And the user is logged out
    And the current screen is LOGIN
    And session fields (display name, login error) are reset
```

Logout is identical for staff and customer sessions — `CustomerBookingsScreen`'s logout icon
calls the same `AppViewModel.logout()`, which clears `AppState` (including `isCustomer`) back
to its default and returns to `LOGIN` regardless of which role was signed in.

---

## UI notes (v1)

| Element | Behaviour |
|---------|-----------|
| Brand | "Heavy Rental" / "Sign in to your account" — role-neutral copy since staff and customers share this screen (was "Administrator Portal" before customer-login-bookings-view) |
| Email field | Email keyboard, next IME action |
| Password field | Visibility toggle |
| "Continue with Google" button | Below the Sign In button, separated by an "or" divider; disabled while `isLoggingIn`; staff-only in effect, see **Role routing** above |
| Error | Shown when `loginError` is non-null (shared between password and Google failures) |
| Dev seed panel | Shows all three seeded accounts — Customer, Driver, Admin — with their email/password, aligned with the seeded-accounts table above |
| Loading | Submit disabled / progress while `isLoggingIn` |

---

## Out of scope (v1)

- Any client-side role/permission model beyond the two-way staff/customer routing split described above — no in-app roles, groups, or per-screen permission checks
- Password reset, MFA, biometric login
- Secure storage of tokens or credentials (EncryptedSharedPreferences, Keystore)
- Token refresh / sliding expiry UX
- Offline or client-only login without calling the auth API
- Multi-user account switching
- Linking/unlinking a Google account from an existing password-based account via in-app UI (Google sign-in links by matching email automatically server-side; no client UI for this)
- Google Workspace domain restriction (`hd` claim) — the backend's OAuth app is "External"/unrestricted

---

## Implementation notes

| Concern | Location |
|---------|----------|
| Login / logout orchestration | `viewmodel/AppViewModel.kt` — `login`, `logout`, `loginWithGoogle`, `setLoginError` |
| Role routing after login | `viewmodel/AppViewModel.kt` — `onLoginSuccess(response, allowCustomer)`; `network/JwtClaims.kt` — `isStaff`, `isCustomer` |
| Interim → access handshake (password) | `data/repository/AuthRepository.kt` — `login` |
| Interim → access handshake (Google) | `data/repository/AuthRepository.kt` — `loginWithGoogle` |
| In-memory tokens | `network/TokenSession.kt` |
| Access Bearer on business calls | `network/AuthInterceptor.kt` |
| Paths | `network/dto/HeavyRentalApiService.kt` |
| Auth DTOs | `network/dto/AuthDtos.kt` — `LoginRequest`, `GoogleLoginRequest`, `LoginResponse` |
| Base URL (Spring Boot default) | `network/dto/RetrofitInstance.kt` — `http://10.0.2.2:8080/`; `USE_MOCK_SERVER = true` switches to `:8081` |
| UI | `ui/screens/LoginScreen.kt` — password form + "Continue with Google" (Credential Manager) |
| Google Web Client ID | `ui/screens/LoginScreen.kt` — `WEB_CLIENT_ID` constant |
| Unauthenticated shell | `MainActivity` / `HeavyRentalApp` shows only `LoginScreen` when `!isLoggedIn` |
| HTTP contract | `specification/api/heavyrental-openapi.yaml` |
| Auth fixtures | `specification/api/examples/interim-token.txt`, `login-response.json`, `logout-response.json`, `google-login-request.json` |

---

## Related specs

- [06-customer-bookings.md](06-customer-bookings.md) — the customer-side destination this screen routes `ROLE_USER` sessions to
- [api/README.md](../api/README.md) — base URLs, endpoint summary, mock commands
- [05-offline-fallback.md](05-offline-fallback.md) — list/status fallback after login (not login itself)
- [project-environment.md](../project-environment.md) — OpenAPI → Mockoon generation
- [ADR-0004](../../adr/0004-three-layer-mock-strategy.md) — mock layers including canned auth
- [ADR-0006](../../adr/0006-google-sign-in-credential-manager.md) — Credential Manager + backend-verified ID token; first-time Google → `ROLE_DRIVER`