# Feature Specification: Admin login

**Feature Branch**: `001-admin-login`  
**Created**: 2026-08-08  
**Status**: Implemented (v1)  
**Input**: Operator authentication before accessing operations screens

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Sign in with credentials (Priority: P1)

An operator opens the app, enters email and password, and reaches the home
dashboard after a successful auth handshake against a reachable API.

**Why this priority**: No other feature is usable without authentication.

**Independent Test**: With Mockoon or Spring running, submit valid credentials
from the login screen and confirm navigation to Home with a welcome name.

**Acceptance Scenarios**:

1. **Given** the user is on the login screen and not logged in, and the auth API is reachable, **When** the user enters email and password and submits login, **Then** the client performs the interim → access JWT handshake, stores the access token in the in-memory session, marks the user logged in, derives the admin display name from the login response username, navigates to Home, and shows no login error.
2. **Given** the user is on the login screen, **When** the user enters email with surrounding whitespace and submits, **Then** the email sent to the API is trimmed; password is sent as entered.

### User Story 2 - Handle login failures (Priority: P1)

An operator remains on the login screen with a clear error when the server is
unreachable or rejects the attempt.

**Why this priority**: Field reliability and security messaging.

**Independent Test**: Stop the API and attempt login; then (on Spring) use wrong password.

**Acceptance Scenarios**:

1. **Given** the user is on the login screen, **When** login is submitted and the auth API is unreachable, **Then** the user remains logged out, the error “Could not reach the server. Please try again.” is shown, and the screen remains Login.
2. **Given** the user is on the login screen, **When** the auth API returns HTTP 400, **Then** the error “Email and password are required.” is shown.
3. **Given** the user is on the login screen, **When** the auth API returns HTTP 401, **Then** the error “Invalid email or password.” is shown.
4. **Given** the user is on the login screen, **When** the auth API returns HTTP 403, **Then** the error “Unable to sign in — please try again.” is shown.
5. **Given** other HTTP failures, **Then** the error is `Login failed ({code}). Please try again.`

### User Story 3 - Sign out (Priority: P2)

A logged-in operator ends the session and returns to the login screen.

**Why this priority**: Required for multi-operator devices and demos.

**Independent Test**: Log in, choose logout, confirm Login screen and cleared session.

**Acceptance Scenarios**:

1. **Given** the user is logged in with an access token, **When** the user chooses logout, **Then** the client attempts logout against the API with the access Bearer, clears the in-memory session even if the call fails, marks the user logged out, shows Login, and resets session fields (admin name, login error).

### Edge Cases

- Mockoon/Prism returns canned 200 for auth and does **not** verify passwords; negative credential tests require Spring Boot or contract tests.
- Seed booking data MUST NOT allow entry without a successful auth handshake.
- Email case sensitivity is defined by the server, not client-side comparison.
- Concurrent double-submit while login is in progress is ignored (loading state).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST require authentication before Home, Deliveries, or Returns are available.
- **FR-002**: System MUST authenticate via interim JWT then access JWT as defined in OpenAPI Auth routes (`getBearerToken` → `login`).
- **FR-003**: System MUST attach the access token as Bearer on business API calls after login.
- **FR-004**: System MUST store tokens in memory only for v1 (lost on process death).
- **FR-005**: System MUST surface login failures on the login screen (not the authenticated shell banner).
- **FR-006**: System MUST trim email before sending; MUST NOT trim password.
- **FR-007**: System MUST attempt server logout then always clear local session.
- **FR-008**: System MUST derive display name from login response username (local-part before `@`, first character uppercased) when presenting “Welcome”.
- **FR-009**: API host selection is build-time only via properties (see `084-api-endpoint-toggle`); Login MUST NOT show an endpoint switch.

### Key Entities

- **Operator session**: Logged-in flag, display name, access token, optional interim token during handshake.
- **Login credentials**: Email + password (OpenAPI `LoginRequest`).
- **Auth tokens**: Interim (single-use) and access (session) JWTs as opaque strings to the client.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: With a reachable auth API and accepted credentials, operator reaches Home in one successful submit.
- **SC-002**: With API down, operator never enters the authenticated shell from seed data alone.
- **SC-003**: After logout, no access token remains and Login is shown.
- **SC-004**: On Spring Boot, invalid password yields 401 messaging; on Mockoon, canned success is documented and expected.

## Assumptions

- Single operator role in v1 (no roles API).
- Dev seed credentials for local backends: `admin@localhost` / `admin1234` (OpenAPI examples / UI hint).
- Default API target is Mockoon on host `:8081` (emulator `10.0.2.2:8081`); Spring on host `localhost:8080` is optional for real credential checks.

## Out of scope (v1)

- Roles / permissions beyond a single operator session  
- Password reset, MFA, biometric login  
- Secure storage of tokens or credentials  
- Token refresh / sliding expiry UX  
- Offline or client-only login without calling the auth API  
- Multi-user account switching  

## Related

- Domain: n/a (auth rules owned by API contract)  
- API: [`specification/api/heavyrental-openapi.yaml`](../../specification/api/heavyrental-openapi.yaml) (Auth)  
- Offline lists (not login): [`005-offline-fallback`](../005-offline-fallback/spec.md)  
- API toggle: [`084-api-endpoint-toggle`](../084-api-endpoint-toggle/spec.md)  
- Index stub: [`specification/product/01-login.md`](../../specification/product/01-login.md)  
