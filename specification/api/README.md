# API specification

## Source of truth

[`heavyrental-openapi.yaml`](heavyrental-openapi.yaml) — OpenAPI 3.0 contract for the Heavy Rental HTTP API.

## Base URL (development)

Default client target is **Mockoon or Prism on port 8081** (OpenAPI `servers` and generated Mockoon env).

| Client | URL |
|--------|-----|
| Android emulator | `http://10.0.2.2:8081/` |
| Host machine / Mockoon | `http://localhost:8081/` |
| Physical device | `http://<host-lan-ip>:8081/` |

Configured via `app/api.properties` (`api.server.target=MOCKOON` | `SPRING_BOOT`), optional root `local.properties` override, injected as `BuildConfig.API_SERVER_TARGET`. Default Mockoon emulator URL above. Spring Boot → host `localhost:8080` / app `http://10.0.2.2:8080/`. No in-app UI toggle.

Product auth behaviour: [product/01-login.md](../product/01-login.md).

**Manual testing (Mockoon + Postman + Android):** [testing-guide.md](../testing-guide.md).

## Endpoints (summary)

| Method | Path | Used by v1 Android client |
|--------|------|---------------------------|
| `GET` | `/api/auth/getBearerToken` | Yes — login step 1 (`AuthRepository`) |
| `POST` | `/api/auth/login` | Yes — login step 2 (`AuthRepository`) |
| `POST` | `/api/auth/logout` | Yes — logout (`AuthRepository`) |
| `GET` | `/api/bookings` | Yes — shared booking state (`BookingRepository.getBookings`) |
| `GET` | `/api/bookings/{bookingId}` | Declared in Retrofit; not used by UI v1 |
| `PUT` | `/api/bookings/{bookingId}` | Declared in Retrofit; not used by UI v1 |
| `GET` | `/api/deliveries` | Yes — Delivery List + home delivery counts |
| `PATCH` | `/api/deliveries/{bookingId}/status` | Yes — mobilise |
| `GET` | `/api/returns` | Yes — Return List + home return counts |
| `PATCH` | `/api/returns/{bookingId}/status` | Yes — complete return |

**Mockoon auth caveat:** Auth routes return static canned bodies (no real JWT signing or password verification). Use a real backend to exercise 400/401/403 credential paths.

## Kotlin mapping

| OpenAPI | Kotlin |
|---------|--------|
| Paths | `network/dto/HeavyRentalApiService.kt` |
| Auth schemas | `network/dto/AuthDtos.kt` |
| Booking schemas | `network/dto/BookingDtos.kt` |
| Auth handshake | `data/repository/AuthRepository.kt` + `network/TokenSession.kt` |
| Access Bearer on business calls | `network/AuthInterceptor.kt` |
| Domain mapping | `BookingRepository` maps DTO → `Booking` / `DeliveryItem` / `ReturnItem` |
| Base URL / endpoint config | `app/api.properties`, `network/ApiServerTarget.kt`, `ApiEndpointConfig`, `BaseUrlInterceptor` |

## Examples

| File | Use |
|------|-----|
| [`examples/interim-token.txt`](examples/interim-token.txt) | `GET /api/auth/getBearerToken` body (`text/plain`) |
| [`examples/login-response.json`](examples/login-response.json) | `POST /api/auth/login` body |
| [`examples/logout-response.json`](examples/logout-response.json) | `POST /api/auth/logout` body |
| [`examples/bookings.json`](examples/bookings.json) | `GET /api/bookings` body |
| [`examples/deliveries.json`](examples/deliveries.json) | `GET /api/deliveries` body |
| [`examples/returns.json`](examples/returns.json) | `GET /api/returns` body |
| [`examples/status-update-request.json`](examples/status-update-request.json) | PATCH status body sample |

**Note:** Example dates use a fixed calendar day (`2026-08-03`) for stable fixtures. The v1 app loads **list screens from `GET /api/deliveries` and `GET /api/returns`**, so those fixtures appear even when device “today” differs. In-app seed (`MockDataRepository`) still uses `LocalDate.now()` for offline fallback only.

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
