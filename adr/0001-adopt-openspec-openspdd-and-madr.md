# ADR-0001: Adopt OpenSpec, OpenSPDD, and MADR as the documentation standard

- Status: accepted
- Date: 2026-08-27
- Tags: documentation, openspec, openspdd, adr

## Context

Product behaviour lived in `specification/` (informal SDD). Architectural decisions lived in `specification/decisions/` with an ADR-like shape but no immutability rules, no repo-root sequence, and no OpenSpec change lifecycle. Sibling repositories (`heavy-rental-react-web-portal`, `heavy-rental-spring-rest-api`, `heavy-rental-devcontainer-configuration`) already use OpenSpec `spec-driven-with-adr`, OpenSPDD REASONS Canvas, and MADR-short ADRs.

The root `README.md` and `CHANGELOG.md` were still Best-README-Template placeholders. Several product specs still claimed backend facts that the Spring OpenSpec SoT had already superseded (Google auto-provision `ROLE_USER`, driver lock-out of ops routes, booking APIs only on branch `HR-80`).

OpenSpec's default `spec-driven` schema archives `design.md` with the change, so architectural rationale disappears from the living tree. OpenSPDD's REASONS Canvas is an executable design contract (norms + safeguards), which specs and ADRs do not replace.

## Decision

Use three complementary artifacts, all in this repository:

1. **OpenSpec** with schema `spec-driven-with-adr` (`openspec/config.yaml`). Living behavior is `openspec/specs/`. Each change uses `proposal → specs → design → adr → tasks`.
2. **OpenSPDD** REASONS Canvas under `spdd/analysis/` and `spdd/prompt/` for implementation contracts (Requirements, Entities, Approach, Structure, Operations, Norms, Safeguards).
3. **MADR-short ADRs** under `adr/NNNN-kebab-title.md`. Accepted ADRs are immutable. Change-local `openspec/changes/<change>/adr.md` is a review manifest only.

`specification/` remains the detailed product SDD (screens, Gherkin, OpenAPI). OpenSpec `product-features` maps each file. On conflict, the running code / workflow YAML wins; then OpenSpec specs; then `specification/` is updated in the same change.

## Consequences

- Future behavior or architecture work MUST add or supersede ADRs and OpenSpec deltas before treating docs as done.
- Agents MUST read in-force ADRs during design.
- Small UI tweaks MAY skip a new durable ADR (manifest records "none") but MUST NOT contradict in-force ADRs.
- Duplicate narrative across OpenSpec, OpenSPDD, and `specification/` is a maintenance cost; the mapping in `specification/README.md` and `openspec/specs/product-features/spec.md` is the consistency check.
- Informal files under `specification/decisions/` become stubs that point at `adr/`.
