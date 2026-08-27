## ADDED Requirements

### Requirement: Canonical repository default

Reusable pipelines MUST set `DEFAULT_APP_REPOSITORY` to `Heavy-Rental/heavy-rental-mobile`.

#### Scenario: Defaults match
- GIVEN Fast Feedback, Integration, and Release YAML
- WHEN `DEFAULT_APP_REPOSITORY` is read
- THEN each value is `Heavy-Rental/heavy-rental-mobile`

### Requirement: Fast Feedback is Integration-only on feature branches

`mobile-fast-feedback-caller.yml` MUST be the only Fast Feedback entry point.

#### Scenario: Feature-branch push
- GIVEN a push to a non-`develop`/non-`master` branch
- WHEN Fast Feedback runs
- THEN only Integration executes

### Requirement: CI on develop PR and push

`mobile-ci-caller.yml` MUST call `integration-pipeline.yml` for PRs and pushes to `develop`.

#### Scenario: PR into develop
- GIVEN a pull request targeting `develop`
- WHEN CI runs
- THEN Integration, QC, Security, CodeQL, Mock Contract Tests, and the GitHub Flow gate run

### Requirement: Release is manual

Release MUST be `workflow_dispatch` only; unsigned APK; no GHCR/Play in v1.

#### Scenario: Manual release
- GIVEN Actions → Release → Run workflow
- WHEN the pipeline runs
- THEN an unsigned APK is packaged

### Requirement: Legacy Android CI is not GitHub Flow SoT

`android-ci.yml` MAY exist; callers are SoT.

#### Scenario: Contributor asks which CI matters
- GIVEN both workflow files
- WHEN they read this spec
- THEN GitHub Flow callers are listed as SoT
