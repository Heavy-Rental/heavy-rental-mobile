## ADDED Requirements

### Requirement: Feature specs are enumerated and current

The product SDD set MUST match `specification/README.md`.

#### Scenario: Contributor finds the index
- GIVEN the repository root
- WHEN they open `specification/README.md`
- THEN every product, domain, API, testing, and environment file is listed

### Requirement: Auth and role routing

Login MUST follow `specification/product/01-login.md`. First-time Google MUST provision `ROLE_DRIVER`.

#### Scenario: Google first-time user is a driver
- GIVEN a verified Google email with no `users` row
- WHEN `POST /api/auth/google` succeeds
- THEN the access JWT includes `ROLE_DRIVER`

### Requirement: Ops lists and status machine

Deliveries/returns MUST use the documented transitions. `ROLE_DRIVER` MUST NOT be documented as locked out.

#### Scenario: Mobilise
- GIVEN a CONFIRMED delivery
- WHEN the operator confirms mobilise
- THEN the client PATCHes `/api/deliveries/{id}/status`

### Requirement: Customer bookings are read-only

Password `ROLE_USER` MUST land on `CUSTOMER_BOOKINGS` with no status actions.

#### Scenario: Customer password login
- GIVEN `alex.tan@example.sg`
- WHEN login succeeds
- THEN the current screen is CUSTOMER_BOOKINGS

### Requirement: HTTP contract and mocks

OpenAPI MUST remain the client HTTP contract. Google login MUST be absent from the mock.

#### Scenario: OpenAPI google provision text
- GIVEN `/api/auth/google` description
- WHEN it mentions auto-provision
- THEN it states `ROLE_DRIVER`
