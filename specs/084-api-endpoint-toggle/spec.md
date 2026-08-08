# Feature Specification: API endpoint configuration (Mockoon ↔ Spring Boot)

**Feature Branch**: `084-api-endpoint-toggle` / `HR-84-implement-toggle-switch-for-rest-api-endpoint-in-android-project`  
**Created**: 2026-08-08  
**Status**: Implemented (v1)  
**Input**: HR-84 — select Mockoon vs Spring Boot REST base URL without UI

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Configure API backend via properties (Priority: P1)

A developer sets the REST backend in a properties file, rebuilds, and the app
calls that host for all API traffic. There is **no** in-app toggle.

**Why this priority**: Core ticket value without shipping a developer control in the UI.

**Independent Test**: Set `api.server.target`, rebuild, confirm traffic to the matching host.

**Acceptance Scenarios**:

1. **Given** `api.server.target=MOCKOON` (default), **When** the app runs on the emulator, **Then** REST calls use `http://10.0.2.2:8081/` (host `localhost:8081`).
2. **Given** `api.server.target=SPRING_BOOT`, **When** the app is rebuilt and run, **Then** REST calls use `http://10.0.2.2:8080/` (host `localhost:8080`).
3. **Given** the same key is set in root `local.properties`, **When** the app is built, **Then** that value overrides `app/api.properties`.

### User Story 2 - No UI control (Priority: P1)

The Login screen (and the rest of the app) does not show an API endpoint switch.

**Why this priority**: Explicit product requirement.

**Independent Test**: Visual check of Login and authenticated shell.

**Acceptance Scenarios**:

1. **Given** any build type, **When** the user views Login, **Then** no API endpoint Switch or similar control is shown.

### Edge Cases

- Invalid `api.server.target` values fail the Gradle configuration with a clear error.  
- Physical devices cannot use `10.0.2.2` — custom LAN URL is out of scope for v1.  
- Changing the property requires Sync/Rebuild before it takes effect.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST default to Mockoon/Prism emulator base URL `http://10.0.2.2:8081/`.
- **FR-002**: System MUST support Spring Boot emulator base URL `http://10.0.2.2:8080/` when configured.
- **FR-003**: System MUST read the target from `app/api.properties` key `api.server.target` at build time.
- **FR-004**: System MUST allow override via root `local.properties` key `api.server.target`.
- **FR-005**: System MUST apply the selected host/port to all REST calls.
- **FR-006**: System MUST NOT expose an in-app UI control to change the API target.

### Key Entities

- **API server target**: `MOCKOON` | `SPRING_BOOT` with fixed emulator base URLs.
- **API properties**: Build-time configuration file(s).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Default properties → Mockoon `:8081` traffic from emulator.
- **SC-002**: `SPRING_BOOT` in properties + rebuild → Spring `:8080` traffic.
- **SC-003**: Login UI has no endpoint switch.

## Assumptions

- Primary client under test is the Android emulator.  
- Developers run Mockoon via `npm run mock:mockoon` and Spring Boot on host `localhost:8080`.  
- Paths and payloads remain the same OpenAPI contract on both servers.

## Out of scope (v1)

- In-app Switch / settings UI for the endpoint  
- Custom base URL / LAN IP text field  
- Runtime flip without rebuild  
- Auto-discovery of which server is reachable  

## Related

- Plan: [`plan.md`](./plan.md)  
- Tasks: [`tasks.md`](./tasks.md)  
- Config file: [`app/api.properties`](../../app/api.properties)  
- Login: [`001-admin-login`](../001-admin-login/spec.md)  
