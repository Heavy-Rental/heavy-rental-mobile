## ADDED Requirements

### Requirement: Android toolchain

The app MUST build with JDK 17, compile/target SDK 35, minSdk 26, application id `com.heavyrental`.

#### Scenario: CI Integration uses JDK 17
- GIVEN `integration-pipeline.yml`
- WHEN Integration runs
- THEN `JAVA_VERSION` is `"17"`

### Requirement: Default HTTP target is Spring Boot

Committed Retrofit default MUST be `http://10.0.2.2:8080/` with `USE_MOCK_SERVER = false`.

#### Scenario: Fresh clone talks to Spring
- GIVEN `RetrofitInstance.kt` on `develop`
- WHEN `USE_MOCK_SERVER` is read
- THEN it is `false`

### Requirement: OpenAPI-driven mocks

`npm run mock:*` MUST generate and serve mocks from OpenAPI + examples on port 8081.

#### Scenario: Local mock start
- GIVEN `npm install`
- WHEN `npm run mock:mockoon` runs
- THEN a mock listens on `:8081`
