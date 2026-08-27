# ADR-0006: Google Sign-In via Credential Manager and server-verified ID token

- Status: accepted
- Date: 2026-08-27
- Tags: auth, google

## Context

Product wants "Continue with Google" on login without replacing the interim → access JWT session model.

Options considered: Firebase Authentication (second identity system); full OAuth 2.0 authorization-code redirect (unnecessary for a native client); Android Credential Manager Sign in with Google + server-side ID token verification.

An earlier informal ADR (`specification/decisions/004-google-sign-in.md`) stated that first-time Google accounts are auto-provisioned as `ROLE_USER`. That is **not** the as-built Spring contract. Backend OpenSpec `auth-login-logout` FR-AUTH-L-001b: first-time Google sign-in provisions **`ROLE_DRIVER`** (never `ROLE_ADMIN`); an existing account keeps its role. The mobile app is a staff ops client; a first-time Google user is assumed to be a driver.

## Decision

Use **Android Credential Manager** (`androidx.credentials`, `GetGoogleIdOption`) on the client to obtain a Google-issued ID token, and verify it **server-side** (`GoogleIdTokenVerifier`). The verified email find-or-creates a `users` row; Spring mints the **same** access JWT as password login. `POST /api/auth/google` is a sibling of `POST /api/auth/login`.

Rules:

1. Auto-provisioned Google accounts get **`ROLE_DRIVER`**. Never auto-elevate to `ROLE_ADMIN`. Customer (`ROLE_USER`) is not the first-time Google path.
2. An existing account is matched by email; Google sign-in does **not** change its role.
3. The OAuth app is External / unrestricted audience (no Workspace `hd` restriction).
4. `POST /api/auth/google` is **not** implemented on Mockoon/Prism (ADR-0004 rule 6).
5. Client routing: Google success with `ROLE_ADMIN`/`ROLE_DRIVER` → Home. A `ROLE_USER` token obtained via Google is **refused** (`allowCustomer = false`) — password login is the only customer entry.

## Consequences

- No second identity system; JWT/session/denylist stay unchanged.
- Google Sign-In cannot be exercised against Mockoon/Prism.
- Requires a Google Play emulator image and Cloud Console Web + Android OAuth clients.
- Aligns with `heavy-rental-spring-rest-api` `openspec/specs/auth-login-logout/spec.md`.

## Alternatives considered

| Option | Why not |
|--------|---------|
| Firebase Authentication | Second identity system vs existing `users` + JWT |
| Full OAuth 2.0 authorization-code redirect | Built for web; the native client only needs an ID token |
| Trust client-reported email without server verification | Spoofable |
| Auto-provision `ROLE_USER` | Contradicts the staff-ops purpose of this app and the Spring as-built contract |
