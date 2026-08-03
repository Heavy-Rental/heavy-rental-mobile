# API specification

## Source of truth

[`heavyrental-openapi.yaml`](heavyrental-openapi.yaml) — OpenAPI 3.0 contract for the Heavy Rental HTTP API.

## Base URL (development)

| Client | URL |
|--------|-----|
| Android emulator | `http://10.0.2.2:8081/` |
| Host machine / Mockoon | `http://localhost:8081/` |
| Physical device | `http://<host-lan-ip>:8081/` |

Configured in app code: `com.heavyrental.network.RetrofitInstance` (`BASE_URL`).

## Endpoints (summary)

| Method | Path | Used by v1 Android client |
|--------|------|---------------------------|
| `GET` | `/api/bookings` | Yes — primary load (`BookingRepository.getBookings`) |
| `GET` | `/api/bookings/{bookingId}` | Declared in Retrofit; not used by UI v1 |
| `PUT` | `/api/bookings/{bookingId}` | Declared in Retrofit; not used by UI v1 |
| `GET` | `/api/deliveries` | No — client derives from bookings |
| `PATCH` | `/api/deliveries/{bookingId}/status` | Yes — mobilise |
| `GET` | `/api/returns` | No — client derives from bookings |
| `PATCH` | `/api/returns/{bookingId}/status` | Yes — complete return |

## Kotlin mapping

| OpenAPI | Kotlin |
|---------|--------|
| Paths | `network/HeavyRentalApiService` (file under `network/dto/`) |
| Schemas | `network/dto/BookingDtos.kt` |
| Domain mapping | `BookingRepository` maps DTO → `data.models.Booking` |

## Examples

| File | Use |
|------|-----|
| [`examples/bookings.json`](examples/bookings.json) | `GET /api/bookings` body |
| [`examples/deliveries.json`](examples/deliveries.json) | `GET /api/deliveries` body |
| [`examples/returns.json`](examples/returns.json) | `GET /api/returns` body |
| [`examples/status-update-request.json`](examples/status-update-request.json) | PATCH status body sample |

**Note:** Example dates use a fixed calendar day (`2026-08-03`) for stable fixtures. The live app mock (`MockDataRepository`) uses `LocalDate.now()`. When testing list filters against fixtures, treat `2026-08-03` as “today” or rewrite dates in the test setup.

## Running mocks (wired from this OpenAPI)

From the **repo root** (requires Node.js):

```bash
npm install

# Option A — Stoplight Prism (OpenAPI-native)
npm run mock:prism

# Option B — Mockoon CLI (env generated from OpenAPI + examples/)
npm run mock:mockoon

# Smoke-test either server
npm run mock:verify
```

Details: [`mocks/README.md`](../../mocks/README.md).

### What the scripts do

1. `scripts/prepare-mocks.mjs` reads this OpenAPI file + `examples/*.json`
2. Writes `mocks/.generated/openapi.bundled.yaml` (inline examples for Prism)
3. Writes `mocks/mockoon/heavy-rental.environment.json` (Mockoon; list routes serve the example files)
4. Starts the chosen server on **port 8081** (`0.0.0.0`)

### Mockoon desktop

```bash
npm run mock:prepare
```

Then open `mocks/mockoon/heavy-rental.environment.json` in the Mockoon app.

## Related decisions

- [001-openapi-as-api-source.md](../decisions/001-openapi-as-api-source.md)
- [002-mock-strategy.md](../decisions/002-mock-strategy.md)
