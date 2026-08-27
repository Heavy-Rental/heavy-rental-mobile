# ADR-0002: Canonical GitHub repository is Heavy-Rental/heavy-rental-mobile

- Status: accepted
- Date: 2026-08-27
- Tags: github, ci

## Context

Reusable CI/Release workflows take an optional `app_repository` override and fall back to `DEFAULT_APP_REPOSITORY`. A wrong org/name (for example a classroom fork) would checkout the wrong application when the pipeline is exercised from another repo (`act`, pipeline-development).

## Decision

The canonical application repository is **`Heavy-Rental/heavy-rental-mobile`**.

`DEFAULT_APP_REPOSITORY` MUST be that string in:

- `.github/workflows/fast-feedback-pipeline.yml`
- `.github/workflows/integration-pipeline.yml`
- `.github/workflows/release-pipeline.yml`

Same-repo callers may leave `app_repository` empty (checkout this repo at `github.sha`). Overrides are for `act` / pipeline-development only.

## Consequences

- Docs, OpenSpec `ci-pipelines`, and YAML MUST use `Heavy-Rental/heavy-rental-mobile`, never a classroom org default.
- Changing the canonical name requires a new ADR that supersedes this one, plus YAML and spec updates in the same change.
