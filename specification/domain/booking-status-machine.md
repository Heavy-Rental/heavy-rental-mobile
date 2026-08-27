# Domain: Booking status state machine

**Status:** Implemented (v1)  
**Code:** `data/models/BookingStatus.kt`; transitions enforced in `viewmodel/AppViewModel.kt`

---

## Status values

```text
PENDING_DEPOSIT → PENDING_CONFIRMED → CONFIRMED → MOBILISED → COMPLETED
                                                                        (CANCELLED: any state)
```

| Status | Meaning | Driven by this app? |
|--------|---------|---------------------|
| `PENDING_DEPOSIT` | Booking raised; deposit not yet paid | No — display only |
| `PENDING_CONFIRMED` | Deposit paid; awaiting confirmation | No — display only |
| `CONFIRMED` | Booking accepted; equipment not yet sent to site | Source state for mobilise |
| `MOBILISED` | Equipment delivered / on hire at site | Both (target, then source) |
| `COMPLETED` | Equipment returned; hire closed | Target of complete |
| `CANCELLED` | Booking cancelled | No — display only |

Enum names are **uppercase** strings on the wire.
Kotlin: `enum class BookingStatus { PENDING_DEPOSIT, PENDING_CONFIRMED, CONFIRMED, MOBILISED, COMPLETED, CANCELLED }`.
Wire serialisation uses `newStatus.name` in `BookingRepository`.

**Source of truth:** these six values mirror the backend `Booking.BookingStatus` enum
(Spring `entity-repository`) and the `BookingStatus` schema in
[`heavyrental-openapi.yaml`](../api/heavyrental-openapi.yaml). All three must be changed together.

### `null` is a seventh case

The domain field is `BookingStatus?`, not `BookingStatus`. `Mappers.kt` yields `null` when the wire
value is absent (`Booking.status` is a nullable column — Spring `entity-repository`) **or**
unrecognised, rather than throwing and failing the entire response over one row.

| Consumer | Behaviour on `null` |
|---|---|
| Seed derive (`toDeliveryItems` / `toReturnItems`) | Excluded — no equality match against `CONFIRMED`/`MOBILISED`/`COMPLETED` |
| UI sub-filter **All** | **Shown** — `All` returns the list unfiltered |
| UI sub-filters Confirmed / Mobilised / Completed | Excluded, same reason as the seed derive |
| Home dashboard totals (`deliveries.size`) | **Counted** |
| Home dashboard per-status counts | Not counted |
| Card status badge | Grey **"Unknown"** — see [03-deliveries.md](../product/03-deliveries.md) **K3** |
| Transition guards (below) | Rejected — `currentStatus == null` fails the precondition, so no API call and no local change |

**Consequence worth knowing:** because the total counts a `null`-status row and the per-status counts
don't, the dashboard total will not equal the sum of its parts whenever one is present (e.g. "5
deliveries" alongside "2 confirmed / 2 mobilised"). Not a defect in itself — it follows from
`null` being unclassifiable — but it is the visible symptom to look for.

Adding a value to the enum here therefore changes behaviour from "renders as Unknown, counts toward
the total, and matches no sub-filter" to "participates normally" — which is the intended way to
adopt a new backend status.

> **HR-78:** `PENDING_DEPOSIT`, `PENDING_CONFIRMED`, and `CANCELLED` were added to match the backend.
> Backend seed data contains bookings in all six states, so `GET /api/bookings` can return any of
> them — four of the six are display-only as far as this client is concerned.

---

## Allowed transitions

| From | To | Trigger (product) | API (v1 client) |
|------|-----|-------------------|-----------------|
| `CONFIRMED` | `MOBILISED` | Operator mobilises on **Deliveries** | `PATCH /api/deliveries/{bookingId}/status` |
| `MOBILISED` | `COMPLETED` | Operator completes on **Returns** | `PATCH /api/returns/{bookingId}/status` |

### Forbidden (v1)

- Skipping states (e.g. `CONFIRMED` → `COMPLETED`)
- Moving backwards (e.g. `MOBILISED` → `CONFIRMED`)
- Any transition not listed above

**Client rule:** if the requested `newStatus` is not exactly the single allowed target for that action, **do nothing** (no API call, no local change).

**Client rule:** if the booking’s **current** status is not the expected source status, **do nothing**.

**Client rule:** if no booking exists for the given `bookingId`, **do nothing**.

---

## Transition guards (formal)

### Mobilise (delivery)

```text
preconditions:
  - booking exists with bookingId
  - booking.bookingStatus == CONFIRMED
  - requested newStatus == MOBILISED
effect:
  - attempt API PATCH /api/deliveries/{bookingId}/status
  - booking.bookingStatus = MOBILISED locally on success or IOException (unreachable);
    NOT applied if the server responds with an HttpException (explicit rejection) — see
    resolved guards note below
  - refresh derived delivery/return lists
```

### Complete (return)

```text
preconditions:
  - booking exists with bookingId
  - booking.bookingStatus == MOBILISED
  - requested newStatus == COMPLETED
  - returnNotes: String — whatever is in the return note field, may be blank
effect:
  - attempt API PATCH /api/returns/{bookingId}/status with { bookingStatus, returnNotes }
  - booking.bookingStatus = COMPLETED locally on success or IOException (unreachable);
    NOT applied if the server responds with an HttpException (explicit rejection) — see
    resolved guards note below
  - booking.returnNotes = returnNotes (local, same conditions as the status change above)
  - refresh derived delivery/return lists
```

Shared implementation: `AppViewModel.updateBookingStatus(id, expectedCurrent, expectedNew, newStatus, returnNotes, apiCall)`.
`returnNotes` is `null` for the delivery path (`updateDeliveryStatus` never passes one) and is only
ever applied to the `_returns` list, never to deliveries or bookings.

### Client guards vs. server guards — RESOLVED (HR-93)

The "applied even on failure" clause above was deliberate v1 design, specified in
[05-offline-fallback.md](../product/05-offline-fallback.md) Principle 3. It was written when the
only backend was Mockoon, which returns `200` to any request and **cannot** reject a transition —
so the client's own preconditions were the only guard that existed, and applying locally was safe.

The Spring backend enforces the same two transitions server-side and returns `400` on anything else
(Spring `booking-delivery-return` FR-BDR-004/006). `ROLE_DRIVER` is **allowed** on these routes;
`403` is for `ROLE_USER` (or missing auth), not a blanket driver lock-out. Now that HR-78 points the
app at Spring, the guards above are no longer the only ones — the server's guard can now say no after
the client's guard already said yes.

**Decision (HR-93):** split by failure type. `AppViewModel.updateBookingStatus` now applies the
local transition only on success or on `IOException` (host genuinely unreachable); an
`HttpException` (server explicitly responded, e.g. `400`/`403`) or any other exception leaves the
booking's status unchanged. See [05-offline-fallback.md](../product/05-offline-fallback.md) O1 for
the full write-up, including the residual gap (O2) around distinguishing `400` from `403` in the
error copy shown to the operator.

---

## State diagram

```text
                 mobilise (Deliveries)
   ┌──────────┐ ──────────────────────► ┌───────────┐
   │ CONFIRMED │                         │ MOBILISED │
   └──────────┘                          └─────┬─────┘
                                               │
                                               │ complete (Returns)
                                               ▼
                                         ┌───────────┐
                                         │ COMPLETED │
                                         └───────────┘
```

---

## Relationship to lists

Status alone does not place a booking on a screen; **dates** also apply — for the two staff
lists, that is:

- Deliveries: today + (`CONFIRMED` | `MOBILISED`) — see [list-filters.md](list-filters.md)
- Returns: today + (`MOBILISED` | `COMPLETED`) — see [list-filters.md](list-filters.md)

A booking can appear on **both** lists only if `startDate` and `endDate` are both today and status is `MOBILISED` (edge case; mock data usually uses multi-day hires).

**The customer bookings list is the exception to "dates also apply."** It has no date
membership filter at all — every status, including the four display-only ones, is shown for
every booking `GET /api/bookings` scopes to that customer, filterable (not excluded) by a
status chip. See [list-filters.md](list-filters.md) "Customer booking list filter" and
[product/06-customer-bookings.md](../product/06-customer-bookings.md). This screen has no
transition logic of its own — it renders `bookingStatus` exactly as returned and drives none of
the transitions documented above.

---

## Request payload

Deliveries send only a status change:

```json
{
  "bookingStatus": "MOBILISED"
}
```

Schema: API `StatusUpdateRequest` in `specification/api/heavyrental-openapi.yaml`.  
DTO: `network/dto/BookingDtos.kt` — `StatusUpdateRequest`.

Returns send a status change **and** a return note (may be an empty string — there is no way to
omit the field, unlike `deliveryNotes` elsewhere, which is nullable):

```json
{
  "bookingStatus": "COMPLETED",
  "returnNotes": "Returned in good condition"
}
```

This is a **separate schema from deliveries**, not `StatusUpdateRequest` plus an extra field —
deliveries has no use for a notes field, so sharing the type would mean it silently accepts one it
ignores.  
Schema: API `ReturnStatusUpdateRequest` in `specification/api/heavyrental-openapi.yaml`.  
DTO: `network/dto/BookingDtos.kt` — `ReturnStatusUpdateRequestDto`.

---

## Test cases (domain)

| # | Current | Requested | Result |
|---|---------|-----------|--------|
| 1 | `CONFIRMED` | `MOBILISED` | Accept → `MOBILISED` |
| 2 | `CONFIRMED` | `COMPLETED` | Reject (no change) |
| 3 | `MOBILISED` | `COMPLETED` | Accept → `COMPLETED`, `returnNotes` applied |
| 4 | `MOBILISED` | `CONFIRMED` | Reject |
| 5 | `COMPLETED` | `MOBILISED` | Reject |
| 6 | missing id | any | Reject |
