# Heavy Rental Mobile — Specification Driven Development

This folder is **detailed product SDD** (screens, domain rules, OpenAPI). Living behavior contracts, change proposals, and architectural why live in:

| Layer | Path | Standard |
| --- | --- | --- |
| What (behavior) | [`openspec/specs/`](../openspec/specs/) | OpenSpec `spec-driven-with-adr` |
| How (implementation contract) | [`spdd/prompt/`](../spdd/prompt/) | OpenSPDD REASONS Canvas |
| Why (architecture) | [`adr/`](../adr/) | MADR-short ADRs |

Implementation code under `app/` must follow these specs. When behaviour changes, update the relevant spec **before or with** the code change.

**Start here for the project itself:** [`00-project-overview.md`](00-project-overview.md) — purpose, actors, scope, stack, architecture.

**First-time machine setup:** [`setup-guide.md`](setup-guide.md) — clone, Android Studio, emulator, Spring or mock, login.

**Conflict rule:** running code / `.github/workflows/` wins, then OpenSpec specs, then this folder. Server-overlapping facts follow [`heavy-rental-spring-rest-api`](https://github.com/Heavy-Rental/heavy-rental-spring-rest-api) `openspec/specs/`.

Canonical GitHub repository: `Heavy-Rental/heavy-rental-mobile`.

---

## Spec layers

| Layer | Path | Purpose | OpenSpec |
|-------|------|---------|----------|
| **Project** | [`00-project-overview.md`](00-project-overview.md) | What the app is: goals, scope, actors, tech stack, architecture | `project-environment`, `product-features` |
| **Product** | [`product/`](product/) | Feature acceptance criteria (what the operator/customer can do) | `product-features` |
| **Domain** | [`domain/`](domain/) | Business rules (status machine, list filters) | `product-features` |
| **API** | [`api/`](api/) | OpenAPI contract + example payloads | `project-environment` (ADR-0003) |
| **Decisions** | [`decisions/`](decisions/) | Stubs → durable [`adr/`](../adr/) | ADR-0003–0006 |
| **Environment** | [`project-environment.md`](project-environment.md) | Spec inputs vs generated Mockoon/Prism env under `mocks/` | `project-environment` |
| **Setup** | [`setup-guide.md`](setup-guide.md) | First-time clone → Studio → emulator → API → login | `project-environment` |
| **Testing** | [`testing-guide.md`](testing-guide.md) | Manual QA: Mockoon + Postman + Android emulator | `project-environment` |

---

## Sources of truth

1. **Project context:** [`00-project-overview.md`](00-project-overview.md)
2. **API (this client):** [`api/heavyrental-openapi.yaml`](api/heavyrental-openapi.yaml)
3. **API (server):** Spring OpenSpec `auth-login-logout`, `booking-delivery-return`
4. **Domain:** [`domain/booking-status-machine.md`](domain/booking-status-machine.md) and [`domain/list-filters.md`](domain/list-filters.md)
5. **Product:** files under [`product/`](product/)

When layers conflict, resolve in this order: **code / YAML → OpenSpec → product intent → domain rules → OpenAPI → implementation notes**. Then align all of them.

---

## Current app map

| Screen | Navigation | Spec | Primary code |
|--------|------------|------|--------------|
| Login | `AppScreen.LOGIN` | [product/01-login.md](product/01-login.md) | `ui/screens/LoginScreen.kt` |
| Home | `AppScreen.HOME` | [product/02-home-dashboard.md](product/02-home-dashboard.md) | `ui/screens/HomeScreen.kt` |
| Deliveries | `AppScreen.DELIVERIES` | [product/03-deliveries.md](product/03-deliveries.md) | `ui/screens/DeliveryListScreen.kt` |
| Returns | `AppScreen.RETURNS` | [product/04-returns.md](product/04-returns.md) | `ui/screens/ReturnListScreen.kt` |
| Offline / API failure | banner in shell | [product/05-offline-fallback.md](product/05-offline-fallback.md) | `MainActivity` / `AppViewModel` |
| Customer Bookings | `AppScreen.CUSTOMER_BOOKINGS` | [product/06-customer-bookings.md](product/06-customer-bookings.md) | `ui/screens/CustomerBookingsScreen.kt` |

**Package root:** `com.heavyrental`

**Runtime (dev):**

- Default app target (ADR-0007): Spring Boot backend on host port `8080` — emulator `http://10.0.2.2:8080/`
- Mock alternative: Mockoon or Prism on host port `8081` — emulator `http://10.0.2.2:8081/`
- Selected by `USE_MOCK_SERVER` in `network/dto/RetrofitInstance.kt` (`false` = Spring Boot)
- Auth: interim → access JWT via `/api/auth/*`; Google via `/api/auth/google` (not on mock)
- In-app booking seed: `MockDataRepository.bookingList` (until API succeeds, and as fallback on list/status failure)
- List load on shell start: `AppViewModel.loadData()` → `GET /api/deliveries`, `GET /api/returns` (and bookings)

---

## Development workflow

For every feature or behaviour change:

1. OpenSpec change folder (`proposal → specs → design → adr → tasks`) when behavior or architecture changes
2. Update the **product** scenario(s)
3. Update **domain** rules if business logic changes
4. Update **OpenAPI** + examples if the network contract changes
5. Refresh mocks (Mockoon / Prism / fixtures) from the API examples
6. Implement app code
7. Add or adjust tests (domain unit tests, MockWebServer contract tests)
8. Update the OpenSPDD canvas if the implementation contract changed

### Checklist (PR)

- [ ] OpenSpec delta (if observable behavior/architecture)
- [ ] Product scenario updated (if user-visible)
- [ ] Domain rule updated (if status/filter logic)
- [ ] OpenAPI + examples updated (if API)
- [ ] Mock still matches contract (`npm run mock:prepare` / `mock:verify`)
- [ ] In-force ADRs not contradicted
- [ ] Tests pass
- [ ] App verified against mock or real API

---

## Mock strategy

| Goal | Tool | Driven by |
|------|------|-----------|
| Run full app against fake HTTP | Mockoon or Prism on port `8081` | OpenAPI + `api/examples/` (includes canned auth; **no Google**) |
| Automated client tests | OkHttp MockWebServer | Same JSON examples / schemas |
| Booking lists without server | `MockDataRepository` | Domain examples (seed/fallback only — **not** auth) |

### Quick start (OpenAPI → mock server)

```bash
npm install
npm run mock:prism      # or: npm run mock:mockoon
npm run mock:verify     # optional smoke test
```

Full instructions: [`mocks/README.md`](../mocks/README.md).

**Manual QA (Mockoon + Postman + Android):** [`testing-guide.md`](testing-guide.md).

See [ADR-0004](../adr/0004-three-layer-mock-strategy.md).

---

## Known gaps (v1)

Documented honestly so specs do not over-claim:

| Topic | Current behaviour | Spec stance |
|-------|-------------------|-------------|
| Auth | Interim → access JWT via OpenAPI Auth routes; Mockoon returns canned tokens (no password/JWT verification); tokens in memory only; Google requires Spring | API auth is in scope; real credential checks need Spring; secure storage is future |
| List data | ViewModel loads `GET /api/deliveries` and `GET /api/returns`; seed uses client domain filters | Server/mock owns today membership for list GETs; client seed-only derive when offline |
| Status updates | Optimistic local update only on `IOException`; `HttpException` leaves status unchanged (HR-93) | Required product behaviour (see offline fallback) |
| Persistence | No Room / offline queue | In-memory state only for v1 |
| Env toggle | `USE_MOCK_SERVER` is a committed constant | Known gap; do not flip to `true` on `develop` |

---

## File index

```text
specification/
  README.md
  00-project-overview.md
  project-environment.md
  setup-guide.md
  testing-guide.md
  product/
    01-login.md
    02-home-dashboard.md
    03-deliveries.md
    04-returns.md
    05-offline-fallback.md
    06-customer-bookings.md
  domain/
    booking-status-machine.md
    list-filters.md
  api/
    heavyrental-openapi.yaml
    README.md
    examples/
      bookings.json
      deliveries.json
      returns.json
      status-update-request.json
      return-status-update-request.json
      interim-token.txt
      login-response.json
      logout-response.json
      google-login-request.json
  decisions/          # stubs → ../adr/
    001-openapi-as-api-source.md
    002-mock-strategy.md
    003-mock-echoes-return-notes.md
    004-google-sign-in.md
```
