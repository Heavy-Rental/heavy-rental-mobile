# Product Features Specification

## Purpose

Index of product SDD under `specification/`. OpenSpec does not duplicate every UI requirement; each file below remains the detailed contract for that feature. Agents MUST load the linked file before changing that area.

## Requirements

### Requirement: Feature specs are enumerated and current

The product SDD set MUST match `specification/README.md`. Adding, renaming, or retiring a feature spec MUST update that README and this requirement's scenario list in the same change.

#### Scenario: Contributor finds the index
- GIVEN the repository root
- WHEN they open `specification/README.md` or `openspec/specs/product-features/spec.md`
- THEN every `specification/product/*.md`, `domain/*.md`, API, setup, testing, and environment file is listed with its role

### Requirement: Auth and role routing

Login MUST follow `specification/product/01-login.md`: interim → access JWT; password and Google paths; staff (`ROLE_ADMIN`/`ROLE_DRIVER`) → Home; password `ROLE_USER` → Customer Bookings; Google `ROLE_USER` refused. First-time Google provision is **`ROLE_DRIVER`** (Spring FR-AUTH-L-001b, ADR-0006). Client routing decodes JWT `roles` (ADR-0008).

#### Scenario: Driver password login
- GIVEN seeded `ah.tan@example.sg` / `driver123`
- WHEN the user submits email/password login against Spring
- THEN the session is staff
- AND the current screen is HOME
- AND subsequent `GET /api/deliveries` is allowed for `ROLE_DRIVER`

#### Scenario: Google first-time user is a driver
- GIVEN a verified Google email that matches no `users` row
- WHEN `POST /api/auth/google` succeeds
- THEN the access JWT `roles` include `ROLE_DRIVER`
- AND the app routes to HOME

### Requirement: Ops lists and status machine

Home, deliveries, returns, and offline fallback MUST follow `specification/product/02-home-dashboard.md`, `03-deliveries.md`, `04-returns.md`, `05-offline-fallback.md` and `specification/domain/*`. Allowed transitions: `CONFIRMED` → `MOBILISED` (deliveries), `MOBILISED` → `COMPLETED` (returns). List payloads MUST use `items[]` (all booking assets). `ROLE_DRIVER` MUST NOT be documented as locked out of these APIs.

#### Scenario: Mobilise
- GIVEN a CONFIRMED delivery
- WHEN the operator confirms mobilise
- THEN the client PATCHes `/api/deliveries/{id}/status` with `MOBILISED`
- AND on `IOException` the local status still becomes MOBILISED
- AND on `HttpException` the local status is unchanged

### Requirement: Customer bookings are read-only

`ROLE_USER` password sessions MUST land on `AppScreen.CUSTOMER_BOOKINGS` per `specification/product/06-customer-bookings.md`. That screen MUST NOT offer status updates.

#### Scenario: Customer password login
- GIVEN `alex.tan@example.sg` / `customer123`
- WHEN login succeeds
- THEN the current screen is CUSTOMER_BOOKINGS
- AND no mobilise/complete control is shown

### Requirement: HTTP contract and mocks

`specification/api/heavyrental-openapi.yaml` MUST remain this repo's client HTTP contract (ADR-0003). Mockoon/Prism MUST follow ADR-0004/0005. Google login MUST remain absent from the mock.

#### Scenario: OpenAPI google provision text
- GIVEN `heavyrental-openapi.yaml` `/api/auth/google` description
- WHEN it mentions auto-provision
- THEN it states `ROLE_DRIVER`, not `ROLE_USER`
