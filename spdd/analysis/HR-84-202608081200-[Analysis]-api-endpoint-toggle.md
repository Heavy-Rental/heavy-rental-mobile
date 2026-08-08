# Analysis: API endpoint toggle (HR-84)

**Ticket**: HR-84  
**Timestamp**: 202608081200  
**Input**: Spec Kit [`specs/084-api-endpoint-toggle/spec.md`](../../specs/084-api-endpoint-toggle/spec.md)  
**Codebase scan**: `RetrofitInstance`, repositories, Login screen, TokenSession

## Domain concepts

| Concept | Existing? | Notes |
|---------|-----------|--------|
| REST base URL | Yes (hardcoded) | Was single constant `10.0.2.2:8081` |
| Mockoon / Prism mock | Yes | Port 8081, OpenAPI-driven |
| Spring Boot API | External | Port 8080 host `localhost` |
| Auth session tokens | Yes | In-memory only; backend-specific |
| Developer preference | New | Persist Mock vs Spring selection |

## Strategic direction

1. Treat host selection as **configuration**, not a compile-time edit.  
2. Keep **one Retrofit service instance**; rewrite host/port per request.  
3. Default remains Mockoon for demos and existing docs.  
4. Switching backends **must invalidate session** (token incompatibility).  
5. UI affordance is **debug-only** on Login (pre-auth moment).

## Risks & edge cases

| Risk | Mitigation |
|------|------------|
| Stale tokens after switch | Clear `TokenSession` + force logout state |
| Repositories hold dead Retrofit | Interceptor approach; stable `api` reference |
| Preference not ready before first call | `ApiEndpointConfig.init` in `Activity.onCreate` before composition |
| Physical device | Out of scope; document emulator-first |
| Release leakage of toggle | `BuildConfig.DEBUG` gate |
| Mockoon vs Spring behaviour drift | Same OpenAPI paths; document auth canned vs real |

## Acceptance coverage (from analysis)

- Default Mockoon  
- Spring host mapping  
- Persistence  
- Session clear  
- Debug-only UI  

## Decision

Proceed to REASONS Canvas and implementation as specified in Spec Kit plan.
