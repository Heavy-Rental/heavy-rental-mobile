# Heavy Rental — Specification Driven Development

This folder is the **source of truth** for product behaviour, domain rules, and the HTTP API contract.

Implementation code under `app/` must follow these specs. When behaviour changes, update the relevant spec **before or with** the code change.

**Start here for the project itself:** [`00-project-overview.md`](00-project-overview.md) — purpose, actors, scope, stack, architecture.

---

## Spec layers

| Layer | Path | Purpose |
|-------|------|---------|
| **Project** | [`00-project-overview.md`](00-project-overview.md) | What the app is: goals, scope, actors, tech stack, architecture |
| **Product** | [`product/`](product/) | Feature acceptance criteria (what the operator can do) |
| **Domain** | [`domain/`](domain/) | Business rules (status machine, list filters) |
| **API** | [`api/`](api/) | OpenAPI contract + example payloads |
| **Decisions** | [`decisions/`](decisions/) | Architecture Decision Records (ADRs) |
| **Environment** | [`project-environment.md`](project-environment.md) | Spec inputs vs generated Mockoon/Prism env under `mocks/` |
| **Testing** | [`testing-guide.md`](testing-guide.md) | Manual QA: Mockoon + Postman + Android emulator |

---

## Sources of truth

1. **Project context:** [`00-project-overview.md`](00-project-overview.md)
2. **API:** [`api/heavyrental-openapi.yaml`](api/heavyrental-openapi.yaml)
3. **Domain:** [`domain/booking-status-machine.md`](domain/booking-status-machine.md) and [`domain/list-filters.md`](domain/list-filters.md)
4. **Product:** files under [`product/`](product/)

When layers conflict, resolve in this order: **product intent → domain rules → API contract → implementation**. Then align all four.

---

## Current app map

| Screen | Navigation | Spec | Primary code |
|--------|------------|------|--------------|
| Login | `AppScreen.LOGIN` | [product/01-login.md](product/01-login.md) | `ui/screens/LoginScreen.kt` |
| Home | `AppScreen.HOME` | [product/02-home-dashboard.md](product/02-home-dashboard.md) | `ui/screens/HomeScreen.kt` |
| Deliveries | `AppScreen.DELIVERIES` | [product/03-deliveries.md](product/03-deliveries.md) | `ui/screens/DeliveryListScreen.kt` |
| Returns | `AppScreen.RETURNS` | [product/04-returns.md](product/04-returns.md) | `ui/screens/ReturnListScreen.kt` |
| Offline / API failure | banner in shell | [product/05-offline-fallback.md](product/05-offline-fallback.md) | `MainActivity` / `AppViewModel` |

**Package root:** `com.heavyrental`

**Runtime (dev):**

- Default HTTP mock: Mockoon or Prism on host port `8081` (OpenAPI `servers`)
- Android emulator base URL: `http://10.0.2.2:8081/` — `RetrofitInstance.BASE_URL`
- Auth: interim → access JWT via `/api/auth/*` ([product/01-login.md](product/01-login.md))
- In-app booking seed: `MockDataRepository.bookingList` (used until API succeeds, and as fallback on list/status failure)
- List load on shell start: `AppViewModel.loadData()` → `GET /api/deliveries`, `GET /api/returns` (and bookings) via `LaunchedEffect` in `HeavyRentalApp`

---

## Development workflow

For every feature or behaviour change:

1. Update the **product** scenario(s)
2. Update **domain** rules if business logic changes
3. Update **OpenAPI** + examples if the network contract changes
4. Refresh mocks (Mockoon / Prism / fixtures) from the API examples
5. Implement app code
6. Add or adjust tests (domain unit tests, MockWebServer contract tests)

### Checklist (PR)

- [ ] Product scenario updated (if user-visible)
- [ ] Domain rule updated (if status/filter logic)
- [ ] OpenAPI + examples updated (if API)
- [ ] Mock still matches contract (`npm run mock:prepare` / `mock:verify`)
- [ ] Tests pass
- [ ] App verified against mock or real API

---

## Mock strategy

| Goal | Tool | Driven by |
|------|------|-----------|
| Run full app against fake HTTP | Mockoon or Prism on port `8081` | OpenAPI + `api/examples/` (includes canned auth) |
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

See [decisions/002-mock-strategy.md](decisions/002-mock-strategy.md).

---

## Known gaps (v1)

Documented honestly so specs do not over-claim:

| Topic | Current behaviour | Spec stance |
|-------|-------------------|-------------|
| Auth | Interim → access JWT via OpenAPI Auth routes; Mockoon returns canned tokens (no password/JWT verification); tokens in memory only | API auth is in scope; real credential checks need Spring (or contract tests); secure storage is future |
| List data | ViewModel loads `GET /api/deliveries` and `GET /api/returns`; seed uses client domain filters | Server/mock owns today membership for list GETs; client seed-only derive when offline |
| Status updates | Optimistic local update even if PATCH fails | Required product behaviour (see offline fallback) |
| Persistence | No Room / offline queue | In-memory state only for v1 |

---

## File index

```text
specification/
  README.md
  00-project-overview.md
  project-environment.md
  testing-guide.md
  product/
    01-login.md
    02-home-dashboard.md
    03-deliveries.md
    04-returns.md
    05-offline-fallback.md
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
      interim-token.txt
      login-response.json
      logout-response.json
  decisions/
    001-openapi-as-api-source.md
    002-mock-strategy.md
```
