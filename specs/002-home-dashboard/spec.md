# Feature Specification: Home dashboard

**Feature Branch**: `002-home-dashboard`  
**Created**: 2026-08-08  
**Status**: Implemented (v1)  
**Input**: Post-login dashboard for today’s work overview

## User Scenarios & Testing *(mandatory)*

### User Story 1 - See today’s work overview (Priority: P1)

After login, the operator sees today’s delivery and return counts broken down by
status, plus a welcome line and today’s date.

**Why this priority**: Primary orientation after sign-in.

**Independent Test**: Log in with known list data; verify counts match today’s delivery/return lists.

**Acceptance Scenarios**:

1. **Given** the user is logged in and list data is available (API or seed), **When** the user views Home, **Then** delivery and return counts match the same lists used by Deliveries and Returns for today, the welcome line shows the admin name, and the date line shows today as `EEEE, d MMMM yyyy`.

### User Story 2 - Navigate and sign out (Priority: P2)

From Home, the operator reaches Deliveries/Returns via shell navigation and can log out.

**Why this priority**: Core app shell behaviour.

**Independent Test**: Use bottom navigation and logout control from Home.

**Acceptance Scenarios**:

1. **Given** the user is on Home, **When** the user selects Deliveries or Returns in the shell, **Then** the corresponding screen is shown.
2. **Given** the user is on Home, **When** the user chooses logout, **Then** the session ends per login/logout rules ([001-admin-login](../001-admin-login/spec.md)).

### User Story 3 - Counts track list load results (Priority: P2)

Dashboard counts follow successful list loads and fall back to seed when loads fail.

**Why this priority**: Demo and field continuity.

**Independent Test**: Load with API up vs down; compare Home counts to list screens.

**Acceptance Scenarios**:

1. **Given** the app shell starts, **When** list load is requested, **Then** deliveries and returns endpoints are called (and optionally bookings), and on success Home uses those payloads for counts.
2. **Given** a list API fails, **When** the user views Home, **Then** counts use seed/previous data and a shell network error may be shown ([005-offline-fallback](../005-offline-fallback/spec.md)).

### Edge Cases

- Counts are derived from delivery list items: total, Confirmed, Mobilised.  
- Counts are derived from return list items: total, Mobilised, Completed.  
- Home does not own deep links from tiles into filtered lists (out of scope).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST show brand, welcome with admin name, and today’s date on Home.
- **FR-002**: System MUST show today’s delivery overview: total, Confirmed count, Mobilised count.
- **FR-003**: System MUST show today’s return overview: total, Mobilised count, Completed count.
- **FR-004**: System MUST compute counts from the same delivery/return list state as the list screens (domain list membership rules apply to seed; server/mock applies membership for API payloads — see domain list-filters).
- **FR-005**: System MUST provide shell navigation among Home, Deliveries, and Returns when authenticated.
- **FR-006**: System MUST provide logout from Home.

### Key Entities

- **Delivery list item**: Booking scheduled for delivery today with Confirmed or Mobilised status (see domain filters / API).
- **Return list item**: Booking due for return today with Mobilised or Completed status.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Home Confirmed + Mobilised delivery counts equal the number of matching rows on the Deliveries screen for the same data set.
- **SC-002**: Home Mobilised + Completed return counts equal the number of matching rows on the Returns screen for the same data set.
- **SC-003**: Date string matches device-local today in `EEEE, d MMMM yyyy` form.

## Assumptions

- “Today” is the device local calendar date.
- List data is loaded by the app shell after composition (seed immediately, API when available).

## Out of scope (v1)

- Historical (non-today) dashboards  
- Charts / analytics  
- Push notifications  
- Multi-yard / multi-company switching  
- Deep links from overview tiles into filtered lists  

## Related

- Domain: [`specification/domain/list-filters.md`](../../specification/domain/list-filters.md)  
- Login: [`001-admin-login`](../001-admin-login/spec.md)  
- Deliveries / Returns / Offline: `003`, `004`, `005`  
- Index stub: [`specification/product/02-home-dashboard.md`](../../specification/product/02-home-dashboard.md)  
