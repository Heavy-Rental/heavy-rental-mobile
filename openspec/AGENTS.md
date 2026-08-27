# OpenSpec — agent / engineer reading order

## Always

1. [`../README.md`](../README.md) — project map
2. [`../specification/setup-guide.md`](../specification/setup-guide.md) — first-time clone and run
3. [`../adr/README.md`](../adr/README.md) — in-force ADRs (walk `Supersedes:`)
4. [`config.yaml`](./config.yaml) — schema and conflict rules
5. Owning `specs/<capability>/spec.md`
6. Matching product file under [`../specification/`](../specification/) when changing UI/domain/API
7. Active work under [`changes/`](./changes/) if present

## By area

| Area | Read |
|------|------|
| Documentation stack | `specs/documentation-system` + ADR-0001 + [`../spdd/README.md`](../spdd/README.md) |
| Runtime / mocks | `specs/project-environment` + ADR-0003, 0004, 0005, 0007 + `specification/setup-guide.md` |
| CI / Release | `specs/ci-pipelines` + ADR-0002 |
| Login / Google / roles | `specification/product/01-login.md` + ADR-0006, 0008 + Spring `auth-login-logout` |
| Home / deliveries / returns | `specification/product/02`–`05` + `domain/` |
| Customer bookings | `specification/product/06-customer-bookings.md` |
| HTTP shapes | `specification/api/heavyrental-openapi.yaml` |

## Backend (read-only, normative for server facts)

https://github.com/Heavy-Rental/heavy-rental-spring-rest-api — especially:

- `openspec/specs/auth-login-logout/` (including Google → `ROLE_DRIVER`)
- `openspec/specs/booking-delivery-return/` (DRIVER may call bookings/deliveries/returns; `items[]` complete)
- `openspec/AGENTS.md` and `DOCUMENTATION.md`

Do **not** restore claims that booking routes exist only on `HR-80`, that drivers are locked out of ops APIs, or that first-time Google sign-in provisions `ROLE_USER`.

## OpenSPDD and ADR

[`../spdd/README.md`](../spdd/README.md) — REASONS canvases. Per-change `design.md` is the OpenSPDD canvas; `adr.md` is the locked decision record.

## Archives

- Documentation stack baseline: [`changes/archive/2026-08-27-hr-242-openspec-openspdd-adr/`](./changes/archive/2026-08-27-hr-242-openspec-openspdd-adr/)
