# Implementation Plan: [FEATURE]

**Branch**: `[###-feature-name]` | **Date**: [DATE] | **Spec**: [link to spec.md]

## Summary

[Primary requirement + technical approach]

## Technical Context

**Language/Version**: Kotlin / project AGP  
**Primary Dependencies**: Jetpack Compose, Retrofit, OkHttp, kotlinx.serialization  
**Storage**: In-memory session (v1); SharedPreferences only if specified  
**Testing**: JVM unit tests, MockWebServer, manual QA per testing-guide  
**Target Platform**: Android (emulator primary for local API)  
**Project Type**: Mobile app  
**Constraints**: See constitution (auth, offline, OpenAPI)  
**Scale/Scope**: [this feature]

## Constitution Check

- [ ] Spec updated under `specs/`  
- [ ] OpenAPI updated if HTTP changes  
- [ ] Domain rules updated if status/filter logic changes  
- [ ] Simplicity: no speculative features  
- [ ] Login not bypassed by seed data  
- [ ] Debug-only affordances gated from release if required  

## Project Structure

```text
app/src/main/java/com/heavyrental/
  …
```

**Structure Decision**: [what changes]

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| — | — | — |

## Related

- Spec: `./spec.md`  
- REASONS: `spdd/prompt/…`  
