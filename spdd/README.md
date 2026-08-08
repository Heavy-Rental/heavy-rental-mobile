# OpenSPDD — Structured Prompt-Driven Development

This directory holds **versioned design contracts** for the Heavy Rental mobile
app, following [OpenSPDD](https://github.com/gszhangwei/open-spdd) /
[Structured Prompt-Driven Development](https://martinfowler.com/articles/structured-prompt-driven/).

Product **intent** lives under [`specs/`](../specs/) (GitHub Spec Kit).  
**HTTP** lives under [`specification/api/`](../specification/api/).  
This folder captures **how** a non-trivial change should be designed and generated.

## Layout

```text
spdd/
  README.md
  analysis/     # strategic analysis (domain concepts, risks)
  prompt/       # REASONS Canvas structured prompts (design contracts)
```

## REASONS Canvas (seven dimensions)

| | Section | Purpose |
|---|---------|---------|
| **R** | Requirements | Why, DoD, scope |
| **E** | Entities | Domain model (prefer Mermaid) |
| **A** | Approach | Strategy and trade-offs |
| **S** | Structure | Layers and dependencies |
| **O** | Operations | Ordered, precise implementation steps |
| **N** | Norms | Coding standards and patterns |
| **S** | Safeguards | Non-negotiable constraints |

All seven sections MUST be fully populated — no empty headers.

## Filename convention

```text
{TICKET}-{YYYYMMDDHHmm}-[{Feat|Fix|Refactor|Docs|Test}]-{scope}-{description}.md
```

Example: `HR-84-202608081200-[Feat]-network-api-endpoint-toggle.md`

## Workflow (content-first; CLI optional later)

```text
Business / Spec Kit spec.md
        │
        ▼
  spdd/analysis/…          (strategic analysis)
        │
        ▼
  spdd/prompt/…            (REASONS Canvas)
        │
        ▼
  Implementation
        │
   ┌────┴────┐
   │         │
 Behaviour  Refactor only
 change
   │         │
   ▼         ▼
 Update REASONS   Update code first
 first, then code then sync REASONS
```

**Rule:** When reality diverges, **fix the structured prompt first** for
behaviour changes — then update the code. For pure refactors (no behaviour
change), update code first and sync the prompt.

## When to write a REASONS prompt

| Scenario | REASONS required? |
|----------|-------------------|
| Non-trivial design (networking, auth, multi-layer) | Yes |
| HR-84 style architecture choice | Yes |
| Typo / copy-only UI tweak | No |
| OpenAPI field rename already fully specified | Optional if Operations would only restate OpenAPI |

## Related

- [specs/README.md](../specs/README.md)  
- [`.specify/memory/constitution.md`](../.specify/memory/constitution.md)  
- [specification/README.md](../specification/README.md)  
