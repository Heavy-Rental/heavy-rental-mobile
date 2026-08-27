# Project overview — Heavy Rental mobile

**Status:** Implemented (v1)  
**Application id:** `com.heavyrental`  
**Package root:** `com.heavyrental`  
**Version (app):** `1.0` (`versionCode` 1)

This document describes **the project itself**: purpose, users, scope, architecture, and how other specs fit together. Feature-level behaviour lives under [`product/`](product/); domain rules under [`domain/`](domain/); HTTP contract under [`api/`](api/).

---

## What it is

**Heavy Rental** is an Android operations app for a heavy-equipment hire business. Operators use it to manage **today’s** equipment **deliveries** (mobilisation) and **returns** (completion of hire), with a simple dashboard after login.

The app is built with **Specification Driven Development**: product scenarios and OpenAPI in this folder are the detailed SDD. Living behavior is OpenSpec (`openspec/specs/`); architecture why is `adr/`; implementation contracts are OpenSPDD (`spdd/prompt/`). Implementation under `app/` MUST follow those layers (ADR-0001).

---

## Problem / goals (v1)

| Goal | Description |
|------|-------------|
| **Today’s work at a glance** | Show how many deliveries and returns are due today, by status |
| **Mobilise deliveries** | Mark confirmed bookings as mobilised when equipment is sent out |
| **Complete returns** | Mark mobilised bookings as completed when equipment is returned |
| **Field-friendly** | Open project locations in maps; remain usable if the API is down |
| **Demo-ready without backend** | In-app seed data + OpenAPI-driven Mockoon/Prism on port `8081` (opt-in; app default is Spring `:8080`) |

---

## Actors

| Actor | Role |
|-------|------|
| **Admin / operator** | Field or office staff who log in, review today’s lists, mobilise deliveries, and complete returns |
| **Customer** | The customer on a booking, who logs in with the same screen (**password only**) to view — never edit — their own bookings and each one's status |

Authentication is via the HTTP interim → access JWT flow, shared by both actors (staff may also use Google Sign-In). Which screen a
session lands on is decided client-side from the access token's `roles` claim — see
[product/01-login.md](product/01-login.md) "Role routing" and
[product/06-customer-bookings.md](product/06-customer-bookings.md). Beyond that one staff/customer
routing split, there is still no broader in-app roles/permissions model. Server authorization: `ROLE_ADMIN` and `ROLE_DRIVER` may call bookings/deliveries/returns (Spring `booking-delivery-return`).

---

## In scope (v1)

| Area | Capability |
|------|------------|
| Auth | Interim → access JWT login / logout over HTTP (`/api/auth/*`); Google Sign-In (`POST /api/auth/google`, Spring only); in-memory session |
| Home | Today’s delivery and return counts by status |
| Deliveries | Today’s list, filter by status, maps, mobilise (`CONFIRMED` → `MOBILISED`) |
| Returns | Today’s list, filter by status, maps, complete (`MOBILISED` → `COMPLETED`) |
| Data load | `GET /api/deliveries` + `GET /api/returns` for lists; optional `GET /api/bookings` |
| Status sync | `PATCH` delivery/return status endpoints |
| Offline / failure | Seed data + optimistic local status; error banner on list/status API failure (after login) |
| Mocks | OpenAPI-driven Mockoon / Prism on `:8081`; in-app `MockDataRepository` for booking seed |
| Customer bookings | Read-only `GET /api/bookings` list for `ROLE_USER` sessions, filterable by status; no edit/status-update affordance |

### Screens

| Screen | Navigation | Spec |
|--------|------------|------|
| Login | `AppScreen.LOGIN` | [product/01-login.md](product/01-login.md) |
| Home | `AppScreen.HOME` | [product/02-home-dashboard.md](product/02-home-dashboard.md) |
| Deliveries | `AppScreen.DELIVERIES` | [product/03-deliveries.md](product/03-deliveries.md) |
| Returns | `AppScreen.RETURNS` | [product/04-returns.md](product/04-returns.md) |
| Offline / API failure | Shell banner | [product/05-offline-fallback.md](product/05-offline-fallback.md) |
| Customer Bookings | `AppScreen.CUSTOMER_BOOKINGS` | [product/06-customer-bookings.md](product/06-customer-bookings.md) |

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

- Any client-side roles/permissions model beyond the staff/customer login routing split (see Actors above); MFA, password reset, biometric login, secure token storage; customer entry via Google Sign-In
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
| Local mocks | `MockDataRepository` (booking seed); Mockoon / Prism on port `8081` (opt-in HTTP) |

### High-level architecture

```text
┌─────────────────────────────────────────────────────────┐
│  UI (Compose screens + HeavyRentalApp shell)            │
│  Login · Home · Deliveries · Returns                    │
│  · Customer Bookings · error banner                     │
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
              HTTP :8080 (Spring Boot — default since HR-78)
              HTTP :8081 (Mockoon / Prism — USE_MOCK_SERVER = true)
              OpenAPI-defined paths
              RetrofitInstance (USE_MOCK_SERVER) resolves to
                http://10.0.2.2:8080/ (emulator, default)
```

**Auth (v1):** interim JWT → access JWT handshake; access Bearer attached to business calls. See [product/01-login.md](product/01-login.md).

**List data (v1):** Delivery and Return screens load **`GET /api/deliveries`** and **`GET /api/returns`**. The client enforces allowed status transitions on those lists. Seed/offline still derives lists from `MockDataRepository` via domain filters (`toDeliveryItems` / `toReturnItems`).

---

## Repo layout (relevant)

| Path | Role |
|------|------|
| `app/` | Android application |
| `specification/` | Detailed product SDD (this folder) |
| `openspec/` | Living behavior contracts (OpenSpec) |
| `adr/` | Durable architecture decisions |
| `spdd/` | OpenSPDD REASONS canvases |
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

**Default since HR-78:** the real Spring Boot backend on host port **8080**. OpenAPI-driven Mockoon or Prism on **8081** remains available for offline/contract work.

| Client | Spring Boot (default) | Mockoon / Prism |
|--------|-----------------------|-----------------|
| Android emulator → host | `http://10.0.2.2:8080/` | `http://10.0.2.2:8081/` |
| Host machine / curl | `http://localhost:8080/` | `http://localhost:8081/` |
| Physical device | `http://<host-lan-ip>:8080/` | `http://<host-lan-ip>:8081/` |

Selected by `USE_MOCK_SERVER` in `network/dto/RetrofitInstance.kt` (`false` = Spring Boot). Booking/delivery/return routes exist on Spring `develop` (OpenSpec `booking-delivery-return`) — do not treat them as `HR-80`-only.

Mock servers and regeneration: [api/README.md](api/README.md), [project-environment.md](project-environment.md), [`mocks/README.md`](../mocks/README.md).

---

## Spec map (how to navigate)

| If you need… | Read |
|--------------|------|
| Project purpose and stack (this file) | [00-project-overview.md](00-project-overview.md) |
| Feature acceptance criteria | [product/](product/) |
| Status machine / list filters | [domain/](domain/) |
| HTTP paths and schemas | [api/heavyrental-openapi.yaml](api/heavyrental-openapi.yaml) |
| Why OpenAPI / mock layers / Google / HTTP target | [`adr/`](../adr/) (stubs in [decisions/](decisions/)) |
| What is generated for mocks | [project-environment.md](project-environment.md) |
| SDD workflow and PR checklist | [README.md](README.md) |
| Living behavior (OpenSpec) | [`openspec/specs/`](../openspec/specs/) |
| Implementation contract (OpenSPDD) | [`spdd/prompt/`](../spdd/prompt/) |
| First-time clone and run | [setup-guide.md](setup-guide.md) |
| Mockoon + Postman + Android testing | [testing-guide.md](testing-guide.md)

**Conflict resolution order:** running code / workflow YAML → OpenSpec → product intent → domain rules → API contract (then align all of them). Server facts: Spring OpenSpec.

---

## Known gaps (v1)

| Topic | Current behaviour |
|-------|-------------------|
| Auth | API handshake implemented; Mockoon returns canned tokens (no credential check); Google requires Spring; no secure token storage |
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
| Role routing (staff vs. customer) | `network/JwtClaims.kt`, `viewmodel/AppViewModel.kt` — `onLoginSuccess` |
| Network | `network/dto/RetrofitInstance.kt`, `HeavyRentalApiService`, `BookingRepository` |
