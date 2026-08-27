# Moved

This decision is now the durable ADR:

[`adr/0006-google-sign-in-credential-manager.md`](../../adr/0006-google-sign-in-credential-manager.md)

The informal version of this file incorrectly stated that first-time Google sign-in auto-provisions `ROLE_USER`. The as-built Spring contract (FR-AUTH-L-001b) and ADR-0006 provision **`ROLE_DRIVER`**. Do not restore `ROLE_USER` auto-provision from git history.

Do not edit this stub. Accepted ADRs are immutable; supersede with a new `adr/NNNN-*.md`.
