# Project overview — Heavy Rental mobile

**Status:** Implemented (v1)  
**Application id:** `com.heavyrental`  
**Package root:** `com.heavyrental`  
**Version (app):** `1.0` (`versionCode` 1)

This document describes **the project itself**: purpose, users, scope, architecture, and how other specs fit together. Feature-level behaviour lives under [`product/`](product/); domain rules under [`domain/`](domain/); HTTP contract under [`api/`](api/).

---

## What it is

**Heavy Rental** is an Android operations app for a heavy-equipment hire business. Operators use it to manage **today’s** equipment **deliveries** (mobilisation) and **returns** (completion of hire), with a simple dashboard after login.

The app is built with **Specification Driven Development (SDD)** aligned with **GitHub Spec Kit** (product `specs/`) and **OpenSPDD** (REASONS under `spdd/`). Domain rules and OpenAPI in this folder remain durable contracts. Implementation under `app/` follows those artifacts.

---

## Problem / goals (v1)

| Goal | Description |
|------|-------------|
| **Today’s work at a glance** | Show how many deliveries and returns are due today, by status |
| **Mobilise deliveries** | Mark confirmed bookings as mobilised when equipment is sent out |
| **Complete returns** | Mark mobilised bookings as completed when equipment is returned |
| **Field-friendly** | Open project locations in maps; remain usable if the API is down |
| **Demo-ready without backend** | In-app seed data + OpenAPI-driven Mockoon/Prism on port `8081` (app default base URL) |

---

## Actors

| Actor | Role |
|-------|------|
| **Admin / operator** | Field or office staff who log in, review today’s lists, mobilise deliveries, and complete returns |

v1 has a **single** operator role (no roles API). Authentication is via the HTTP interim → access JWT flow. See [product/01-login.md](product/01-login.md).

---

## In scope (v1)

| Area | Capability |
|------|------------|
| Auth | Interim → access JWT login / logout over HTTP (`/api/auth/*`); in-memory session |
| Home | Today’s delivery and return counts by status |
| Deliveries | Today’s list, filter by status, maps, mobilise (`CONFIRMED` → `MOBILISED`) |
| Returns | Today’s list, filter by status, maps, complete (`MOBILISED` → `COMPLETED`) |
| Data load | `GET /api/deliveries` + `GET /api/returns` for lists; optional `GET /api/bookings` |
| Status sync | `PATCH` delivery/return status endpoints |
| Offline / failure | Seed data + optimistic local status; error banner on list/status API failure (after login) |
| Mocks | OpenAPI-driven Mockoon / Prism on `:8081`; in-app `MockDataRepository` for booking seed |

### Screens

| Screen | Navigation | Spec |
|--------|------------|------|
| Login | `AppScreen.LOGIN` | [product/01-login.md](product/01-login.md) |
| Home | `AppScreen.HOME` | [product/02-home-dashboard.md](product/02-home-dashboard.md) |
| Deliveries | `AppScreen.DELIVERIES` | [product/03-deliveries.md](product/03-deliveries.md) |
| Returns | `AppScreen.RETURNS` | [product/04-returns.md](product/04-returns.md) |
| Offline / API failure | Shell banner | [product/05-offline-fallback.md](product/05-offline-fallback.md) |

### Core domain (summary)

```text
CONFIRMED  →  MOBILISED  →  COMPLETED
   (Deliveries: mobilise)     (Returns: complete)
```

- **Deliveries today:** `startDate == today` and status ∈ `{ CONFIRMED, MOBILISED }`
- **Returns today:** `endDate == today` and status ∈ `{ MOBILISED, COMPLETED }`

Details: [domain/booking-status-machine.md](domain/booking-status-machine.md), [domain/list-filters.md](domain/list-filters.md).

---

## Out of scope (v1)

Product-level exclusions (see also per-feature “Out of scope” sections):

- Roles, MFA, password reset, biometric login, secure token storage
- Offline / client-only login without a reachable auth API
- Historical (non-today) dashboards and analytics
- Rescheduling dates, partial quantity, damage inspection, late fees
- Photo / signature capture; truck/driver assignment
- Persistent offline queue, Room database, conflict resolution with server
- Multi-yard / multi-company switching
- Push notifications

---

## Tech stack

| Concern | Choice |
|---------|--------|
| Platform | Android (`minSdk` 26, `targetSdk` / `compileSdk` 35) |
| Language | Kotlin (JVM 17) |
| UI | Jetpack Compose, Material 3 |
| State | `AppViewModel` + Kotlin `StateFlow` |
| Navigation | Simple shell (`AppScreen` enum) + bottom bar after login |
| Networking | Retrofit + OkHttp + kotlinx.serialization |
| API contract | OpenAPI 3 (`specification/api/heavyrental-openapi.yaml`) |
| Local mocks | `MockDataRepository` (booking seed); Mockoon / Prism on port `8081` (default HTTP) |

### High-level architecture

```text
┌─────────────────────────────────────────────────────────┐
│  UI (Compose screens + HeavyRentalApp shell)            │
│  Login · Home · Deliveries · Returns · error banner     │
└───────────────────────────┬─────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────┐
│  AppViewModel                                           │
│  auth · loadData · status transitions · networkError    │
└───────────────────────────┬─────────────────────────────┘
                            │
     ┌──────────────────────┼──────────────────────┐
     ▼                      ▼                      ▼
┌──────────────┐  ┌──────────────────┐  ┌──────────────────────┐
│ AuthRepository│  │ MockDataRepository│  │ BookingRepository    │
│ TokenSession  │  │ (seed / fallback) │  │ → Retrofit API       │
└──────┬───────┘  └──────────────────┘  └──────────┬───────────┘
       │                                           │
       └───────────────────┬───────────────────────┘
                           ▼
              HTTP :8081 Mockoon (default) or :8080 Spring Boot
              OpenAPI-defined paths
              ApiServerTarget / BaseUrlInterceptor
                http://10.0.2.2:8081/ or :8080/ (emulator)
```

**Auth (v1):** interim JWT → access JWT handshake; access Bearer attached to business calls. See [product/01-login.md](product/01-login.md).

**List data (v1):** Delivery and Return screens load **`GET /api/deliveries`** and **`GET /api/returns`**. The client enforces allowed status transitions on those lists. Seed/offline still derives lists from `MockDataRepository` via domain filters (`toDeliveryItems` / `toReturnItems`).

---

## Repo layout (relevant)

| Path | Role |
|------|------|
| `app/` | Android application |
| `specification/` | SDD source of truth (this folder) |
| `mocks/` | Generated Mockoon env + Prism bundle (from API specs) |
| `scripts/` | `prepare-mocks`, start Prism/Mockoon, verify |
| `package.json` | Node tooling for mocks only |

Spec folder map and process: [README.md](README.md).  
Spec vs generated env: [project-environment.md](project-environment.md).

---

## Credentials (dev / v1 demo)

| Field | Value |
|-------|--------|
| Email | `admin@localhost` |
| Password | `admin1234` |
| Display name | `Admin` (from `LoginResponse.username` local-part) |

Aligned with OpenAPI `LoginRequest` examples and the Login screen seed hint. Full behaviour (Mockoon canned vs real Spring validation): [product/01-login.md](product/01-login.md).

---

## Development runtime (API)

**Default:** OpenAPI-driven Mockoon or Prism on host port **8081**. The app emulator base URL matches OpenAPI `servers`:

| Client | Base URL |
|--------|----------|
| Android emulator → host mock | `http://10.0.2.2:8081/` |
| Host machine / curl | `http://localhost:8081/` |
| Physical device | `http://<host-lan-ip>:8081/` |

Default target is Mockoon/Prism (`api.server.target=MOCKOON` in `app/api.properties`). Set `SPRING_BOOT` for host `localhost:8080` (emulator `http://10.0.2.2:8080/`). Applied via `BuildConfig` + `BaseUrlInterceptor` — no in-app UI toggle.

Mock servers and regeneration: [api/README.md](api/README.md), [project-environment.md](project-environment.md), [`mocks/README.md`](../mocks/README.md).

---

## Spec map (how to navigate)

| If you need… | Read |
|--------------|------|
| Project purpose and stack (this file) | [00-project-overview.md](00-project-overview.md) |
| Feature specs (Spec Kit, canonical) | [`specs/`](../specs/) |
| Constitution | [`.specify/memory/constitution.md`](../.specify/memory/constitution.md) |
| Design contracts (OpenSPDD REASONS) | [`spdd/`](../spdd/) |
| Product index stubs | [product/](product/) |
| Status machine / list filters | [domain/](domain/) |
| HTTP paths and schemas | [api/heavyrental-openapi.yaml](api/heavyrental-openapi.yaml) |
| Why OpenAPI / mock layers | [decisions/](decisions/) |
| What is generated for mocks | [project-environment.md](project-environment.md) |
| SDD workflow and PR checklist | [README.md](README.md) |
| Mockoon + Postman + Android testing | [testing-guide.md](testing-guide.md) |

**Conflict resolution order:** product intent (`specs/`) → domain rules → API contract → implementation (then align all four).

---

## Known gaps (v1)

| Topic | Current behaviour |
|-------|-------------------|
| Auth | API handshake implemented; Mockoon returns canned tokens (no credential check); no secure token storage |
| List data | Loaded from `GET /api/deliveries` / `GET /api/returns`; seed derive only offline |
| Status updates | Optimistic local update if PATCH fails |
| Persistence | In-memory only (no Room / offline queue) |

---

## Related code entry points

| Concern | Location |
|---------|----------|
| App shell | `MainActivity.kt` — `HeavyRentalApp` |
| State / API orchestration | `viewmodel/AppViewModel.kt` |
| Screens | `ui/screens/*` |
| Domain models / filters | `data/models/*` |
| Booking seed / offline fallback | `data/repository/MockDataRepository.kt` |
| Auth handshake | `data/repository/AuthRepository.kt`, `network/TokenSession.kt`, `network/AuthInterceptor.kt` |
| Network | `network/dto/RetrofitInstance.kt`, `HeavyRentalApiService`, `BookingRepository` |
