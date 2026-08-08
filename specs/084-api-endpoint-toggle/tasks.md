# Tasks: API endpoint configuration (HR-84)

**Spec**: [spec.md](./spec.md) | **Plan**: [plan.md](./plan.md)

## Phase 1 — Spec & design

- [x] Spec.md (properties, no UI)
- [x] Plan.md

## Phase 2 — Implementation

- [x] T1: `app/api.properties`
- [x] T2: Gradle load + `BuildConfig.API_SERVER_TARGET`
- [x] T3: Read-only `ApiEndpointConfig` from BuildConfig
- [x] T4: Keep `BaseUrlInterceptor`
- [x] T5: Remove Login Switch + ViewModel mutation path
- [x] T6: Docs/specs updated

## Phase 3 — Verification

- [ ] Compile debug
- [ ] Manual: MOCKOON default
- [ ] Manual: SPRING_BOOT via properties + rebuild
