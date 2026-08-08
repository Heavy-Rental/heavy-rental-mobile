# Heavy Rental — Specification Driven Development

This folder holds the **living baseline** for domain rules, OpenAPI, ADRs, environment, and testing.

**Canonical product feature specs** follow [GitHub Spec Kit](https://github.com/github/spec-kit) and live under [`specs/`](../specs/).  
**Implementation design contracts** follow [OpenSPDD](https://github.com/gszhangwei/open-spdd) (REASONS Canvas) under [`spdd/`](../spdd/).  
**Project constitution:** [`.specify/memory/constitution.md`](../.specify/memory/constitution.md).

Implementation under `app/` must follow these artifacts. When behaviour changes, update the relevant **Spec Kit `spec.md`** (and domain/OpenAPI/REASONS as needed) **before or with** the code change.

**Project overview:** [`00-project-overview.md`](00-project-overview.md).

---

## Standards map

| Standard | Role in this repo | Location |
|----------|-------------------|----------|
| **GitHub Spec Kit** | Product intent: user stories, FR-###, SC-###, plan/tasks | [`specs/`](../specs/) |
| **OpenSPDD** | Executable design contract (REASONS) for non-trivial changes | [`spdd/`](../spdd/) |
| **OpenAPI** | HTTP paths, schemas, examples | [`api/`](api/) |
| **Domain** | Status machine, list membership | [`domain/`](domain/) |
| **ADRs** | Architecture decisions | [`decisions/`](decisions/) |
| **Constitution** | Non-negotiable principles | [`.specify/memory/constitution.md`](../.specify/memory/constitution.md) |

### Conflict resolution

1. Product intent → `specs/###-feature/spec.md`  
2. Domain rules → `specification/domain/`  
3. HTTP contract → OpenAPI  
4. Implementation design → `spdd/prompt/` REASONS  
5. When behaviour and design diverge: update REASONS first for behaviour changes; code-first refactor then sync REASONS  

### Workflow (content-first)

| Step | Spec Kit | OpenSPDD |
|------|----------|----------|
| 1. Intent | Write/update `specs/…/spec.md` | Optional analysis under `spdd/analysis/` |
| 2. Design | `plan.md` for non-trivial work | REASONS under `spdd/prompt/` |
| 3. Tasks | `tasks.md` | Operations section of REASONS |
| 4. Implement | Code against FR/SC + Operations | — |
| 5. Drift | Update `spec.md` / `plan.md` | Update prompt then code (behaviour) or code then prompt (refactor) |

CLI tools (`specify`, `openspdd`) are **optional** later; folder layout and writing standards are mandatory now.

---

## Spec layers

| Layer | Path | Purpose |
|-------|------|---------|
| **Constitution** | [`.specify/memory/constitution.md`](../.specify/memory/constitution.md) | Immutable project principles |
| **Feature specs** | [`specs/`](../specs/) | Canonical product WHAT (Spec Kit) |
| **Product index** | [`product/`](product/) | Short stubs linking to `specs/` |
| **Domain** | [`domain/`](domain/) | Business rules |
| **API** | [`api/`](api/) | OpenAPI + examples |
| **Decisions** | [`decisions/`](decisions/) | ADRs |
| **Environment** | [`project-environment.md`](project-environment.md) | Spec inputs vs generated mocks |
| **Testing** | [`testing-guide.md`](testing-guide.md) | Manual QA |
| **Design prompts** | [`spdd/`](../spdd/) | OpenSPDD REASONS |

---

## Sources of truth

1. **Constitution:** [`.specify/memory/constitution.md`](../.specify/memory/constitution.md)  
2. **Product features:** [`specs/*/spec.md`](../specs/)  
3. **API:** [`api/heavyrental-openapi.yaml`](api/heavyrental-openapi.yaml)  
4. **Domain:** [`domain/booking-status-machine.md`](domain/booking-status-machine.md), [`domain/list-filters.md`](domain/list-filters.md)  
5. **Project context:** [`00-project-overview.md`](00-project-overview.md)  

---

## Current app map

| Screen | Navigation | Spec Kit | Primary code |
|--------|------------|----------|--------------|
| Login | `AppScreen.LOGIN` | [001-admin-login](../specs/001-admin-login/spec.md) | `ui/screens/LoginScreen.kt` |
| Home | `AppScreen.HOME` | [002-home-dashboard](../specs/002-home-dashboard/spec.md) | `ui/screens/HomeScreen.kt` |
| Deliveries | `AppScreen.DELIVERIES` | [003-deliveries](../specs/003-deliveries/spec.md) | `ui/screens/DeliveryListScreen.kt` |
| Returns | `AppScreen.RETURNS` | [004-returns](../specs/004-returns/spec.md) | `ui/screens/ReturnListScreen.kt` |
| Offline / API failure | shell banner | [005-offline-fallback](../specs/005-offline-fallback/spec.md) | `MainActivity` / `AppViewModel` |
| API endpoint config | `app/api.properties` | [084-api-endpoint-toggle](../specs/084-api-endpoint-toggle/spec.md) | `BuildConfig.API_SERVER_TARGET`, `ApiEndpointConfig` |

**Package root:** `com.heavyrental`

**Runtime (dev):**

- Default HTTP mock: Mockoon or Prism on host port `8081`
- Android emulator: `http://10.0.2.2:8081/` when `api.server.target=MOCKOON`; set `SPRING_BOOT` in `app/api.properties` (or `local.properties`) for `http://10.0.2.2:8080/` (host `localhost:8080`)
- Auth: interim → access JWT via `/api/auth/*`
- List load: `GET /api/deliveries`, `GET /api/returns` (+ bookings); seed fallback on failure

---

## Development workflow

For every feature or behaviour change:

1. Update **Spec Kit** `specs/…/spec.md` (stories, FR, SC)  
2. Update **domain** if business logic changes  
3. Update **OpenAPI** + examples if the network contract changes  
4. For non-trivial design: write/update **OpenSPDD REASONS** under `spdd/prompt/`  
5. Refresh mocks from API examples when needed  
6. Implement app code  
7. Add or adjust tests  

### Checklist (PR)

- [ ] Spec Kit `spec.md` updated (if user-visible)  
- [ ] FR/SC testable; no unresolved ambiguities  
- [ ] Domain rule updated (if status/filter logic)  
- [ ] OpenAPI + examples updated (if API)  
- [ ] OpenSPDD REASONS created/updated (if non-trivial design)  
- [ ] Mock still matches contract (`npm run mock:prepare` / `mock:verify`)  
- [ ] Constitution gates considered  
- [ ] Tests pass; app verified against mock or Spring  

---

## Mock strategy

| Goal | Tool | Driven by |
|------|------|-----------|
| Run full app against fake HTTP | Mockoon or Prism on port `8081` | OpenAPI + `api/examples/` |
| Automated client tests | OkHttp MockWebServer | Same JSON examples / schemas |
| Booking lists without server | `MockDataRepository` | Domain examples (seed/fallback — **not** auth) |

```bash
npm install
npm run mock:prism      # or: npm run mock:mockoon
npm run mock:verify
```

Details: [`mocks/README.md`](../mocks/README.md), [decisions/002-mock-strategy.md](decisions/002-mock-strategy.md).  
Manual QA: [`testing-guide.md`](testing-guide.md).

---

## Known gaps (v1)

| Topic | Current behaviour | Spec stance |
|-------|-------------------|-------------|
| Auth | Interim → access JWT; Mockoon canned; tokens in memory | Real credentials need Spring; secure storage future |
| List data | Loads deliveries/returns endpoints; seed offline | Server/mock owns membership for list GETs |
| Status updates | Optimistic local update if PATCH fails | Required product behaviour |
| Persistence | No Room / offline queue | In-memory state only for v1 |

---

## File index

```text
.specify/memory/constitution.md
specs/                          # Spec Kit feature specs (canonical product)
spdd/                           # OpenSPDD analysis + REASONS prompts
specification/
  README.md                     # this file
  00-project-overview.md
  project-environment.md
  testing-guide.md
  product/                      # index stubs → specs/
  domain/
  api/
  decisions/
```
