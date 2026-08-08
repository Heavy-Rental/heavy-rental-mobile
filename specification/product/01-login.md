# Feature index: Admin login

**Status:** Implemented (v1)  
**Canonical Spec Kit spec:** [`specs/001-admin-login/spec.md`](../../specs/001-admin-login/spec.md)

Operators authenticate via interim → access JWT before using Home, Deliveries, or Returns. Tokens are in-memory only in v1.

## Related

| Resource | Path |
|----------|------|
| Spec Kit (canonical) | [`specs/001-admin-login/spec.md`](../../specs/001-admin-login/spec.md) |
| API endpoint config (properties) | [`specs/084-api-endpoint-toggle/spec.md`](../../specs/084-api-endpoint-toggle/spec.md), `app/api.properties` |
| Offline lists (not login) | [`specs/005-offline-fallback/spec.md`](../../specs/005-offline-fallback/spec.md) |
| OpenAPI Auth | [`../api/heavyrental-openapi.yaml`](../api/heavyrental-openapi.yaml) |
| Testing | [`../testing-guide.md`](../testing-guide.md) |
