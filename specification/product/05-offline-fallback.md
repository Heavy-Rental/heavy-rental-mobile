# Feature index: Offline / API failure fallback

**Status:** Implemented (v1)  
**Canonical Spec Kit spec:** [`specs/005-offline-fallback/spec.md`](../../specs/005-offline-fallback/spec.md)

After authentication, list and status API failures keep the app usable: seed/previous lists, visible errors, optimistic status updates. Login itself is never offline.

## Related

| Resource | Path |
|----------|------|
| Spec Kit (canonical) | [`specs/005-offline-fallback/spec.md`](../../specs/005-offline-fallback/spec.md) |
| Login | [`specs/001-admin-login/spec.md`](../../specs/001-admin-login/spec.md) |
| API toggle | [`specs/084-api-endpoint-toggle/spec.md`](../../specs/084-api-endpoint-toggle/spec.md) |
