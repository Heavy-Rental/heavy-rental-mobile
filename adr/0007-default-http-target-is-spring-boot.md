# ADR-0007: Default HTTP target is the Spring Boot backend

- Status: accepted
- Date: 2026-08-27
- Tags: networking, runtime

## Context

HR-78 switched the committed Retrofit base URL from Mockoon `:8081` to the real Spring Boot backend on host `:8080` (emulator `http://10.0.2.2:8080/`). Booking/delivery/return routes now exist on Spring `develop` (`booking-delivery-return` OpenSpec). Older product docs still said those routes lived only on branch `HR-80` and would `404` on `develop`.

A properties-based toggle (`BuildConfig` + gitignored override) was merged as PR #7 and reverted by PR #9. Environment selection remains a committed constant.

## Decision

1. Default `USE_MOCK_SERVER = false` in `RetrofitInstance` — emulator talks to Spring at `http://10.0.2.2:8080/`.
2. Mockoon/Prism on `:8081` remains the OpenAPI mock target, selected by setting `USE_MOCK_SERVER = true`.
3. Docs MUST treat Spring `develop` as the live ops API. Do **not** claim booking routes exist only on `HR-80`.
4. A local.properties/`BuildConfig` toggle MAY return later; until then the flag is a source edit (known gap in [05-offline-fallback.md](../specification/product/05-offline-fallback.md)).

## Consequences

- Local Android work needs a running Spring API (or an explicit mock flag flip).
- Google Sign-In works only against Spring (ADR-0006).
- Contributors who leave `USE_MOCK_SERVER = true` in a PR will point CI-unrelated local runs at the mock; the constant MUST stay `false` on `develop`.
