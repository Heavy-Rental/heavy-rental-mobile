# ADR-0004: Three complementary mock layers

- Status: accepted
- Date: 2026-08-27
- Tags: mocks, testing

## Context

The app needs realistic data when developing UI without a live backend, demonstrating flows, running automated tests, and when the network is unavailable after login.

Tools discussed: Mockoon, OkHttp MockWebServer, in-app `MockDataRepository`.

Predecessor: informal `specification/decisions/002-mock-strategy.md`.

## Decision

Use **three complementary mocks**, all aligned to the same domain/API specs:

| Layer | Tool | When |
|-------|------|------|
| **In-app seed / offline** | `MockDataRepository` | App launch, API failure fallback |
| **Manual full-stack HTTP** | Mockoon or Prism on port **8081** | Run emulator against host mock |
| **Automated tests** | OkHttp **MockWebServer** | JVM unit/contract tests |

### Wiring

| Command | Server |
|---------|--------|
| `npm run mock:prism` | Prism from bundled OpenAPI |
| `npm run mock:mockoon` | Mockoon CLI from generated environment |
| `npm run mock:prepare` | Regenerate `mocks/` assets only |
| `npm run mock:verify` | HTTP smoke checks against `:8081` |

Rules:

1. JSON field names and enums MUST match `specification/api/heavyrental-openapi.yaml`.
2. Domain examples SHOULD stay consistent with `specification/api/examples/` and product scenarios.
3. MockWebServer is **not** the primary manual mock server.
4. Optimistic UI updates on PATCH failure remain product behaviour ([05-offline-fallback.md](../specification/product/05-offline-fallback.md)), split by failure type (HR-93).
5. Generated files under `mocks/.generated/` are not hand-edited.
6. **Auth on Mockoon/Prism is static:** getBearerToken / login / logout return canned fixtures. They do **not** sign real JWTs or verify passwords. `POST /api/auth/google` is **not** implemented on the mock.
7. **One exception to static fixtures:** the return-status route echoes `bookingStatus`/`returnNotes` (ADR-0005).

## Consequences

- Developers can work without a backend (`MockDataRepository`) or with HTTP (`Mockoon`/`Prism`).
- Tests stay hermetic with MockWebServer.
- Three places can drift — discipline via specs + PR checklist.
- Fixture dates in `examples/` are fixed; in-app seed uses `LocalDate.now()`.
- Mock auth cannot replace Spring for negative login tests or Google Sign-In.
