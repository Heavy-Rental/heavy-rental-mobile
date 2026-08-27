# Project Environment Specification

## Purpose

Runtime, tooling, and local HTTP targets for the Android operations app. Narrative: `specification/project-environment.md` and `specification/00-project-overview.md`.

## Requirements

### Requirement: Android toolchain

The app MUST build with JDK 17, compile/target SDK 35, minSdk 26, application id `com.heavyrental`. CI MUST use Temurin 17 and those SDK values (`JAVA_VERSION`, `ANDROID_COMPILE_SDK` in reusable workflows).

#### Scenario: CI Integration uses JDK 17
- GIVEN `integration-pipeline.yml`
- WHEN Integration runs
- THEN `JAVA_VERSION` is `"17"`
- AND `ANDROID_COMPILE_SDK` is `"35"`

### Requirement: Default HTTP target is Spring Boot

The committed Retrofit default MUST be the Spring Boot backend at emulator `http://10.0.2.2:8080/` (`USE_MOCK_SERVER = false`). Mockoon/Prism on host port `8081` MUST remain available when `USE_MOCK_SERVER` is `true` (ADR-0007).

#### Scenario: Fresh clone talks to Spring
- GIVEN `RetrofitInstance.kt` on `develop`
- WHEN `USE_MOCK_SERVER` is read
- THEN it is `false`
- AND `REAL_BASE_URL` is `http://10.0.2.2:8080/`

### Requirement: OpenAPI-driven mocks

`npm run mock:prepare` MUST generate Mockoon env and Prism bundle from `specification/api/heavyrental-openapi.yaml` plus `specification/api/examples/`. Generated files under `mocks/.generated/` MUST NOT be hand-edited (ADR-0004, ADR-0005). CI mock contract tests MUST use Node 22.

#### Scenario: Local mock start
- GIVEN Node.js and `npm install` at repo root
- WHEN a contributor runs `npm run mock:mockoon` or `npm run mock:prism`
- THEN a mock listens on `0.0.0.0:8081`
- AND list routes serve the OpenAPI examples

### Requirement: npm scripts for mocks only

The Node `package.json` MUST provide `mock:prepare`, `mock:prism`, `mock:mockoon`, and `mock:verify`. It MUST NOT be used to build the Android app.

#### Scenario: Mock verify
- GIVEN a mock on `:8081`
- WHEN `npm run mock:verify` runs
- THEN it smoke-tests OpenAPI paths including the return-notes echo on Mockoon

### Requirement: Project setup guide

The repository MUST publish `specification/setup-guide.md` describing clone, Android Studio, emulator, the default Spring `:8080` path, the optional Mockoon/Prism path, Google Sign-In extras, physical-device cleartext, and first-run troubleshooting. That guide MUST match ADR-0004, ADR-0006, and ADR-0007. Root `README.md` MUST link to it from Quick start.

#### Scenario: New contributor finds setup
- GIVEN a clone of `Heavy-Rental/heavy-rental-mobile`
- WHEN they open `README.md` or `specification/README.md`
- THEN they are pointed at `specification/setup-guide.md`
- AND that guide states `USE_MOCK_SERVER = false` as the committed default
- AND it does not instruct committing a `true` mock flag or a LAN IP
