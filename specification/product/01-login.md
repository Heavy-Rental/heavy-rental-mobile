# Feature: Admin login

**Status:** Implemented (v1)  
**Screens:** `LoginScreen`  
**Navigation:** `AppScreen.LOGIN` → success navigates to `AppScreen.HOME`  
**Code root:** `com.heavyrental`

---

## Summary

Operators must authenticate before accessing Home, Deliveries, or Returns. In v1, authentication is **client-only** (no auth API).

---

## Actors

- **Admin / operator** — field or office staff managing mobilisation and returns

---

## Credentials (v1)

| Field | Value |
|-------|--------|
| Email | `admin@heavyrental.com` |
| Password | `admin123` |
| Display name after login | `Admin` |

Source of truth in code today: `MockDataRepository` constants (`ADMIN_EMAIL`, `ADMIN_PASSWORD`, `ADMIN_NAME`). Any change must update this spec and the mock/fixture.

---

## Acceptance criteria

### Successful login

```gherkin
Feature: Admin login

  Scenario: Valid credentials open the home screen
    Given the user is on the login screen
    And the user is not logged in
    When the user enters email "admin@heavyrental.com"
    And the user enters password "admin123"
    And the user submits login
    Then the user is marked as logged in
    And the admin display name is "Admin"
    And the current screen is HOME
    And no login error is shown
```

### Invalid login

```gherkin
  Scenario: Invalid credentials show an error
    Given the user is on the login screen
    When the user enters an email or password that does not match the v1 admin credentials
    And the user submits login
    Then the user remains logged out
    And the login error "Invalid email or password." is shown
    And the current screen remains LOGIN
```

### Email normalisation

```gherkin
  Scenario: Email is trimmed and compared case-insensitively
    Given the user is on the login screen
    When the user enters email "  Admin@HeavyRental.com  "
    And the user enters password "admin123"
    And the user submits login
    Then login succeeds
```

Password comparison is **case-sensitive** and exact (no trim on password in `AppViewModel.login`).

### Logout

```gherkin
  Scenario: Logout returns to login and clears session state
    Given the user is logged in
    When the user chooses logout
    Then the user is logged out
    And the current screen is LOGIN
    And session fields (admin name, login error) are reset
```

Logout is implemented by resetting `AppState()` entirely (`AppViewModel.logout`).

---

## UI notes (v1)

| Element | Behaviour |
|---------|-----------|
| Brand | "Heavy Rental" / "Administrator Portal" |
| Email field | Email keyboard, next IME action |
| Password field | Visibility toggle |
| Error | Shown when `loginError` is non-null |

---

## Out of scope (v1)

- Remote authentication (`POST /api/auth/login` or similar)
- Roles / permissions beyond a single admin
- Password reset, MFA, session expiry tokens
- Biometric login
- Secure storage of credentials

---

## Implementation notes

| Concern | Location |
|---------|----------|
| Login / logout logic | `viewmodel/AppViewModel.kt` — `login`, `logout` |
| Credentials | `data/repository/MockDataRepository.kt` |
| UI | `ui/screens/LoginScreen.kt` |
| Gate (unauthenticated shell) | `MainActivity` / `HeavyRentalApp` shows only `LoginScreen` when `!isLoggedIn` |

---

## Future (not v1)

If auth moves to the API:

1. Add paths and schemas to `specification/api/heavyrental-openapi.yaml`
2. Replace client credential check with repository call
3. Update this product spec and remove hardcoded password from production builds
