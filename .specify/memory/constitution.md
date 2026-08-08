# Heavy Rental Mobile — Project Constitution

**Status:** Active  
**Aligned with:** GitHub Spec Kit (constitutional foundation)  
**Scope:** Android mobile client for Heavy Rental operations

This constitution is the set of **non-negotiable principles** that every feature
specification, plan, REASONS prompt, and implementation MUST respect. When a
change would violate an article, either update the constitution via the amendment
process or redesign the change.

---

## Article I — Specifications before or with code

Behaviour changes MUST be expressed in a Spec Kit feature `spec.md` (under
`specs/`) **before or in the same change** as application code. Code is the
expression of the specification, not the other way around.

## Article II — Layered sources of truth

| Concern | Canonical source |
|---------|------------------|
| User-visible behaviour | `specs/###-feature/spec.md` |
| HTTP contract | `specification/api/heavyrental-openapi.yaml` |
| Domain invariants | `specification/domain/` |
| Architecture decisions | `specification/decisions/` |
| Implementation design (non-trivial) | `spdd/prompt/` REASONS Canvas |

Conflict order: **product intent (spec) → domain rules → OpenAPI → implementation**.
Then align all four.

## Article III — OpenAPI is the API source of truth

Client HTTP paths, methods, schemas, and examples MUST derive from OpenAPI.
Do not invent endpoints in app code first. See ADR 001.

## Article IV — Mock strategy (three complementary layers)

1. **In-app seed** (`MockDataRepository`) — demo / offline list fallback only (not auth).  
2. **Manual HTTP mock** — Mockoon or Prism on host port **8081** (default).  
3. **Automated tests** — OkHttp MockWebServer for hermetic JVM tests.

Mockoon/Prism auth is **canned** (no real password/JWT verification). Real
credential behaviour requires Spring Boot (host `localhost:8080`). See ADR 002.

## Article V — Platform and stack defaults (v1)

- **Language:** Kotlin; **UI:** Jetpack Compose + Material3  
- **Networking:** Retrofit + OkHttp + kotlinx.serialization  
- **Min SDK / target:** as declared in `app/build.gradle.kts`  
- **Auth model:** interim JWT → access JWT; tokens **in memory only** (v1)  
- Cleartext HTTP only for documented local-dev hosts (`10.0.2.2`, etc.)

## Article VI — Product behaviour invariants (v1)

1. **Login requires a reachable auth API** — seed data MUST NOT authenticate.  
2. **List screens load from** `GET /api/deliveries` and `GET /api/returns` (not client re-filter of all bookings with device “today” as the sole source after a successful API load).  
3. **Allowed status transitions only:** `CONFIRMED → MOBILISED` (deliveries), `MOBILISED → COMPLETED` (returns). Invalid transitions are ignored.  
4. **Optimistic status updates:** on PATCH failure, local state still updates and a network error is shown.  
5. **Never blank main lists** solely because the network failed — keep seed/previous data and surface an error.

## Article VII — Simplicity

- Prefer the smallest change that satisfies the feature `spec.md`.  
- No speculative features (“might need later”).  
- No secure token storage, Room offline queue, or multi-role auth until specified.  
- Maximum complexity must be justified in a plan’s Complexity Tracking section.

## Article VIII — Anti-abstraction

- Use framework APIs directly (Compose, Retrofit, SharedPreferences) unless a thin
  boundary is required for testability or multi-backend host selection.  
- Prefer one representation for a domain concept (DTO map at the edge, domain model in UI/state).

## Article IX — Verification

- Feature specs MUST include testable **FR-###** and measurable **SC-###**.  
- Acceptance scenarios use **Given / When / Then** (or Gherkin).  
- Prefer contract alignment with OpenAPI examples and manual QA in
  `specification/testing-guide.md`.  
- Non-trivial design work SHOULD include an OpenSPDD REASONS prompt under `spdd/prompt/`.

## Article X — Dev API targets

| Target | Host | Emulator base URL |
|--------|------|-------------------|
| Mockoon / Prism (default) | `localhost:8081` | `http://10.0.2.2:8081/` |
| Spring Boot | `localhost:8080` | `http://10.0.2.2:8080/` |

Default remains Mockoon. Runtime switching (when specified) MUST clear the auth
session. Debug-only developer affordances MUST NOT appear in release builds unless
explicitly specified.

---

## Amendment process

1. Document rationale and impact on existing features.  
2. Update this file and link from the PR.  
3. Ensure active `specs/` and OpenAPI remain consistent.  
4. Prefer additive clarification over silent reinterpretation of past behaviour.

## Related

- [specs/README.md](../../specs/README.md)  
- [spdd/README.md](../../spdd/README.md)  
- [specification/README.md](../../specification/README.md)  
- [specification/decisions/001-openapi-as-api-source.md](../../specification/decisions/001-openapi-as-api-source.md)  
- [specification/decisions/002-mock-strategy.md](../../specification/decisions/002-mock-strategy.md)  
