# Feature Specification: Today's deliveries

**Feature Branch**: `003-deliveries`  
**Created**: 2026-08-08  
**Status**: Implemented (v1)  
**Input**: Manage equipment deliveries scheduled for today

## User Scenarios & Testing *(mandatory)*

### User Story 1 - View and filter today’s deliveries (Priority: P1)

An operator opens the Deliveries screen and sees today’s delivery items, can filter by status, and open the project location on a map.

**Why this priority**: Core field workflow for mobilisation.

**Independent Test**: Load deliveries (API or seed); exercise filters and maps intent.

**Acceptance Scenarios**:

1. **Given** list data is available, **When** the operator opens Deliveries, **Then** the list shows delivery items from `GET /api/deliveries` when that call succeeded, or seed-derived deliveries on failure, with title “Delivery List” and subtitle `{visible} of {total} deliveries`.
2. **Given** delivery items are shown, **When** the operator selects Show All / Confirmed / Mobilised, **Then** only matching rows appear, default is Show All, and visible rows sort by customer name ascending.
3. **Given** a delivery with a project location, **When** the operator chooses Open in Google Maps, **Then** a geo/maps intent opens for that location (Google Maps app preferred; web maps fallback).

### User Story 2 - Mark confirmed delivery as mobilised (Priority: P1)

An operator confirms mobilisation for a Confirmed delivery; status becomes Mobilised locally and the API is notified.

**Why this priority**: Primary status action for deliveries.

**Independent Test**: Mobilise a Confirmed item with API up and with API down.

**Acceptance Scenarios**:

1. **Given** a delivery with status Confirmed, **When** the operator confirms “Mark as Mobilised?”, **Then** status becomes Mobilised, local list updates for that booking id, and the app attempts PATCH with `bookingStatus` = `MOBILISED`.
2. **Given** a delivery that is not Confirmed, **When** an invalid transition is attempted, **Then** status is unchanged.
3. **Given** a Confirmed delivery, **When** mobilise is confirmed and PATCH fails, **Then** local status still becomes Mobilised and a network error explains local-only update ([005-offline-fallback](../005-offline-fallback/spec.md)).

### Edge Cases

- Mobilise action is only offered for Confirmed items.  
- Confirmation dialog explains the action cannot be undone.  
- Client MUST NOT drop API rows solely because `startDate` differs from device “today” after a successful list load (server/mock owns membership).  
- Seed membership uses domain rules: startDate == today AND status Confirmed or Mobilised.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST load the delivery list primarily via `GET /api/deliveries`.
- **FR-002**: System MUST support client-side filters: All, Confirmed, Mobilised.
- **FR-003**: System MUST show booking id, status badge, asset name, serial, quantity (if > 1), customer, location, maps action.
- **FR-004**: System MUST allow only Confirmed → Mobilised for delivery status updates.
- **FR-005**: System MUST require confirmation before mobilising.
- **FR-006**: System MUST apply optimistic local status update when PATCH fails (with error message).
- **FR-007**: System MUST NOT rebuild the delivery list solely by re-filtering `GET /api/bookings` with device today after a successful deliveries load.

### Key Entities

- **Delivery item**: Identified by booking id; includes customer, asset, serial, quantity, location, status, start-related scheduling.
- **Status update request**: Target booking status value (`MOBILISED`).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Operator can filter to only Confirmed items and mobilise one end-to-end (local status Mobilised).
- **SC-002**: With PATCH forced to fail, local Mobilised state still appears and error banner/message is shown.
- **SC-003**: Maps control opens a location for a non-empty project location string.

## Assumptions

- Domain status machine and list filters in `specification/domain/` apply.  
- OpenAPI examples under `api/examples/deliveries.json` drive Mockoon fixtures.

## Out of scope (v1)

- Rescheduling start date  
- Partial quantity delivery  
- Photo / signature on mobilise  
- Assigning truck or driver  

## Related

- Domain: [`list-filters.md`](../../specification/domain/list-filters.md), [`booking-status-machine.md`](../../specification/domain/booking-status-machine.md)  
- API: OpenAPI Deliveries tag  
- Index stub: [`specification/product/03-deliveries.md`](../../specification/product/03-deliveries.md)  
