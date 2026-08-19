# Mock API servers (OpenAPI-driven)

These mocks are generated from:

- `specification/api/heavyrental-openapi.yaml`
- `specification/api/examples/*.json`

Do **not** hand-edit files under `mocks/.generated/` or treat the Mockoon environment as the source of truth. Change the specs, then re-run prepare / start.

## Prerequisites

- Node.js 18+ (project verified with Node 26)
- From the repo root:

```bash
npm install
```

## Start a mock (port 8081)

Pick **one**:

```bash
# Stoplight Prism — serves the OpenAPI contract directly
npm run mock:prism

# Mockoon CLI — environment built from OpenAPI + example JSON files
npm run mock:mockoon
```

Regenerate assets without starting a server:

```bash
npm run mock:prepare
```

## Full manual QA guide

Step-by-step Mockoon + **Postman** + **Android** walkthrough (checklists, auth sequence, troubleshooting):

→ [`specification/testing-guide.md`](../specification/testing-guide.md)

## Android app

`RetrofitInstance` uses:

```text
http://10.0.2.2:8081/
```

That is the emulator’s alias for the host machine’s `localhost:8081`.

| Client | URL |
|--------|-----|
| Emulator | `http://10.0.2.2:8081/` |
| Host browser / curl | `http://127.0.0.1:8081/` |
| Physical device | `http://<your-lan-ip>:8081/` (update app base URL) |

## Verify

With a mock running in another terminal:

```bash
npm run mock:verify
```

Checks:

- `GET /api/bookings`
- `GET /api/deliveries`
- `GET /api/returns`
- `PATCH /api/deliveries/{id}/status`
- `PATCH /api/returns/{id}/status` — also asserts ADR 003 (`returnNotes` echo). That behaviour is **Mockoon-only**. Against Prism, skip the echo check:

```bash
# PowerShell
$env:MOCK_EXPECT_ECHO="0"; npm run mock:verify

# bash
MOCK_EXPECT_ECHO=0 npm run mock:verify
```

CI Mock Contract Tests start **Mockoon** (not Prism) so the echo assertion stays a gate.

## What gets generated

| Path | Purpose |
|------|---------|
| `mocks/.generated/openapi.bundled.yaml` | OpenAPI with **inline** examples for Prism |
| `mocks/.generated/*-item.json` | Single-resource bodies for PATCH/GET-by-id |
| `mocks/mockoon/heavy-rental.environment.json` | Mockoon env; list routes use **FILE** bodies pointing at `specification/api/examples` |

## Mockoon desktop app

1. `npm run mock:prepare`
2. Open Mockoon → **Open environment** → select  
   `mocks/mockoon/heavy-rental.environment.json`
3. Ensure port is **8081** and start the environment

Or use **Import OpenAPI** on `specification/api/heavyrental-openapi.yaml`, then paste example JSON from `specification/api/examples/` into responses.

## Ports and env vars

| Variable | Default | Meaning |
|----------|---------|---------|
| `MOCK_PORT` | `8081` | Listen port |
| `MOCK_HOST` | `0.0.0.0` | Bind address (all interfaces) |

Example:

```bash
# PowerShell
$env:MOCK_PORT=8081; npm run mock:prism
```

## Related specs

- [specification/api/README.md](../specification/api/README.md)
- [specification/decisions/002-mock-strategy.md](../specification/decisions/002-mock-strategy.md)
