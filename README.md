# Heavy Rental Mobile

Android operations app for a heavy-equipment hire business. Operators manage **today’s deliveries** (mobilise) and **returns** (complete hire). Customers who sign in with email/password see a **read-only** list of their own bookings.

Canonical GitHub repository: [`Heavy-Rental/heavy-rental-mobile`](https://github.com/Heavy-Rental/heavy-rental-mobile).

| Item | Value |
|------|--------|
| **Application id** | `com.heavyrental` |
| **Package** | `com.heavyrental` |
| **Stack** | Kotlin · Jetpack Compose · Material 3 · Retrofit |
| **minSdk / targetSdk** | 26 / 35 |
| **Default API** | Spring Boot `http://10.0.2.2:8080/` (emulator) |
| **Behavior SoT** | OpenSpec [`openspec/specs/`](openspec/specs/) |

## Features (v1)

- **Staff login** — email/password or Google Sign-In (Credential Manager); interim → access JWT
- **Home dashboard** — today’s delivery and return counts by status
- **Deliveries** — today’s list, status chips, maps, mobilise (`CONFIRMED` → `MOBILISED`)
- **Returns** — today’s list, status chips, maps, complete (`MOBILISED` → `COMPLETED`) with optional notes
- **Customer bookings** — password `ROLE_USER` sessions only; read-only `GET /api/bookings`
- **Offline fallback** — in-app seed + optimistic status when the API is unreachable after login

### Demo accounts (Spring `data.sql`)

| Email | Password | Role | Lands on |
|-------|----------|------|----------|
| `admin@localhost` | `admin1234` | ADMIN | Home |
| `ah.tan@example.sg` | `driver123` | DRIVER | Home |
| `alex.tan@example.sg` | `customer123` | USER | Customer bookings |

Google Sign-In requires the real Spring backend, a Google Play emulator image, and a test user in Google Cloud Console. First-time Google accounts are provisioned as **`ROLE_DRIVER`** (never `ROLE_ADMIN`). See [specification/product/01-login.md](specification/product/01-login.md).

## Stack

- Kotlin (JVM 17), Android Gradle Plugin 9.2, Gradle 9.6
- Jetpack Compose + Material 3
- Retrofit 2 + OkHttp + kotlinx.serialization
- AndroidX Credentials + Google Identity for Sign in with Google
- Node.js (CI: 22) only for OpenAPI mock tooling (Prism / Mockoon)

## Documentation

| Layer | Path | Standard |
| --- | --- | --- |
| Behavior (what) | [`openspec/specs/`](openspec/specs/) | OpenSpec `spec-driven-with-adr` |
| Design contract (how) | [`spdd/prompt/`](spdd/prompt/) | OpenSPDD REASONS Canvas |
| Architecture (why) | [`adr/`](adr/) | MADR-short ADRs |
| Feature SDD | [`specification/`](specification/) | Product, domain, OpenAPI + [index](specification/README.md) |

Conflict rule: **running code / workflow YAML**, then **OpenSpec specs**, then **`specification/`**. Fix the stale file in the same change.

Backend contracts: [`Heavy-Rental/heavy-rental-spring-rest-api`](https://github.com/Heavy-Rental/heavy-rental-spring-rest-api) (`openspec/specs/`). Local packs: [`Heavy-Rental/heavy-rental-devcontainer-configuration`](https://github.com/Heavy-Rental/heavy-rental-devcontainer-configuration). Portal: [`Heavy-Rental/heavy-rental-react-web-portal`](https://github.com/Heavy-Rental/heavy-rental-react-web-portal).

## Quick start

**Full walkthrough (clone → Android Studio → emulator → Spring → login):** [`specification/setup-guide.md`](specification/setup-guide.md).

### Android app (default: Spring Boot on host `:8080`)

1. Run [`heavy-rental-spring-rest-api`](https://github.com/Heavy-Rental/heavy-rental-spring-rest-api) on port `8080`.
2. Open this repo in Android Studio.
3. Start an emulator (Google Play image if you need Google Sign-In).
4. Run the `app` configuration.

`USE_MOCK_SERVER` in `app/src/main/java/com/heavyrental/network/dto/RetrofitInstance.kt` selects the target (`false` = `:8080`, `true` = Mockoon/Prism `:8081`).

### Mock API (optional)

```bash
npm install
npm run mock:prism      # or: npm run mock:mockoon
npm run mock:verify
```

Then set `USE_MOCK_SERVER = true`. Google Sign-In is **not** implemented on the mock. Details: [`mocks/README.md`](mocks/README.md), [`specification/testing-guide.md`](specification/testing-guide.md).

### Tests

```bash
./gradlew testDebugUnitTest
```

## Project layout

```text
app/src/main/java/com/heavyrental/
  MainActivity.kt              # Compose shell
  navigation/AppScreen.kt      # LOGIN, HOME, DELIVERIES, RETURNS, CUSTOMER_BOOKINGS
  viewmodel/AppViewModel.kt    # Auth, lists, status transitions
  ui/screens/                  # Login, Home, Deliveries, Returns, Customer bookings
  data/models/                 # Booking, status machine, list filters
  data/repository/             # Auth, Booking, MockData seed
  network/                     # Retrofit, JWT claims, TokenSession
specification/                 # Product SDD, domain, OpenAPI
openspec/                      # Living behavior contracts
adr/                           # Durable architecture decisions
spdd/                          # OpenSPDD REASONS canvases
mocks/                         # Generated Mockoon env (from OpenAPI)
.github/workflows/             # Fast Feedback, CI, Release
```

## Delivery pipelines (summary)

GitHub Flow: feature branch → PR into `develop` (CI) → merge to `develop` → manual **Release**.

| Workflow | Trigger | What it runs |
|----------|---------|----------------|
| **Fast Feedback** | Push to branches other than `master`/`develop` | Integration only |
| **CI** | PR / push to `develop` | Integration (reuses Fast Feedback when possible), QC, Security, CodeQL, Mock contract tests |
| **Release** | Manual `workflow_dispatch` | Integration, QC, unsigned APK, DAST, GitHub Release (no GHCR / Play) |

Defaults: JDK 17, compile SDK 35, Node 22 for mock contract tests, `DEFAULT_APP_REPOSITORY=Heavy-Rental/heavy-rental-mobile`. Callers: `mobile-fast-feedback-caller.yml`, `mobile-ci-caller.yml`, `mobile-release-caller.yml`.

`.github/workflows/android-ci.yml` is a **legacy leftover** from HR-157 (push/PR to `develop`/`main`). It is **not** the GitHub Flow source of truth.

This repository does **not** ship Portal-style Academy/paid CD.

## Related

- Setup guide: [`specification/setup-guide.md`](specification/setup-guide.md)
- Product index: [`specification/README.md`](specification/README.md)
- OpenSpec reading order: [`openspec/AGENTS.md`](openspec/AGENTS.md)
- ADRs: [`adr/README.md`](adr/README.md)
