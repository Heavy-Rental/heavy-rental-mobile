# Implementation Plan: API endpoint configuration

**Branch**: `HR-84-implement-toggle-switch-for-rest-api-endpoint-in-android-project`  
**Date**: 2026-08-08  
**Spec**: [spec.md](./spec.md)

## Summary

Select Mockoon (`10.0.2.2:8081`) vs Spring Boot (`10.0.2.2:8080`) via
`app/api.properties` (optional `local.properties` override), injected as
`BuildConfig.API_SERVER_TARGET`. No Login UI toggle. OkHttp
`BaseUrlInterceptor` applies the host.

## Technical Context

**Language/Version**: Kotlin, Gradle  
**Primary Dependencies**: Retrofit, OkHttp  
**Storage**: None (build-time only)  
**Target Platform**: Android emulator (primary)  

## Constitution Check

- [x] Spec under `specs/084-api-endpoint-toggle/`  
- [x] OpenAPI unchanged (host only)  
- [x] No UI developer control  
- [x] Default Mockoon  

## Project Structure

```text
app/api.properties                 # api.server.target=MOCKOON|SPRING_BOOT
app/build.gradle.kts               # load props → BuildConfig.API_SERVER_TARGET
app/src/main/java/com/heavyrental/network/
  ApiServerTarget.kt
  ApiEndpointConfig.kt             # read-only from BuildConfig
  BaseUrlInterceptor.kt
  dto/RetrofitInstance.kt
```

## Approach

| Option | Decision |
|--------|----------|
| Login Switch | Removed |
| SharedPreferences | Removed |
| Properties + BuildConfig | **Chosen** |
| Base URL interceptor | Kept |

## Related

- REASONS: `spdd/prompt/HR-84-*-api-endpoint-toggle.md`  
