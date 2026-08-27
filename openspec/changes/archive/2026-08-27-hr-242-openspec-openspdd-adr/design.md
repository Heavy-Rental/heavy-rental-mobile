# Design: HR-242 mobile OpenSpec / OpenSPDD / ADR

## Context

Branch `HR-242-update-documentation-for-mobile-application-project`. Sibling portal already uses `spec-driven-with-adr` with living `openspec/specs/`, durable `adr/`, and `spdd/prompt/`. Spring REST API uses OpenSpec as the server behavior SoT.

This repo: `specification/` product SDD is mostly accurate for screens, but README is a template, ADRs live under `specification/decisions/`, and several backend-overlapping claims are stale.

In-force ADRs before this change: none at `adr/`.

## Goals / Non-Goals

Goals:

- One documentation stack agents can follow (OpenSpec behavior, OpenSPDD contract, MADR why).
- Specs that match Kotlin, YAML, and Spring OpenSpec for overlapping facts.
- GitHub Flow callers documented; portal CD not invented.

Non-goals:

- Rewriting every product markdown file into OpenSpec requirement blocks.
- Deleting `android-ci.yml` or `BLANK_README.md`.
- Changing `USE_MOCK_SERVER`, Credential Manager, or pipelines.

## Decisions

### Decision: Three-layer docs, keep specification/

Same as the portal: OpenSpec indexes and governs; `specification/` stays detailed product SDD.

Alternatives: (a) migrate all SDD into OpenSpec only — high rewrite cost; (b) ADRs only — no testable scenarios; (c) delete `specification/` like Spring after a full OpenSpec rewrite — too large for HR-242.

### Decision: Archive this change as the baseline

There were no prior `openspec/specs/`. Requirements are written as current-state specs and this folder is archived so the next change uses deltas.

### Decision: Correct backend-overlapping facts in the same change

Leaving Google as `ROLE_USER` or drivers as locked out would make ADR-0006/0008 immediately false.

## Risks / Trade-offs

- [Duplicate sources] → Mitigation: conflict rule in `openspec/config.yaml`.
- [Agents restore HR-80 / ROLE_USER from git] → Mitigation: `openspec/AGENTS.md` warnings and product spec corrections.
- [Product specs still informal] → Mitigation: `product-features` index; migrate per change when those features move.

## Migration Plan

1. Add `openspec/config.yaml`, specs, ADRs, OpenSPDD analysis + canvas.
2. Update `specification/` facts and index.
3. Replace root README; write CHANGELOG for this alignment.
4. Archive under `openspec/changes/archive/2026-08-27-hr-242-openspec-openspdd-adr/`.

Rollback: delete the new doc trees and revert specification/README edits.

## Open Questions

None remaining for this documentation change.
