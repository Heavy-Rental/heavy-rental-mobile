# Feature specifications (GitHub Spec Kit)

This directory holds **canonical product feature specifications** in the
[GitHub Spec Kit](https://github.com/github/spec-kit) shape.

## Layout

```text
specs/
  README.md                 # this file
  templates/                # offline Spec Kit-aligned templates
    spec.md
    plan.md
    tasks.md
  001-admin-login/
    spec.md                 # WHAT (required)
    plan.md                 # HOW overview (optional for small features)
    tasks.md                # checklist (optional)
  …
  084-api-endpoint-toggle/
    spec.md
    plan.md
    tasks.md
```

## Numbering

| Folder | Feature |
|--------|---------|
| `001-admin-login` | Admin login / logout |
| `002-home-dashboard` | Home dashboard |
| `003-deliveries` | Today’s deliveries |
| `004-returns` | Today’s returns |
| `005-offline-fallback` | Offline / API failure fallback |
| `084-api-endpoint-toggle` | Mockoon ↔ Spring Boot API toggle (HR-84) |

Use sequential `00N` for product features, or ticket numbers (`084-…`) when a
ticket owns the feature.

## What goes where

| Artifact | Contains | Must not contain |
|----------|----------|------------------|
| `spec.md` | User stories, FR-###, SC-###, edge cases, assumptions | Stack-specific HOW (prefer plan / REASONS) |
| `plan.md` | Tech context, constitution gates, structure | Unbounded speculative design |
| `tasks.md` | Ordered, checkable implementation tasks | Vague “do the feature” blobs |

**HTTP contract** stays in `specification/api/heavyrental-openapi.yaml`.  
**Domain rules** stay in `specification/domain/`.  
**Implementation design contracts** (OpenSPDD REASONS) live under `spdd/prompt/`.

## Lifecycle

1. Write or update `spec.md` (intent).  
2. For non-trivial work: `plan.md` + OpenSPDD REASONS under `spdd/`.  
3. Break into `tasks.md`.  
4. Implement; keep specs and REASONS in sync with behaviour.  
5. Point `specification/product/` index stubs at the canonical `specs/…/spec.md`.

## Constitution

Project non-negotiables: [`.specify/memory/constitution.md`](../.specify/memory/constitution.md).

## Related

- [spdd/README.md](../spdd/README.md) — OpenSPDD REASONS workflow  
- [specification/README.md](../specification/README.md) — baseline domain, API, testing  
