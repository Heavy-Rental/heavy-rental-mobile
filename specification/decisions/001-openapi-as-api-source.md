# ADR 001: OpenAPI as the API source of truth

## Status

Accepted (v1)

## Context

The Android app talks to a backend (or mock) over HTTP using Retrofit. Endpoint shapes previously existed only in Kotlin (`HeavyRentalApiService`, DTOs). That made it hard to:

- Share the contract with mock tools (Mockoon, Prism)
- Keep backend and mobile aligned
- Practice Specification Driven Development

## Decision

1. Store the HTTP contract in `specification/api/heavyrental-openapi.yaml`.
2. Treat that file as the **source of truth** for paths, methods, and JSON schemas.
3. Keep Retrofit interfaces and DTOs as the **implementation** of the contract; when they diverge, update OpenAPI and code together.
4. Document v1 client usage nuances in the OpenAPI description and `specification/api/README.md` (e.g. list endpoints optional for the client).

## Consequences

**Positive**

- Mocks can be generated or configured from one file
- New contributors can read the API without reading Kotlin
- Future codegen (OpenAPI Generator → Retrofit) is possible

**Negative / trade-offs**

- Two artefacts to update (YAML + Kotlin) until codegen is adopted
- Risk of drift if PRs change only one side — mitigate with review checklist in `specification/README.md`

## Alternatives considered

| Option | Why not (for now) |
|--------|-------------------|
| Code-only (Retrofit as truth) | Weak for mocks and cross-team SDD |
| Generate OpenAPI from code | Possible later; reverse-spec is enough for v1 |
| Separate contracts git repo | Overhead too high for single mobile project |
