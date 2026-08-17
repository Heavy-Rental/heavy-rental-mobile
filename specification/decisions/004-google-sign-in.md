# ADR 004: Google Sign-In implementation approach

## Status

Accepted (HR-152)

## Context

Product wants a "Continue with Google" option on login, alongside the existing email/password flow, without a full rewrite of the existing interim → access JWT session model.

Options considered: Firebase Authentication (managed, but introduces a new backend dependency and a second identity system to reconcile with the existing `users` table); a full OAuth 2.0 authorization-code redirect flow (unnecessary complexity for a native Android client); Android Credential Manager's Sign in with Google + server-side ID token verification (minimal new surface area, reuses the existing JWT session model).

## Decision

Use **Android Credential Manager** (`androidx.credentials`, `GetGoogleIdOption`) on the client to obtain a Google-issued ID token, and verify it **server-side** with Google's own `google-api-client` (`GoogleIdTokenVerifier`) rather than trusting the client. The verified token's email is used to find-or-create a row in the existing `users` table, and the server mints the **same kind of access JWT** the password flow mints — `POST /api/auth/google` is a sibling of `POST /api/auth/login`, not a separate identity system.

Rules:

1. Auto-provisioned Google accounts always get `ROLE_USER`. Never auto-elevate to `ROLE_ADMIN`/`ROLE_DRIVER` — those remain a manual `POST /api/users` action.
2. An existing account is matched by email; Google sign-in links to it rather than creating a duplicate.
3. The OAuth app is "External" / unrestricted audience (no Google Workspace domain), since there is no company Workspace domain — see [product/01-login.md](../product/01-login.md) L2.
4. `POST /api/auth/google` is **not** implemented on Mockoon/Prism — real Google ID token verification cannot be meaningfully faked, and adding a canned-but-unverified route would contradict ADR 002 rule 6 (auth mocks are static and explicitly not a substitute for real credential testing).

## Consequences

**Positive**

- No new backend identity system or dependency beyond `google-api-client`; the existing JWT/session/denylist model is reused unchanged.
- Client and server auth code follow the same shape as the password flow (`AuthRepository`/`AuthService` gain a sibling method each, not a parallel stack).

**Negative / trade-offs**

- Google Sign-In cannot be exercised against Mockoon/Prism — developers need the real Spring Boot backend running to test or demo it.
- Requires a Google Play–enabled emulator system image (not "Google APIs" or AOSP) for local testing, and a Google Cloud Console project with a Web application OAuth client (verification audience) and an Android OAuth client (package name + signing-key SHA-1) per signing key (debug and release).
- New Google accounts self-provision on first sign-in — see [product/01-login.md](../product/01-login.md) L2 for the role-assignment rule this relies on.

## Alternatives considered

| Option | Why not |
|--------|---------|
| Firebase Authentication | New backend dependency and a second identity system to reconcile against the existing `users` table; the JWT/session model already exists and works |
| Full OAuth 2.0 authorization-code redirect | Built for web/server-to-server flows; unnecessary complexity for a native Android client that just needs an ID token |
| Trust the client-reported email without server-side token verification | Trivially spoofable — any client could claim to be any email |