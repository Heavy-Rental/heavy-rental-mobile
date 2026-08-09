# Feature: Admin login

**Status:** Implemented (v1)  
**Screens:** `LoginScreen`  
**Navigation:** `AppScreen.LOGIN` → success navigates to `AppScreen.HOME`  
**Code root:** `com.heavyrental`  
**API contract:** [`api/heavyrental-openapi.yaml`](../api/heavyrental-openapi.yaml) (Auth tag)

---

## Summary

Operators must authenticate before accessing Home, Deliveries, or Returns.

In v1, authentication is an **HTTP interim → access JWT handshake** defined in OpenAPI:

1. `GET /api/auth/getBearerToken` — mint a short-lived interim JWT (`text/plain`)
2. `POST /api/auth/login` — upgrade interim JWT + email/password to an access (session) JWT
3. Business calls use the access token as `Authorization: Bearer …` (via `AuthInterceptor`)
4. `POST /api/auth/logout` — revoke the access token (best-effort from the client)

Tokens are held **in memory only** (`TokenSession`). There is no secure storage or automatic refresh in v1.

**Default dev server (since HR-78):** the real Spring Boot backend on port **8080** — emulator base URL `http://10.0.2.2:8080/`. Mockoon/Prism on **8081** remains available via `USE_MOCK_SERVER = true` in `RetrofitInstance`. See [05-offline-fallback.md](05-offline-fallback.md) for the full table, [project-environment.md](../project-environment.md) and [`mocks/README.md`](../../mocks/README.md).

**Seeded accounts** (backend `data.sql`; `SPEC-auth-login-logout.md` §8.3):

| Email | Password | Role |
|-------|----------|------|
| `admin@localhost` | `admin1234` | ADMIN |
| `alex.tan@example.sg` | `customer123` | USER |
| `ravi.kumar@example.sg` | `admin123` | ADMIN |
| `ah.tan@example.sg` | `driver123` | DRIVER |

A seeded login returning `invalid_credentials` means the backend hasn't restarted since `data.sql` last changed — it upserts `users` on every boot.

---

## Actors

- **Admin / operator** — field or office staff managing mobilisation and returns

v1 treats every authenticated user as a single operator. Authorisation beyond “has a valid access token” is out of scope for the client.

> **Correction (HR-78).** The earlier wording — *“no roles API”* — is no longer accurate. The backend
> has had a role model since before this branch: `User.role` is `USER` / `ADMIN` / `DRIVER`, and the
> access JWT carries a `roles` claim (`SPEC-auth-login-logout.md` §4). What is missing is client
> *visibility*: `LoginResponse` exposes only `accessToken`, `tokenType`, `expiresIn`, and `username`,
> so the app has no server-provided role to adapt its UI with. See **L1** below.

### L1 — Roles are not visible to the client *(ticket: TBD)*

The app cannot vary its UI by role, and cannot anticipate an authorisation failure before making a
call. Two consequences worth recording:

- **`ROLE_DRIVER` is locked out of every business route today.** The backend's blanket rule grants
  only `ROLE_USER`/`ROLE_ADMIN` (`SPEC-api-index.md` §4), so `ah.tan@example.sg` authenticates
  successfully and then receives `403` on every list and status call — including the delivery and
  return endpoints the driver role exists for. That is a backend gap, tracked on their side.
- **A `403` currently renders as success**, because of the optimistic-update behaviour in
  [05-offline-fallback.md](05-offline-fallback.md) **O1** and the error classification in **O2**.

**Two possible routes**, not decided here: add `roles` to `LoginResponse` (a backend contract
change), or decode the `roles` claim from the JWT the app already holds (client-only, no contract
change). The second needs no coordination and is available today.

---

## Credentials and environments

| Environment | Credential behaviour | Display name after login |
|-------------|----------------------|---------------------------|
| **Mockoon / Prism (port 8081)** | Static canned responses — **does not** verify password or interim JWT. Typical requests receive 200. | From `LoginResponse.username` (fixture: `admin@localhost` → UI shows `Admin`) |
| **Real Spring Boot backend** (optional; often host port `8080`) | Validates interim JWT + email/password (see backend auth specs referenced from OpenAPI). | From server `username` |
| **Login UI dev hint** | Shows seed `admin@localhost` / `admin1234` (OpenAPI `LoginRequest` / example) | — |

| Field | Dev seed (OpenAPI + UI hint) |
|-------|------------------------------|
| Email | `admin@localhost` |
| Password | `admin1234` |
| Display name derivation | Local-part of `username` before `@`, first character uppercased (e.g. `admin@localhost` → `Admin`) |

**Source of truth for request/response shapes:** OpenAPI schemas `LoginRequest`, `LoginResponse`, and examples under [`api/examples/`](../api/examples/).  
**Do not** treat `MockDataRepository` as an auth credential store — it only seeds booking list data for offline fallback.

---

## Acceptance criteria

### Successful login (API available)

```gherkin
Feature: Admin login

  Scenario: Valid login against a reachable auth API opens the home screen
    Given the user is on the login screen
    And the user is not logged in
    And the auth API is reachable
    When the user enters email and password
    And the user submits login
    Then the client calls GET /api/auth/getBearerToken
    And the client calls POST /api/auth/login with the interim Bearer and the credentials body
    And the access token is stored in the in-memory session
    And the user is marked as logged in
    And the admin display name is derived from LoginResponse.username
    And the current screen is HOME
    And no login error is shown
```

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
    And session fields (admin name, login error) are reset
```

---

## UI notes (v1)

| Element | Behaviour |
|---------|-----------|
| Brand | "Heavy Rental" / "Administrator Portal" |
| Email field | Email keyboard, next IME action |
| Password field | Visibility toggle |
| Error | Shown when `loginError` is non-null |
| Dev seed panel | Shows OpenAPI-aligned seed email/password for local backends |
| Loading | Submit disabled / progress while `isLoggingIn` |

---

## Out of scope (v1)

- Roles / permissions beyond a single operator session
- Password reset, MFA, biometric login
- Secure storage of tokens or credentials (EncryptedSharedPreferences, Keystore)
- Token refresh / sliding expiry UX
- Offline or client-only login without calling the auth API
- Multi-user account switching

---

## Implementation notes

| Concern | Location |
|---------|----------|
| Login / logout orchestration | `viewmodel/AppViewModel.kt` — `login`, `logout` |
| Interim → access handshake | `data/repository/AuthRepository.kt` |
| In-memory tokens | `network/TokenSession.kt` |
| Access Bearer on business calls | `network/AuthInterceptor.kt` |
| Paths | `network/dto/HeavyRentalApiService.kt` |
| Auth DTOs | `network/dto/AuthDtos.kt` |
| Base URL (Spring Boot default) | `network/dto/RetrofitInstance.kt` — `http://10.0.2.2:8080/`; `USE_MOCK_SERVER = true` switches to `:8081` |
| UI | `ui/screens/LoginScreen.kt` |
| Unauthenticated shell | `MainActivity` / `HeavyRentalApp` shows only `LoginScreen` when `!isLoggedIn` |
| HTTP contract | `specification/api/heavyrental-openapi.yaml` |
| Auth fixtures | `specification/api/examples/interim-token.txt`, `login-response.json`, `logout-response.json` |

---

## Related specs

- [api/README.md](../api/README.md) — base URLs, endpoint summary, mock commands
- [05-offline-fallback.md](05-offline-fallback.md) — list/status fallback after login (not login itself)
- [project-environment.md](../project-environment.md) — OpenAPI → Mockoon generation
- [decisions/002-mock-strategy.md](../decisions/002-mock-strategy.md) — mock layers including canned auth
