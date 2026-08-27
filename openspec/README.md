# OpenSpec

Living behavior contracts for this Android app. Schema: **`spec-driven-with-adr`** (`config.yaml`).

```text
openspec/
  config.yaml          # schema, project context, agent rules
  AGENTS.md            # reading order
  specs/               # current behavior (source of truth)
    documentation-system/
    project-environment/
    ci-pipelines/
    product-features/
  changes/
    archive/           # completed changes (proposal, design, adr manifest, tasks, deltas)
```

Artifact order for a new change: `proposal → specs → design → adr → tasks`.

- Durable ADRs: `adr/NNNN-kebab-title.md` (not inside this folder). Walk `Supersedes:`.
- Change-local `adr.md` is a review manifest only.
- Product UI / domain / OpenAPI detail stays in `specification/`; `specs/product-features` indexes it. First-time setup: `specification/setup-guide.md`.
- Archived changes are history. Current behavior is `specs/` only.
- Baseline archive: `changes/archive/2026-08-27-hr-242-openspec-openspdd-adr/`.

See [Fission-AI/OpenSpec](https://github.com/Fission-AI/OpenSpec) and [spec-driven-with-adr](https://github.com/intent-driven-dev/openspec-schemas/tree/main/openspec/schemas/spec-driven-with-adr).
