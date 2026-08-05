# ADR 002: Mock strategy

## Status

Accepted (v1)

## Context

The app needs realistic data when:

- Developing UI without a live backend
- Demonstrating flows (login, mobilise, complete)
- Running automated tests
- The network is unavailable

Tools discussed: Mockoon, OkHttp MockWebServer, in-app `MockDataRepository`.

## Decision

Use **three complementary mocks**, all aligned to the same domain/API specs:

| Layer | Tool | When |
|-------|------|------|
| **In-app seed / offline** | `MockDataRepository` | App launch, API failure fallback |
| **Manual full-stack HTTP** | Mockoon or Prism on port **8081** | Run emulator against host mock |
| **Automated tests** | OkHttp **MockWebServer** | JVM unit/contract tests |

### Wiring (repo automation)

| Command | Server |
|---------|--------|
| `npm run mock:prism` | Prism from bundled OpenAPI |
| `npm run mock:mockoon` | Mockoon CLI from generated environment |
| `npm run mock:prepare` | Regenerate `mocks/` assets only |
| `npm run mock:verify` | HTTP smoke checks against `:8081` |

Implementation: `scripts/prepare-mocks.mjs`, `scripts/start-prism.mjs`, `scripts/start-mockoon.mjs`.  
Docs: [`mocks/README.md`](../../mocks/README.md).  
Source inputs: `specification/api/heavyrental-openapi.yaml` + `specification/api/examples/`.

Rules:

1. JSON field names and enums must match `specification/api/heavyrental-openapi.yaml`.
2. Domain examples should stay consistent with `specification/api/examples/` and product scenarios.
3. MockWebServer is **not** the primary manual mock server (prefer Mockoon/Prism for that).
4. Optimistic UI updates on PATCH failure remain product behaviour ([05-offline-fallback.md](../product/05-offline-fallback.md)).
5. Generated files under `mocks/.generated/` are not hand-edited; change OpenAPI/examples and re-run prepare.
6. **Auth on Mockoon/Prism is static:** getBearerToken / login / logout return canned fixtures. They do **not** sign real JWTs or verify passwords. Use a real Spring backend (or MockWebServer scenarios) for credential-failure and token-lifecycle testing. Product caveats: [01-login.md](../product/01-login.md).

## Consequences

**Positive**

- Developers can work without backend or Mockoon installed (seed data)
- Optional Mockoon for integration realism
- Tests stay fast and hermetic with MockWebServer

**Negative / trade-offs**

- Three places can drift — discipline via specs + PR checklist
- Fixture dates in `examples/` are fixed; in-app seed uses `LocalDate.now()`
- Mock auth cannot replace a real identity server for negative login tests

## Alternatives considered

| Option | Why not as sole approach |
|--------|---------------------------|
| Mockoon only | Blocks offline demo; not great in CI |
| MockWebServer only | Poor manual DX; not a long-running GUI mock |
| Seed only | Never exercises real HTTP client code paths |
