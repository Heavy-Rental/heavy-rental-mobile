# Feature Specification: Offline / API failure fallback

**Feature Branch**: `005-offline-fallback`  
**Created**: 2026-08-08  
**Status**: Implemented (v1)  
**Input**: Remain usable when list/status APIs fail after authentication

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Lists remain usable without API (Priority: P1)

After the app has seed (or previous) list data, network failure on list load does not blank the UI; an error is shown.

**Why this priority**: Demo-ready and field-tolerant product requirement.

**Independent Test**: Start with seed; fail list GETs; confirm lists and banner.

**Acceptance Scenarios**:

1. **Given** the app starts, **Then** bookings/lists are initialised from seed so UI has content immediately.
2. **Given** seed or previous data is shown, **When** `GET /api/deliveries` and/or `GET /api/returns` succeeds, **Then** those lists are replaced with API payloads and the load error is cleared when all succeed.
3. **Given** seed lists are shown, **When** a list GET fails, **Then** the corresponding list remains seed/previous data and a human-readable network error is shown in the authenticated shell.
4. **Given** seed bookings are shown, **When** `GET /api/bookings` succeeds, **Then** bookings update without rebuilding deliveries/returns solely by re-filtering with device today.

### User Story 2 - Status changes still apply when PATCH fails (Priority: P1)

Allowed status transitions update local state even if the server cannot be reached.

**Why this priority**: Operators must record mobilisation/completion offline for demos/field.

**Independent Test**: Trigger mobilise/complete with mock server stopped.

**Acceptance Scenarios**:

1. **Given** an allowed transition, **When** the operator confirms and PATCH fails, **Then** local status still updates and the error explains local-only update.
2. **Given** an allowed transition, **When** PATCH succeeds, **Then** local status updates and the status-related network error is cleared.
3. **Given** a disallowed transition, **When** attempted, **Then** local state is unchanged (silent ignore).

### User Story 3 - Login is not offline (Priority: P1)

Auth failures stay on the login screen; seed data never authenticates.

**Why this priority**: Security and product boundary.

**Independent Test**: Attempt login with API down.

**Acceptance Scenarios**:

1. **Given** the user is on Login, **When** login is submitted and auth API is unreachable, **Then** the user remains logged out, login error is shown on Login, and the shell network banner is not used for that failure.

### Edge Cases

- Error banner appears only in the authenticated shell when `networkError` is set.  
- Load error copy pattern: `Could not reach API — showing mock data. ({detail})`.  
- Status error copy pattern: `Could not sync status update to API — updated locally only. ({detail})`.  
- Local status is applied after the API attempt completes (success or failure); no rollback on failure in v1.  
- No automatic offline queue or retry control in v1.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST never blank main delivery/return lists solely because a list API failed.
- **FR-002**: System MUST surface a human-readable error when list or status API sync fails (authenticated shell).
- **FR-003**: System MUST apply optimistic local updates for allowed status transitions when PATCH fails.
- **FR-004**: System MUST initialise list state from seed before the first successful API response.
- **FR-005**: System MUST NOT authenticate using seed data; login requires a successful auth handshake.
- **FR-006**: System MUST ignore disallowed status transitions without requiring an error dialog.

### Key Entities

- **Seed booking set**: Demo fixtures used until/unless API replaces list state.  
- **Network error message**: User-visible string for shell banner.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: With list APIs down after shell start, Deliveries and Returns still show non-empty seed (when seed is non-empty) and an error banner is visible when logged in.
- **SC-002**: Mobilise/complete with PATCH down still changes local status and shows the local-only error message.
- **SC-003**: Login with auth API down never enters the authenticated shell.

## Assumptions

- Default dev mock is Mockoon/Prism on host `:8081`; Spring optional on `:8080` (see `084-api-endpoint-toggle`).  
- Cleartext local-dev networking is allowed for documented hosts.

## Out of scope (v1)

- Persistent offline queue / auto-retry when back online  
- Conflict resolution if server status diverges  
- Full offline database (Room)  
- Airplane-mode specific UX beyond the banner  
- Manual “retry” control (relaunch / re-entry reloads)  

## Related

- Login: [`001-admin-login`](../001-admin-login/spec.md)  
- Deliveries / Returns: `003`, `004`  
- Index stub: [`specification/product/05-offline-fallback.md`](../../specification/product/05-offline-fallback.md)  
