# ADR-0008: Client decodes JWT roles for staff vs customer routing

- Status: accepted
- Date: 2026-08-27
- Tags: auth, roles, navigation

## Context

`LoginResponse` exposes `accessToken`, `tokenType`, `expiresIn`, and `username` only. It does not include a `roles` field. Spring puts authorities on the access JWT `roles` claim.

The app is shared by staff (`ROLE_ADMIN`, `ROLE_DRIVER`) and customers (`ROLE_USER`). Staff need Home / Deliveries / Returns. Customers must not see ops actions. Password login cannot be refused at the API for `ROLE_USER` because the same `/api/auth/login` serves the web portal.

HR-198 blocked customer login client-side. HR-215 added a read-only customer bookings screen instead.

## Decision

1. Decode the access token payload in `JwtClaims` **without** verifying the signature (the token was just issued over TLS by our own login/google call).
2. `isStaff` = `ROLE_ADMIN` or `ROLE_DRIVER`. `isCustomer` = `ROLE_USER`. Staff wins if both appear.
3. `onLoginSuccess(..., allowCustomer)`:
   - staff → `HOME`
   - customer and `allowCustomer` → `CUSTOMER_BOOKINGS`
   - otherwise revoke the token and stay on `LOGIN`
4. Password login sets `allowCustomer = true`. Google login sets `allowCustomer = false` (ADR-0006).
5. The client MUST NOT implement a broader permission model. Deliveries/returns/bookings authorization remains the server's (`403`). As-built Spring: `ROLE_DRIVER` **is** allowed on `/api/bookings`, `/api/deliveries`, and `/api/returns`.

## Consequences

- No backend contract change required for routing.
- Signature is not re-checked client-side; a compromised process could mint a fake JWT, which is out of v1 threat model (tokens are in-memory only).
- Older notes that "`ROLE_DRIVER` is locked out of every business route" are **false** against current Spring OpenSpec and MUST not be restored.
