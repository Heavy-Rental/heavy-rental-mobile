# ADR-0003: OpenAPI is the client HTTP contract

- Status: accepted
- Date: 2026-08-27
- Tags: api, openapi, sdd

## Context

The Android app talks to a backend (or mock) over HTTP using Retrofit. Endpoint shapes previously existed only in Kotlin (`HeavyRentalApiService`, DTOs). That made it hard to share the contract with mock tools, keep backend and mobile aligned, and practice spec-driven development.

The Spring REST API owns **server** behavior in its own OpenSpec (`openspec/specs/` in `heavy-rental-spring-rest-api`). This mobile repo still needs a **client-facing** HTTP contract that Mockoon/Prism and Retrofit can share.

Predecessor: informal `specification/decisions/001-openapi-as-api-source.md`.

## Decision

1. Store the HTTP contract in `specification/api/heavyrental-openapi.yaml`.
2. Treat that file as this repository's source of truth for paths, methods, and JSON schemas the **mobile client** uses.
3. Keep Retrofit interfaces and DTOs as the **implementation** of the contract; when they diverge, update OpenAPI and code together.
4. Server-side auth, ownership, and status-machine rules remain owned by `Heavy-Rental/heavy-rental-spring-rest-api` OpenSpec. This OpenAPI MUST not invent server behavior that contradicts that SoT.
5. Document v1 client usage nuances in the OpenAPI description and `specification/api/README.md`.

## Consequences

**Positive**

- Mocks can be generated from one file.
- Contributors can read the API without reading Kotlin.
- Future codegen (OpenAPI Generator → Retrofit) remains possible.

**Negative / trade-offs**

- Two artefacts to update (YAML + Kotlin) until codegen is adopted.
- Risk of drift if PRs change only one side — mitigate with the PR checklist in `specification/README.md`.
- A third artefact exists on the backend; conflict rule is: Spring OpenSpec for server facts, this OpenAPI for what the client sends/expects.

## Alternatives considered

| Option | Why not (for now) |
|--------|-------------------|
| Code-only (Retrofit as truth) | Weak for mocks and cross-team SDD |
| Generate OpenAPI from code | Possible later; reverse-spec is enough for v1 |
| Separate contracts git repo | Overhead too high for this client |
