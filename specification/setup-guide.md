# Project setup guide — Heavy Rental Mobile

**Audience:** A developer cloning this repository for the first time.  
**Scope:** Install tools, open the Android app, point it at an API, run it, and confirm login.  
**Not this file:** Manual QA checklists live in [`testing-guide.md`](testing-guide.md). Product behaviour lives under [`product/`](product/).

Canonical repo: [`Heavy-Rental/heavy-rental-mobile`](https://github.com/Heavy-Rental/heavy-rental-mobile). Default HTTP target is Spring Boot on host `:8080` (ADR-0007).

---

## 1. What you will run

```text
┌─────────────────────┐         HTTP          ┌──────────────────────────────┐
│  Android emulator   │  10.0.2.2:8080  ────► │  Spring REST API (host :8080)│
│  com.heavyrental    │                       │  heavy-rental-spring-rest-api│
└─────────────────────┘                       └──────────────────────────────┘
         │
         │  optional, USE_MOCK_SERVER = true
         ▼
   Mockoon / Prism on host :8081
```

| Piece | Required for first run? | Notes |
|-------|-------------------------|--------|
| This repo + Android Studio | **Yes** | Builds and launches the app |
| Spring REST API on `:8080` | **Yes** (default) | Password login, lists, Google Sign-In |
| Node.js + `npm run mock:*` | No | Only if you flip `USE_MOCK_SERVER` |
| Google Play emulator + Cloud test user | No | Only for “Continue with Google” |

---

## 2. Prerequisites

Install these on the machine that will run Android Studio (Windows, macOS, or Linux).

| Tool | Version / notes |
|------|-----------------|
| **Git** | To clone `Heavy-Rental/heavy-rental-mobile` |
| **Android Studio** | Current stable. SDK **35**, build-tools **35.0.0**, a system image for an emulator |
| **JDK** | App source/target is **JVM 17**. CI uses Temurin 17. Android Studio’s bundled JDK is enough for IDE builds. Command-line Gradle may download a **daemon JDK 21** via `gradle/gradle-daemon-jvm.properties` — that is expected; do not retarget `app/build.gradle.kts` to 21 |
| **Node.js** | **18+** locally for mocks; CI mock tests use **22**. Not needed if you only talk to Spring |
| **Spring REST API** | Clone and run [`heavy-rental-spring-rest-api`](https://github.com/Heavy-Rental/heavy-rental-spring-rest-api) (Java 21 + PostgreSQL). Optional: Dev Container packs in [`heavy-rental-devcontainer-configuration`](https://github.com/Heavy-Rental/heavy-rental-devcontainer-configuration) |

This Android repo has **no** `.devcontainer`. You develop the app in Android Studio on the host.

### Android Studio SDK components

In **Settings → Languages & Frameworks → Android SDK**:

- SDK Platforms: **Android 15.0 (API 35)** (or the platform matching `compileSdk 35`)
- SDK Tools: **Android SDK Build-Tools 35**, **Android SDK Platform-Tools**, **Android Emulator**
- For Google Sign-In: a **Google Play** system image (not “Google APIs” and not AOSP)

Accept licenses if Studio prompts. First Gradle sync downloads the Gradle **9.6.1** wrapper from `gradle/wrapper/gradle-wrapper.properties`.

---

## 3. Clone and open the project

```powershell
cd $HOME\Desktop
git clone https://github.com/Heavy-Rental/heavy-rental-mobile.git
cd heavy-rental-mobile
git checkout develop
```

1. Start **Android Studio**.
2. **File → Open** and select the `heavy-rental-mobile` folder (the directory that contains `settings.gradle.kts` and `app/`).
3. Trust the project if asked.
4. Wait for Gradle sync. The included module is `:app` (`rootProject.name = "HeavyRental"`).
5. Confirm `app/local.properties` exists after sync (`sdk.dir=...`). That file is gitignored — never commit it.

Command-line check (optional, from the repo root):

```powershell
.\gradlew.bat :app:assembleDebug
```

On macOS/Linux use `./gradlew :app:assembleDebug`.

---

## 4. Create an emulator

1. **Device Manager → Create Virtual Device**.
2. Pick a phone (Pixel 6 class is fine).
3. System image:
   - **Google Play** — required for Credential Manager / “Continue with Google”.
   - **Google APIs** — password login and lists only; Google Sign-In will fail at the account picker.
4. Finish and **Start**.

The emulator reaches the host as **`10.0.2.2`**. That is why Retrofit uses `http://10.0.2.2:8080/`, not `localhost`.

`res/xml/network_security_config.xml` permits cleartext HTTP only for host `10.0.2.2`. Emulator traffic to Spring/Mockoon is allowed; see [§8](#8-physical-device-optional) for a real phone.

---

## 5. Start the Spring Boot API (default path)

The committed flag is `USE_MOCK_SERVER = false` in `app/src/main/java/com/heavyrental/network/dto/RetrofitInstance.kt`. Do **not** commit that flag as `true` (ADR-0007).

In a **second** clone of the API, follow that repo’s README. Minimum:

```bash
export APP_JWT_SECRET='<at least 32 characters>'
export POSTGRES_PASSWORD='<postgres password>'
./mvnw spring-boot:run
```

On Windows PowerShell:

```powershell
$env:APP_JWT_SECRET = '<at least 32 characters>'
$env:POSTGRES_PASSWORD = '<postgres password>'
.\mvnw.cmd spring-boot:run
```

Health check on the **host**:

```powershell
curl.exe -s http://localhost:8080/actuator/health
```

Auth smoke:

```powershell
$INTERIM = curl.exe -s http://localhost:8080/api/auth/getBearerToken
curl.exe -s -X POST http://localhost:8080/api/auth/login `
  -H "Authorization: Bearer $INTERIM" `
  -H "Content-Type: application/json" `
  -d '{"email":"admin@localhost","password":"admin1234"}'
```

A `200` with `accessToken` means the backend is ready. A connection refused error means Spring is not on `:8080`. A `404` on `/api/bookings` after login means a wrong base URL or an old API image — those routes exist on Spring `develop`.

### Seeded accounts (from API `data.sql`)

| Email | Password | Role | App lands on |
|-------|----------|------|----------------|
| `admin@localhost` | `admin1234` | ADMIN | Home |
| `ah.tan@example.sg` | `driver123` | DRIVER | Home |
| `alex.tan@example.sg` | `customer123` | USER | Customer bookings |

`ROLE_DRIVER` **is** allowed to call deliveries/returns/bookings. Full seed list: API `DOCUMENTATION.md`.

---

## 6. Run the Android app

1. Select the **app** run configuration and the emulator.
2. **Run** (Shift+F10) or `.\gradlew.bat :app:installDebug` then launch from the device.
3. You should see **Heavy Rental / Sign in to your account**.
4. Sign in as `admin@localhost` / `admin1234`.
5. Home should show today’s delivery and return counts from `GET /api/deliveries` and `GET /api/returns`.

If lists show seed data plus a red banner, the emulator cannot reach Spring — see [§10](#10-troubleshooting).

Logcat filters that help: `AUTH_ERROR`, `OkHttp` (body logging is on in debug).

---

## 7. Optional: Mockoon / Prism instead of Spring

Use this when you want HTTP without PostgreSQL. Google Sign-In is **not** on the mock (ADR-0004, ADR-0006).

```powershell
npm install
npm run mock:mockoon
# other terminal:
npm run mock:verify
```

Prism alternative: `npm run mock:prism` (same port **8081**). Do not start both.

Then **temporarily** set in `RetrofitInstance.kt`:

```kotlin
private const val USE_MOCK_SERVER = true
```

Rebuild and run. Mock login accepts a canned body (no real password check). Revert the flag to `false` before you push.

Regenerate mocks after OpenAPI/example edits: `npm run mock:prepare`. Do not hand-edit `mocks/.generated/`. Details: [`mocks/README.md`](../mocks/README.md), [`project-environment.md`](project-environment.md).

---

## 8. Physical device (optional)

1. Enable USB debugging; connect the phone (`adb devices` shows it).
2. Host and phone must be on the same LAN. Find the host IPv4 (e.g. `192.168.1.20`).
3. Change `REAL_BASE_URL` / `MOCK_BASE_URL` in `RetrofitInstance.kt` from `10.0.2.2` to that LAN IP **and matching port**.
4. Add that IP to `app/src/main/res/xml/network_security_config.xml` (the current file allows cleartext **only** for `10.0.2.2`). Without that, HTTP calls fail even though `usesCleartextTraffic` is set on the application tag.
5. Do not commit LAN IPs or a `true` mock flag.

---

## 9. Google Sign-In (optional)

Password login does not need this. For “Continue with Google”:

1. Emulator **Google Play** image; add a Google account under emulator Settings.
2. Spring running with `APP_GOOGLE_WEB_CLIENT_ID` equal to the **Web application** OAuth client id.
3. That Web client id must match `WEB_CLIENT_ID` in `ui/screens/LoginScreen.kt`.
4. An **Android** OAuth client in the same Cloud project: package `com.heavyrental` + the **debug keystore SHA-1**:

```powershell
keytool -list -v -keystore "$env:USERPROFILE\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
```

5. While the OAuth app is in Testing, the Google account must be listed under **Audience → Test users**.
6. First-time Google users are provisioned as **`ROLE_DRIVER`** and land on Home. An existing customer email (`ROLE_USER`) used with Google is **refused** on this app. See [product/01-login.md](product/01-login.md) and ADR-0006.

---

## 10. Troubleshooting

| Symptom | Likely cause | What to do |
|---------|--------------|------------|
| Gradle sync fails on SDK | API 35 / build-tools missing | Install SDK 35 in Studio SDK Manager |
| `sdk.dir` missing | `local.properties` not generated | Re-open the project in Studio; do not copy another machine’s path |
| App opens but login: “Could not reach the server” | Spring not on `:8080`, or emulator used `localhost` | Confirm `curl.exe http://localhost:8080/actuator/health` on the host; keep Retrofit on `10.0.2.2` |
| Login works, lists show mock seed + error banner | List GET failed (wrong API, 401, or 404) | Smoke `GET /api/deliveries` with the **access** token; check Logcat |
| `invalid_credentials` for seed users | API `data.sql` not loaded | Restart Spring after seed changes |
| Driver login then empty/error lists | Wrong API image (historical lock-out docs) | Current Spring allows `ROLE_DRIVER` on deliveries/returns |
| Google picker never appears | Not a Play image, or no Google account | Use a Play system image; add an account |
| Google 404 | `USE_MOCK_SERVER = true` | Google is Spring-only |
| Cleartext / `CLEARTEXT` on a physical device | LAN IP not in `network_security_config.xml` | Add the host IP to that file |
| Accidental mock in a PR | `USE_MOCK_SERVER = true` committed | Set it back to `false` |

---

## 11. Tests and mocks from the command line

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
```

Reports: `app/build/reports/tests/testDebugUnitTest/`.

Mock tooling (needs Node):

```powershell
npm install
npm run mock:prepare
npm run mock:verify   # mock must already be listening on :8081
```

---

## 12. What to read next

| Goal | Doc |
|------|-----|
| Product screens | [`product/`](product/) |
| Status machine / filters | [`domain/`](domain/) |
| HTTP contract | [`api/heavyrental-openapi.yaml`](api/heavyrental-openapi.yaml) |
| Manual QA (Postman + emulator) | [`testing-guide.md`](testing-guide.md) |
| Living behavior | [`openspec/AGENTS.md`](../openspec/AGENTS.md) |
| Why Spring is default / why three mocks | [`adr/0007`](../adr/0007-default-http-target-is-spring-boot.md), [`adr/0004`](../adr/0004-three-layer-mock-strategy.md) |

**Done when:** Gradle sync succeeds, Spring (or the mock) is reachable, and at least one seeded login reaches Home or Customer bookings as in the table in [§5](#5-start-the-spring-boot-api-default-path).
