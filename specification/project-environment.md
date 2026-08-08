# Project environment (specification vs generated mocks)

This document explains what lives **inside** `specification/` and what is **generated outside** it for local development (Mockoon / Prism).

---

## Short answer

A project **environment** file (Mockoon) is **not** stored under `specification/`.

- **`specification/`** holds the **source of truth** (product, domain, OpenAPI, examples, decisions).
- The **Mockoon project environment** is an **output** of those sources, written under `mocks/`.

---

## What is in `specification/` (source of truth)

These are hand-authored (or curated) specs. They are **not** the Mockoon environment file.

| Layer | Path | Contents |
|-------|------|----------|
| **Index** | [`README.md`](README.md) | Spec layers, workflow, PR checklist |
| **Product** | [`product/`](product/) | Feature acceptance criteria (`01-login` … `05-offline-fallback`) |
| **Domain** | [`domain/`](domain/) | Booking status machine, list filters |
| **API** | [`api/heavyrental-openapi.yaml`](api/heavyrental-openapi.yaml) | OpenAPI 3.0 contract |
| **API examples** | [`api/examples/`](api/examples/) | JSON fixtures for bookings, deliveries, returns, status PATCH body |
| **API docs** | [`api/README.md`](api/README.md) | Base URLs, endpoints, mock commands |
| **Decisions** | [`decisions/`](decisions/) | ADRs (OpenAPI as truth, mock strategy) |

There is **no** `*.environment.json` (or equivalent Mockoon project file) under `specification/`.

---

## What is generated outside `specification/`

From the OpenAPI contract and example JSON, via:

```bash
npm run mock:prepare
```

| Generated artefact | Path | Purpose |
|--------------------|------|---------|
| **Mockoon project environment** | `mocks/mockoon/heavy-rental.environment.json` | Full Mockoon env (port, routes, FILE bodies) |
| Bundled OpenAPI (Prism) | `mocks/.generated/openapi.bundled.yaml` | OpenAPI with inline examples for Prism |
| Single-item fixtures | `mocks/.generated/*-item.json` | Bodies for PATCH / GET-by-id style responses |

Also documented in [`mocks/README.md`](../mocks/README.md).

### Mockoon environment (summary)

When present, `mocks/mockoon/heavy-rental.environment.json` is generated with properties such as:

| Property | Typical value |
|----------|----------------|
| Name | Heavy Rental API |
| Port | `8081` |
| Hostname | `0.0.0.0` |
| List route bodies | `FILE` → relative paths into `specification/api/examples/` |

Example (bookings list route):

- Label: `200 — bookings from specification/api/examples`
- File path: `../../specification/api/examples/bookings.json`

Regenerate after changing OpenAPI or examples:

```bash
npm install
npm run mock:prepare
# then either:
npm run mock:prism
# or:
npm run mock:mockoon
```

---

## Design: inputs → outputs

```text
specification/api/heavyrental-openapi.yaml
specification/api/examples/*.json
            │
            ▼  scripts/prepare-mocks.mjs
            │
            ├── mocks/mockoon/heavy-rental.environment.json
            └── mocks/.generated/
                  ├── openapi.bundled.yaml
                  └── *-item.json
```

| Role | Location |
|------|----------|
| **Inputs (edit these)** | `specification/api/` |
| **Outputs (do not hand-edit)** | `mocks/mockoon/`, `mocks/.generated/` |

By design, the environment is derived from the specification so:

1. OpenAPI + examples remain the single HTTP contract source of truth.
2. Mock servers stay aligned when the contract changes.
3. Generated Mockoon files are not treated as specs.

---

## Android client (dev) alignment

The app’s **default** API target is the Android emulator alias for host Mockoon/Prism, matching OpenAPI `servers` entry `http://10.0.2.2:8081`:

| Client | Mockoon / Prism | Spring Boot |
|--------|-----------------|-------------|
| Emulator → host | `http://10.0.2.2:8081/` (default) | `http://10.0.2.2:8080/` |
| Host machine / curl | `http://localhost:8081/` | `http://localhost:8080/` |
| Physical device | `http://<host-lan-ip>:8081/` | `http://<host-lan-ip>:8080/` |

App config: `app/api.properties` key `api.server.target` (`MOCKOON` | `SPRING_BOOT`), optional override in root `local.properties`, injected as `BuildConfig.API_SERVER_TARGET` and applied by `BaseUrlInterceptor`. No in-app UI toggle — edit properties, then Sync/Rebuild.

Auth and booking routes are defined in OpenAPI and served by the generated Mockoon env. Product behaviour: [product/01-login.md](product/01-login.md). Mock auth is canned (no real credential verification) — see [api/README.md](api/README.md) and [decisions/002-mock-strategy.md](decisions/002-mock-strategy.md).

List routes (`GET /api/deliveries`, `GET /api/returns`) use **FILE** bodies pointing at `specification/api/examples/deliveries.json` and `returns.json`. The v1 app loads those endpoints for the Delivery and Return screens.

---

## Related specs

- [00-project-overview.md](00-project-overview.md) — project purpose, scope, stack
- [specification/README.md](README.md) — SDD overview
- [specification/api/README.md](api/README.md) — API + mock commands
- [specification/decisions/002-mock-strategy.md](decisions/002-mock-strategy.md) — why three mock layers
- [mocks/README.md](../mocks/README.md) — how to run and verify mocks
