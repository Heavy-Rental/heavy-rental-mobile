# Changelog

## Unreleased

### Added

- OpenSpec (`spec-driven-with-adr`), OpenSPDD REASONS canvases, and MADR-short ADRs (`adr/0001`–`0008`) as the documentation stack (HR-242).
- Root README describing the Android ops app, documentation layers, and GitHub Flow callers.
- Project setup guide (`specification/setup-guide.md`) for clone → Android Studio → emulator → Spring or mock → login.

### Changed

- Product/domain/API docs aligned with Spring OpenSpec: Google first-time provision is `ROLE_DRIVER`; drivers may call bookings/deliveries/returns; list payloads use `items[]`; booking routes exist on Spring `develop`.
- Informal `specification/decisions/` files are stubs pointing at `adr/`.

### Notes

- `BLANK_README.md` and `.github/workflows/android-ci.yml` remain as template/legacy leftovers and are not the source of truth.
