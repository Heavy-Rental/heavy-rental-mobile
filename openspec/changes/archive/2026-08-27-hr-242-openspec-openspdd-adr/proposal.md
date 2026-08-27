# Proposal: HR-242 mobile documentation standard

## Why

HR-242 asks that mobile documentation and specifications be updated, consistent, and accurate using OpenSpec, OpenSPDD, and ADR — matching sibling Heavy Rental repositories. This repo still had informal SDD only, template README/CHANGELOG, and several facts that contradict Spring OpenSpec and current Kotlin.

## What Changes

- Adopt OpenSpec `spec-driven-with-adr`, OpenSPDD REASONS Canvas, and MADR-short ADRs (ADR-0001).
- Record durable decisions ADR-0002–0008 (canonical repo, OpenAPI, mocks, Google `ROLE_DRIVER`, default Spring HTTP, JWT role routing).
- Index product SDD; correct stale claims (driver lock-out, HR-80-only routes, one-asset lists, Google `ROLE_USER`).
- Replace template README with the actual app, stack, and GitHub Flow callers.
- No application runtime change.

## Capabilities

### New Capabilities

- `documentation-system` — OpenSpec + OpenSPDD + ADR collaboration rules
- `project-environment` — Android toolchain, HTTP targets, mock scripts
- `ci-pipelines` — Fast Feedback, CI, Release (legacy `android-ci.yml` noted)
- `product-features` — index of `specification/` feature SDD

### Modified Capabilities

- None (first OpenSpec baseline).

## Impact

- Docs: `openspec/`, `adr/`, `spdd/`, `specification/`, `README.md`, `CHANGELOG.md`, `mocks/README.md`
- No Kotlin or workflow YAML behavior change
