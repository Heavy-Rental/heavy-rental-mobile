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

- Android emulator base URL: `http://10.0.2.2:8081/` (host machine port `8081`) — `RetrofitInstance.BASE_URL`
- In-app seed data: `MockDataRepository.bookingList` (used until API succeeds, and as fallback on failure)
- Bookings load on shell start: `AppViewModel.loadBookings()` via `LaunchedEffect` in `HeavyRentalApp`

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
| Run full app against fake HTTP | Mockoon or Prism on port `8081` | OpenAPI + `api/examples/` |
| Automated client tests | OkHttp MockWebServer | Same JSON examples / schemas |
| UI without server | `MockDataRepository` | Domain examples (keep aligned with fixtures) |

### Quick start (OpenAPI → mock server)

```bash
npm install
npm run mock:prism      # or: npm run mock:mockoon
npm run mock:verify     # optional smoke test
```

Full instructions: [`mocks/README.md`](../mocks/README.md).

See [decisions/002-mock-strategy.md](decisions/002-mock-strategy.md).

---

## Known gaps (v1)

Documented honestly so specs do not over-claim:

| Topic | Current behaviour | Spec stance |
|-------|-------------------|-------------|
| Auth | Client-only hardcoded admin | v1 local auth; API login is future |
| List data | ViewModel loads `GET /api/bookings`, then derives deliveries/returns locally | Domain filters are client-side; dedicated list endpoints exist in API for future/backend use |
| Status updates | Optimistic local update even if PATCH fails | Required product behaviour (see offline fallback) |
| Persistence | No Room / offline queue | In-memory state only for v1 |

---

## File index

```text
specification/
  README.md
  00-project-overview.md
  project-environment.md
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
  decisions/
    001-openapi-as-api-source.md
    002-mock-strategy.md
```
