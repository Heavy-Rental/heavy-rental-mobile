# Project overview — Heavy Rental mobile

**Status:** Implemented (v1)  
**Application id:** `com.heavyrental`  
**Package root:** `com.heavyrental`  
**Version (app):** `1.0` (`versionCode` 1)

This document describes **the project itself**: purpose, users, scope, architecture, and how other specs fit together. Feature-level behaviour lives under [`product/`](product/); domain rules under [`domain/`](domain/); HTTP contract under [`api/`](api/).

---

## What it is

**Heavy Rental** is an Android operations app for a heavy-equipment hire business. Operators use it to manage **today’s** equipment **deliveries** (mobilisation) and **returns** (completion of hire), with a simple dashboard after login.

The app is built with **Specification Driven Development (SDD)**: product scenarios, domain rules, and an OpenAPI contract in this folder are the source of truth for implementation under `app/`.

---

## Problem / goals (v1)

| Goal | Description |
|------|-------------|
| **Today’s work at a glance** | Show how many deliveries and returns are due today, by status |
| **Mobilise deliveries** | Mark confirmed bookings as mobilised when equipment is sent out |
| **Complete returns** | Mark mobilised bookings as completed when equipment is returned |
| **Field-friendly** | Open project locations in maps; remain usable if the API is down |
| **Demo-ready without backend** | In-app seed data + optional Mockoon/Prism on port `8081` |

---

## Actors

| Actor | Role |
|-------|------|
| **Admin / operator** | Field or office staff who log in, review today’s lists, mobilise deliveries, and complete returns |

v1 has a **single** hardcoded admin (no roles API). See [product/01-login.md](product/01-login.md).

---

## In scope (v1)

| Area | Capability |
|------|------------|
| Auth | Client-only login / logout (hardcoded admin credentials) |
| Home | Today’s delivery and return counts by status |
| Deliveries | Today’s list, filter by status, maps, mobilise (`CONFIRMED` → `MOBILISED`) |
| Returns | Today’s list, filter by status, maps, complete (`MOBILISED` → `COMPLETED`) |
| Data load | `GET /api/bookings`; client derives delivery/return lists |
| Status sync | `PATCH` delivery/return status endpoints |
| Offline / failure | Seed data + optimistic local status; error banner on API failure |
| Mocks | OpenAPI-driven Mockoon / Prism; in-app `MockDataRepository` |

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

- Remote authentication, roles, MFA, password reset, session tokens
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
| Local mocks | `MockDataRepository`; optional Mockoon / Prism on port `8081` |

### High-level architecture

```text
┌─────────────────────────────────────────────────────────┐
│  UI (Compose screens + HeavyRentalApp shell)            │
│  Login · Home · Deliveries · Returns · error banner     │
└───────────────────────────┬─────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────┐
│  AppViewModel                                           │
│  auth · loadBookings · status transitions · networkError│
└───────────────────────────┬─────────────────────────────┘
                            │
          ┌─────────────────┼─────────────────┐
          ▼                                   ▼
┌──────────────────┐               ┌──────────────────────┐
│ MockDataRepository│               │ BookingRepository    │
│ (seed / fallback) │               │ → Retrofit API       │
└──────────────────┘               └──────────┬───────────┘
                                              │
                                   HTTP :8081 (dev mock / backend)
                                   OpenAPI-defined paths
```

**Domain ownership on the client (v1):** after `GET /api/bookings`, the app derives delivery/return lists and enforces status transitions. Dedicated `GET /api/deliveries` and `GET /api/returns` exist in the contract for mocks/backends but are not required for list rendering.

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
| Email | `admin@heavyrental.com` |
| Password | `admin123` |
| Display name | `Admin` |

Source: `MockDataRepository` — must stay aligned with [product/01-login.md](product/01-login.md).

---

## Development runtime (API)

| Client | Base URL |
|--------|----------|
| Android emulator → host | `http://10.0.2.2:8081/` |
| Host machine | `http://localhost:8081/` |
| Physical device | `http://<host-lan-ip>:8081/` |

Configured in `RetrofitInstance.BASE_URL`. Mock servers and regeneration: [api/README.md](api/README.md), [project-environment.md](project-environment.md), [`mocks/README.md`](../mocks/README.md).

---

## Spec map (how to navigate)

| If you need… | Read |
|--------------|------|
| Project purpose and stack (this file) | [00-project-overview.md](00-project-overview.md) |
| Feature acceptance criteria | [product/](product/) |
| Status machine / list filters | [domain/](domain/) |
| HTTP paths and schemas | [api/heavyrental-openapi.yaml](api/heavyrental-openapi.yaml) |
| Why OpenAPI / mock layers | [decisions/](decisions/) |
| What is generated for mocks | [project-environment.md](project-environment.md) |
| SDD workflow and PR checklist | [README.md](README.md) |

**Conflict resolution order:** product intent → domain rules → API contract → implementation (then align all four).

---

## Known gaps (v1)

| Topic | Current behaviour |
|-------|-------------------|
| Auth | Client-only hardcoded admin |
| List data | Derived on client from bookings |
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
| Seed data | `data/repository/MockDataRepository.kt` |
| Network | `network/RetrofitInstance.kt`, `HeavyRentalApiService`, `BookingRepository` |
