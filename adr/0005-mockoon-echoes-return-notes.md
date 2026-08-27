# ADR-0005: Mockoon echoes returnNotes on the return-status route

- Status: accepted
- Date: 2026-08-27
- Tags: mocks, returns

## Context

HR-100 added operator-entered `returnNotes` on `PATCH /api/returns/{bookingId}/status`. Mockoon otherwise serves a **static** FILE fixture, so completing a return against the mock always showed a canned note instead of what the operator typed.

Predecessor: informal `specification/decisions/003-mock-echoes-return-notes.md`.

## Decision

1. `mocks/.generated/return-item.json` uses Mockoon `{{body 'path' 'default'}}` for exactly two fields: `returnNotes` and `bookingStatus`.
2. Templating is written by `scripts/prepare-mocks.mjs`, not hand-edited. Prism and the OpenAPI bundled example keep a plain realistic value (templating is Mockoon-only).
3. Scope is **only** the return-status route. `PATCH /api/deliveries/{bookingId}/status` stays static.

## Consequences

- Manual QA against Mockoon can demonstrate the HR-100 note round-trip; `npm run mock:verify` can assert the echo.
- Mockoon still does not validate the body (missing `returnNotes` still returns `200` with `""`). Real validation is Spring.
- Prism does not echo; testers who need the round-trip MUST use Mockoon.
