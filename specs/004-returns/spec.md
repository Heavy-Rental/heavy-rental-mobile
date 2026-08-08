# Feature Specification: Today's returns

**Feature Branch**: `004-returns`  
**Created**: 2026-08-08  
**Status**: Implemented (v1)  
**Input**: Manage equipment returns due today

## User Scenarios & Testing *(mandatory)*

### User Story 1 - View and filter today’s returns (Priority: P1)

An operator opens the Returns screen and sees today’s return items, can filter by status, and open the project location on a map.

**Why this priority**: Core field workflow for completing rentals.

**Independent Test**: Load returns (API or seed); exercise filters and maps.

**Acceptance Scenarios**:

1. **Given** list data is available, **When** the operator opens Returns, **Then** the list shows return items from `GET /api/returns` when that call succeeded, or seed-derived returns on failure, with title “Return List” and subtitle `{visible} of {total} returns`.
2. **Given** return items are shown, **When** the operator selects Show All / Mobilised / Completed, **Then** only matching rows appear, default is Show All, and visible rows sort by customer name ascending.
3. **Given** a return with a project location, **When** the operator chooses Open in Google Maps, **Then** a geo/maps intent opens (Google Maps preferred; web fallback).

### User Story 2 - Mark mobilised return as completed (Priority: P1)

An operator completes a Mobilised return; status becomes Completed locally and the API is notified.

**Why this priority**: Primary status action for returns.

**Independent Test**: Complete a Mobilised item with API up and with API down.

**Acceptance Scenarios**:

1. **Given** a return with status Mobilised, **When** the operator confirms complete, **Then** status becomes Completed, local list updates, and the app attempts PATCH with `bookingStatus` = `COMPLETED`.
2. **Given** a return that is not Mobilised, **When** an invalid transition is attempted, **Then** status is unchanged.
3. **Given** a Mobilised return, **When** complete is confirmed and PATCH fails, **Then** local status still becomes Completed and a network error explains local-only update.

### Edge Cases

- Complete action is only offered for Mobilised items.  
- Client MUST NOT drop API rows solely because `endDate` differs from device “today” after a successful list load.  
- Seed membership uses domain rules: endDate == today AND status Mobilised or Completed.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST load the return list primarily via `GET /api/returns`.
- **FR-002**: System MUST support client-side filters: All, Mobilised, Completed.
- **FR-003**: System MUST show booking id, status badge, asset name, serial, quantity (if > 1), customer, location, maps action.
- **FR-004**: System MUST allow only Mobilised → Completed for return status updates.
- **FR-005**: System MUST require confirmation before completing a return.
- **FR-006**: System MUST apply optimistic local status update when PATCH fails (with error message).
- **FR-007**: System MUST NOT rebuild the return list solely by re-filtering bookings with device today after a successful returns load.

### Key Entities

- **Return item**: Identified by booking id; includes customer, asset, serial, quantity, location, status, end-related scheduling.
- **Status update request**: Target booking status value (`COMPLETED`).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Operator can filter to Mobilised items and complete one end-to-end (local status Completed).
- **SC-002**: With PATCH forced to fail, local Completed state still appears and error is shown.
- **SC-003**: Maps control opens a location for a non-empty project location string.

## Assumptions

- Domain status machine and list filters apply.  
- OpenAPI examples under `api/examples/returns.json` drive Mockoon fixtures.

## Out of scope (v1)

- Damage / inspection checklist  
- Late fees or overhire calculation  
- Partial returns  
- Changing end date  

## Related

- Domain: [`list-filters.md`](../../specification/domain/list-filters.md), [`booking-status-machine.md`](../../specification/domain/booking-status-machine.md)  
- API: OpenAPI Returns tag  
- Index stub: [`specification/product/04-returns.md`](../../specification/product/04-returns.md)  
