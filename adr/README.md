# Architecture Decision Records

Durable architecture log for `heavy-rental-mobile`.

These files are the **why** of the system. OpenSpec specs (`openspec/specs/`) are the **what**. OpenSPDD REASONS canvases (`spdd/prompt/`) are the **how** for a given change.

## Standard

- Format: [MADR-short](https://adr.github.io/madr/) (title, status/date, context, decision, consequences).
- Location: this folder only (`adr/NNNN-kebab-title.md`). Never store durable ADRs inside `openspec/changes/`.
- Numbering: monotonic four-digit sequence. Numbers are never reused.
- Immutability: an **accepted** ADR is frozen. To change a decision, add a new ADR whose Status is `accepted, supersedes ADR-NNNN` and whose `Supersedes:` field names the prior file. Do not edit the old file.
- In-force set: walk `Supersedes:` links. Only accepted ADRs that are not superseded constrain new designs.

Informal predecessors lived under `specification/decisions/`. Those files are now stubs that point here.

## In-force index

Walk `Supersedes:`. Only accepted ADRs that are **not** superseded constrain design.

| ID | File | Decision |
| --- | --- | --- |
| ADR-0001 | [0001-adopt-openspec-openspdd-and-madr.md](0001-adopt-openspec-openspdd-and-madr.md) | OpenSpec `spec-driven-with-adr` + OpenSPDD + MADR |
| ADR-0002 | [0002-canonical-github-repository-heavy-rental.md](0002-canonical-github-repository-heavy-rental.md) | Canonical repo is `Heavy-Rental/heavy-rental-mobile` |
| ADR-0003 | [0003-openapi-as-api-source.md](0003-openapi-as-api-source.md) | Client HTTP contract is `specification/api/heavyrental-openapi.yaml` |
| ADR-0004 | [0004-three-layer-mock-strategy.md](0004-three-layer-mock-strategy.md) | Seed + Mockoon/Prism + MockWebServer |
| ADR-0005 | [0005-mockoon-echoes-return-notes.md](0005-mockoon-echoes-return-notes.md) | Mockoon return PATCH echoes `returnNotes` |
| ADR-0006 | [0006-google-sign-in-credential-manager.md](0006-google-sign-in-credential-manager.md) | Credential Manager + server-verified ID token; first-time Google → `ROLE_DRIVER` |
| ADR-0007 | [0007-default-http-target-is-spring-boot.md](0007-default-http-target-is-spring-boot.md) | Default base URL is Spring `:8080`; mock is opt-in |
| ADR-0008 | [0008-client-decodes-jwt-roles-for-routing.md](0008-client-decodes-jwt-roles-for-routing.md) | Staff vs customer routing from JWT `roles` claim |

## Superseded (historical only)

None yet.

## New ADR

1. Take the next sequence number.
2. Copy the MADR-short sections from an existing file.
3. Set Status to `accepted` only after the design is implemented.
4. Reference the file from `openspec/changes/<change>/adr.md` (review manifest).
5. Update the in-force index in this README.
