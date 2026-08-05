# Testing guide — Mockoon, Postman, and Android

**Status:** Active (v1)  
**Audience:** Developers verifying the mobile app against the local mock API  
**Scope:** Manual QA only (not unit/UI automations)

This guide is **how to run and verify**. Expected product behaviour remains in [`product/`](product/); HTTP shapes remain in [`api/heavyrental-openapi.yaml`](api/heavyrental-openapi.yaml). Mock generation details: [`project-environment.md`](project-environment.md) and [`mocks/README.md`](../mocks/README.md).

---

## 1. Prerequisites

| Tool | Notes |
|------|--------|
| **Node.js 18+** | For Mockoon CLI scripts (`npm run mock:*`) |
| **Repo root install** | `npm install` from `heavy-rental-mobile/` |
| **Postman** | Desktop or web app (free tier is enough) |
| **Android Studio** | Emulator recommended (`10.0.2.2` → host) |
| **Optional** | Mockoon desktop app; curl / PowerShell |

### Base URLs

| Client | Base URL |
|--------|----------|
| Host machine (Postman, curl, browser) | `http://localhost:8081` |
| Android **emulator** | `http://10.0.2.2:8081/` |
| Physical device | `http://<your-host-LAN-IP>:8081/` (must match app `BASE_URL`) |

App default: `com.heavyrental.network.RetrofitInstance` → `http://10.0.2.2:8081/` (Mockoon/Prism on host port **8081**).

---

## 2. Start the Mockoon mock API

From the **repository root** (`heavy-rental-mobile/`):

```bash
npm install
npm run mock:mockoon
```

What this does:

1. Regenerates mock assets from OpenAPI + examples (`mock:prepare`)
2. Starts Mockoon CLI on **`0.0.0.0:8081`** using `mocks/mockoon/heavy-rental.environment.json`

Leave this terminal open while testing.

### Optional: Mockoon desktop

```bash
npm run mock:prepare
```

Then open Mockoon → **Open environment** →  
`mocks/mockoon/heavy-rental.environment.json` → ensure port **8081** → start.

### Alternative mock (same port)

```bash
npm run mock:prism
```

This guide focuses on **Mockoon**; Prism serves the same OpenAPI contract on the same default port.

### Do not hand-edit generated mocks

Change `specification/api/heavyrental-openapi.yaml` and/or `specification/api/examples/*`, then re-run `npm run mock:prepare` (or start mock again). See [project-environment.md](project-environment.md).

---

## 3. Quick smoke check (before Postman / app)

In a **second** terminal (mock still running):

```bash
npm run mock:verify
```

Smoke checks include:

- `GET /api/bookings`
- `GET /api/deliveries`
- `GET /api/returns`
- `PATCH /api/deliveries/{id}/status`
- `PATCH /api/returns/{id}/status`

### Manual curl (PowerShell-friendly)

```bash
curl http://127.0.0.1:8081/api/deliveries
curl http://127.0.0.1:8081/api/returns
curl http://127.0.0.1:8081/api/bookings
curl http://127.0.0.1:8081/api/auth/getBearerToken
```

**Expect:** JSON arrays for list routes (deliveries fixture has **6** items); interim token as **plain text** for getBearerToken.

---

## 4. Postman testing

### 4.1 Create a Postman environment

| Variable | Initial value | Notes |
|----------|---------------|--------|
| `baseUrl` | `http://localhost:8081` | Host → Mockoon |
| `interimToken` | *(empty)* | Set after step 1 |
| `accessToken` | *(empty)* | Set after step 2 |

Select this environment for all requests below.

### 4.2 Optional: import OpenAPI

In Postman: **Import** → file:

- `specification/api/heavyrental-openapi.yaml`, or  
- `mocks/.generated/openapi.bundled.yaml` (examples inlined)

Then set the collection/server URL to `http://localhost:8081`. You can still follow the manual sequence below.

### 4.3 Recommended request sequence

#### 1) Get interim token

| Field | Value |
|-------|--------|
| Method | `GET` |
| URL | `{{baseUrl}}/api/auth/getBearerToken` |
| Headers | none required |
| Body | none |

**Expect:** `200`, body is **plain text** (not JSON), e.g. a mock JWT string.

**Save:** copy body into environment variable `interimToken`.

---

#### 2) Login (upgrade to access token)

| Field | Value |
|-------|--------|
| Method | `POST` |
| URL | `{{baseUrl}}/api/auth/login` |
| Header | `Authorization: Bearer {{interimToken}}` |
| Header | `Content-Type: application/json` |
| Body (raw JSON) | see below |

```json
{
  "email": "admin@localhost",
  "password": "admin1234"
}
```

**Expect:** `200` JSON with `accessToken`, `tokenType`, `expiresIn`, `username` (see [api/examples/login-response.json](api/examples/login-response.json)).

**Save:** `accessToken` from the response into the environment.

**Mockoon caveat:** credentials are **not** verified; most bodies still return canned 200. Real 401/403 behaviour needs Spring Boot. Product: [product/01-login.md](product/01-login.md).

---

#### 3) Today's deliveries (Delivery List data)

| Field | Value |
|-------|--------|
| Method | `GET` |
| URL | `{{baseUrl}}/api/deliveries` |
| Header (optional) | `Authorization: Bearer {{accessToken}}` |

**Expect:** `200`, JSON **array**, typically **6** items from [api/examples/deliveries.json](api/examples/deliveries.json).  
Fixture dates may be fixed (e.g. `2026-08-03`); that is expected for the mock.

---

#### 4) Today's returns

| Field | Value |
|-------|--------|
| Method | `GET` |
| URL | `{{baseUrl}}/api/returns` |
| Header (optional) | `Authorization: Bearer {{accessToken}}` |

**Expect:** `200`, non-empty JSON array ([api/examples/returns.json](api/examples/returns.json)).

---

#### 5) Bookings (shared state)

| Field | Value |
|-------|--------|
| Method | `GET` |
| URL | `{{baseUrl}}/api/bookings` |
| Header (optional) | `Authorization: Bearer {{accessToken}}` |

**Expect:** `200`, non-empty array ([api/examples/bookings.json](api/examples/bookings.json)).

---

#### 6) Mobilise a delivery (status PATCH)

| Field | Value |
|-------|--------|
| Method | `PATCH` |
| URL | `{{baseUrl}}/api/deliveries/DLV-003/status` |
| Header | `Content-Type: application/json` |
| Header (optional) | `Authorization: Bearer {{accessToken}}` |
| Body | see below |

```json
{
  "bookingStatus": "MOBILISED"
}
```

**Expect:** `200` with an updated delivery item body (mock may return a canned single item).  
Product transition: `CONFIRMED` → `MOBILISED` — [product/03-deliveries.md](product/03-deliveries.md).

---

#### 7) Complete a return (status PATCH)

| Field | Value |
|-------|--------|
| Method | `PATCH` |
| URL | `{{baseUrl}}/api/returns/RET-002/status` |
| Header | `Content-Type: application/json` |
| Body | |

```json
{
  "bookingStatus": "COMPLETED"
}
```

**Expect:** `200`. Product: [product/04-returns.md](product/04-returns.md).

---

#### 8) Logout

| Field | Value |
|-------|--------|
| Method | `POST` |
| URL | `{{baseUrl}}/api/auth/logout` |
| Header | `Authorization: Bearer {{accessToken}}` |

**Expect:** `200` JSON message ([api/examples/logout-response.json](api/examples/logout-response.json)). Mock does not truly revoke tokens.

### 4.4 Postman pass criteria

| Check | Pass |
|-------|------|
| Mock reachable on `:8081` | Yes |
| `GET /api/deliveries` non-empty | Yes |
| `GET /api/returns` non-empty | Yes |
| Auth handshake returns tokens | Yes (canned) |
| PATCH status returns 200 | Yes |

---

## 5. Android app testing

### 5.1 Configuration checklist

1. Mockoon is running on the **host** (`npm run mock:mockoon`).
2. App base URL is the emulator alias:

```text
http://10.0.2.2:8081/
```

(`RetrofitInstance.BASE_URL` — not `localhost`, and not Spring’s `:8080` unless intentional.)

3. Cleartext HTTP is allowed for `10.0.2.2` (`AndroidManifest` + `network_security_config`).

### 5.2 Run the app

1. Start an **Android emulator**.
2. Run the **debug** build from Android Studio (or Gradle).
3. On first composition, `HeavyRentalApp` calls `AppViewModel.loadData()` which hits:
   - `GET /api/bookings`
   - `GET /api/deliveries`
   - `GET /api/returns`

### 5.3 Functional walkthrough

#### Login

| Step | Action | Expected |
|------|--------|----------|
| 1 | Open app | Login screen |
| 2 | Enter email/password (seed: `admin@localhost` / `admin1234`) | — |
| 3 | Sign in | Home after success |

Against Mockoon, wrong passwords may still succeed (canned auth). Network errors show *Could not reach the server…* if Mockoon is down. Spec: [product/01-login.md](product/01-login.md).

#### Home dashboard

| Check | Expected |
|-------|----------|
| Delivery counts | Non-zero after successful list load |
| Return counts | Non-zero after successful list load |
| Error banner | Absent if APIs succeeded |

Spec: [product/02-home-dashboard.md](product/02-home-dashboard.md).

#### Delivery List

| Step | Action | Expected |
|------|--------|----------|
| 1 | Open **Deliveries** tab | List populated from `GET /api/deliveries` (fixture rows, not empty) |
| 2 | Filter **Confirmed** / **Mobilised** | Chips filter the loaded list |
| 3 | **Mark as Mobilised** on a CONFIRMED row | Status updates locally; PATCH attempted |

Spec: [product/03-deliveries.md](product/03-deliveries.md).

#### Return List

| Step | Action | Expected |
|------|--------|----------|
| 1 | Open **Returns** tab | List populated from `GET /api/returns` |
| 2 | Complete a MOBILISED return | Status → COMPLETED; PATCH attempted |

Spec: [product/04-returns.md](product/04-returns.md).

#### Offline / mock stopped (optional)

| Step | Action | Expected |
|------|--------|----------|
| 1 | Stop Mockoon | — |
| 2 | Force reload / relaunch app | Login may fail if auth unreachable; after prior session behaviour depends on state |
| 3 | With app past login, kill network mid-session | List load/PATCH may show banner; seed or last data kept |

Prefer: cold start with Mockoon **down** → login error. With Mockoon **up** then stop → list reload/status shows offline messaging. Spec: [product/05-offline-fallback.md](product/05-offline-fallback.md).

### 5.4 Logcat filters

In Android Studio Logcat, useful tags/filters:

```text
OkHttp
API_ERROR
AUTH_ERROR
```

**Expect** request lines to host paths such as:

```text
--> GET http://10.0.2.2:8081/api/deliveries
--> GET http://10.0.2.2:8081/api/returns
--> GET http://10.0.2.2:8081/api/auth/getBearerToken
```

### 5.5 Physical device

1. Find host LAN IP (e.g. `ipconfig` on Windows).
2. Ensure phone and PC are on the same network; firewall allows inbound **8081**.
3. Set app `BASE_URL` to `http://<LAN-IP>:8081/`.
4. Extend cleartext config for that IP if needed.
5. Mock must listen on `0.0.0.0` (default `MOCK_HOST`).

---

## 6. End-to-end acceptance checklist

Use this as a short QA pass:

- [ ] `npm run mock:mockoon` is running without errors  
- [ ] `npm run mock:verify` passes **or** curl shows non-empty `/api/deliveries`  
- [ ] Postman: auth handshake works; deliveries/returns/bookings return data  
- [ ] Postman: PATCH delivery/return status returns 200  
- [ ] Emulator app: `BASE_URL` is `http://10.0.2.2:8081/`  
- [ ] App login reaches Home  
- [ ] Delivery List is **not empty**  
- [ ] Return List is **not empty**  
- [ ] Mobilise and/or complete update UI (and attempt PATCH)  
- [ ] (Optional) Stopping mock surfaces offline/network messaging as specified  

---

## 7. Troubleshooting

| Symptom | Likely cause | What to do |
|---------|--------------|------------|
| Connection refused / failed to connect | Mock not running or wrong port | Start `npm run mock:mockoon`; confirm **8081** |
| Postman works, app login fails | App using `localhost` or wrong port | Emulator must use `10.0.2.2:8081` |
| Empty Delivery List in app | Not calling `/api/deliveries`, or mock file missing | Confirm Logcat `GET .../api/deliveries`; re-run `mock:prepare`; check `examples/deliveries.json` |
| App shows seed only + error banner | List API failed | Check mock logs; network security; base URL |
| Login always succeeds with bad password | Mockoon canned auth | Expected; use real Spring for credential negatives |
| Fixture `startDate` ≠ device today | Fixed example dates | Expected; list endpoints still return those rows |
| Port already in use | Another process on 8081 | Stop other mock/Spring, or set `MOCK_PORT` and align app URL |
| Physical device cannot connect | Firewall / wrong IP / not on LAN | Allow 8081; use host LAN IP; Mockoon on `0.0.0.0` |

---

## 8. Related documentation

| Doc | Use |
|-----|-----|
| [api/README.md](api/README.md) | Endpoint summary, mock commands |
| [api/heavyrental-openapi.yaml](api/heavyrental-openapi.yaml) | Contract source of truth |
| [project-environment.md](project-environment.md) | Specs vs generated mocks |
| [../mocks/README.md](../mocks/README.md) | Prepare / verify / desktop Mockoon |
| [product/01-login.md](product/01-login.md) | Auth scenarios |
| [product/03-deliveries.md](product/03-deliveries.md) | Delivery List + mobilise |
| [product/04-returns.md](product/04-returns.md) | Return List + complete |
| [product/05-offline-fallback.md](product/05-offline-fallback.md) | Seed + error banner |
| [decisions/002-mock-strategy.md](decisions/002-mock-strategy.md) | Why Mockoon / Prism / seed |
| [00-project-overview.md](00-project-overview.md) | Stack and runtime URLs |
