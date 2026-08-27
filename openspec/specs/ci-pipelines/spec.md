# CI Pipelines Specification

## Purpose

GitHub Flow delivery for this Android repository. Callers live in `.github/workflows/mobile-*-caller.yml`; reusable pipelines are `fast-feedback-pipeline.yml`, `integration-pipeline.yml`, and `release-pipeline.yml`.

## Requirements

### Requirement: Canonical repository default

Reusable Fast Feedback, Integration, and Release workflows MUST set `DEFAULT_APP_REPOSITORY` to `Heavy-Rental/heavy-rental-mobile` (ADR-0002).

#### Scenario: Defaults match
- GIVEN the three reusable pipeline YAML files
- WHEN `DEFAULT_APP_REPOSITORY` is read
- THEN each value is `Heavy-Rental/heavy-rental-mobile`

### Requirement: Fast Feedback is Integration-only on feature branches

`mobile-fast-feedback-caller.yml` MUST run on push to branches other than `master` and `develop` (and on `workflow_dispatch`). It MUST call `fast-feedback-pipeline.yml` only. The reusable pipeline MUST `assert-caller` so only that caller is allowed. Fast Feedback MUST NOT run QC, Security, CodeQL, or mock contract tests.

#### Scenario: Feature-branch push
- GIVEN a push to `HR-242-update-documentation-for-mobile-application-project`
- WHEN Fast Feedback runs
- THEN only Integration (checkout, JDK 17, Android SDK, Gradle, layout) executes

### Requirement: CI on develop PR and push

`mobile-ci-caller.yml` MUST run on pull_request to `develop` (opened, synchronize, reopened, ready_for_review), push to `develop`, and `workflow_dispatch`. It MUST call `integration-pipeline.yml` only (MUST NOT `uses:` Fast Feedback). On pull_request, Integration SHOULD reuse a successful Fast Feedback run for the head SHA when one exists.

#### Scenario: PR into develop
- GIVEN a pull request targeting `develop`
- WHEN CI runs
- THEN jobs include Integration, Quality Control, Security Testing, CodeQL Analysis, Mock Contract Tests, and GitHub Flow CI Gate

### Requirement: Release is manual

`mobile-release-caller.yml` MUST trigger only on `workflow_dispatch` and MUST call `release-pipeline.yml`. Release MUST produce an unsigned APK and MAY run DAST and publish a GitHub Release. It MUST NOT push to GHCR or Play Store in v1. It MUST NOT use `secrets: inherit` for cloud deploy (this repo has no portal CD).

#### Scenario: Manual release
- GIVEN Actions → Release → Run workflow
- WHEN the reusable pipeline runs
- THEN Integration and Quality Control run
- AND an unsigned APK is packaged
- AND no GHCR image is published

### Requirement: Caller gates

Each reusable pipeline MUST keep an `assert-caller` job that rejects any workflow other than its designated `mobile-*-caller.yml`.

#### Scenario: Wrong caller rejected
- GIVEN a workflow other than `mobile-ci-caller.yml` attempts to `uses:` `integration-pipeline.yml`
- WHEN `assert-caller` runs
- THEN the job fails

### Requirement: Legacy Android CI is not GitHub Flow SoT

`.github/workflows/android-ci.yml` MAY still run on push/PR to `develop`/`main`. Docs and OpenSpec MUST treat the `mobile-*-caller.yml` pair as the GitHub Flow source of truth. Agents MUST NOT document `main` as the release branch (canonical integration branch is `develop`; release checkout default is `master`).

#### Scenario: Contributor asks which CI matters
- GIVEN both `android-ci.yml` and `mobile-ci-caller.yml` exist
- WHEN they read `openspec/specs/ci-pipelines/spec.md` or the root README
- THEN GitHub Flow callers are listed as SoT
- AND `android-ci.yml` is labeled a legacy leftover
