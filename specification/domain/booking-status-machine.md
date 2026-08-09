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
(`SPEC-entity-repository.md` §6.2) and the `BookingStatus` schema in
[`heavyrental-openapi.yaml`](../api/heavyrental-openapi.yaml). All three must be changed together.

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
  - booking.bookingStatus = MOBILISED (local, even if PATCH failed)  ← see open question below
  - refresh derived delivery/return lists
```

### Complete (return)

```text
preconditions:
  - booking exists with bookingId
  - booking.bookingStatus == MOBILISED
  - requested newStatus == COMPLETED
effect:
  - attempt API PATCH /api/returns/{bookingId}/status
  - booking.bookingStatus = COMPLETED (local, even if PATCH failed)  ← see open question below
  - refresh derived delivery/return lists
```

Shared implementation: `AppViewModel.updateBookingStatus(id, expectedCurrent, expectedNew, newStatus, apiCall)`.

### Open question — client guards vs. server guards (undecided)

The "even if PATCH failed" clause above is deliberate v1 design, specified in
[05-offline-fallback.md](../product/05-offline-fallback.md) Principle 3. It was written when the
only backend was Mockoon, which returns `200` to any request and **cannot** reject a transition —
so the client's own preconditions were the only guard that existed, and applying locally was safe.

The Spring backend enforces the same two transitions server-side and returns `400` on anything else
(`SPEC-booking-delivery-return-api.md` §4, Requirements 4.2/6). Now that HR-78 points the app at it,
the guards are duplicated and the failure mode has inverted: a rejected transition still ends in the
new status on screen.

Not resolved here. See [05-offline-fallback.md](../product/05-offline-fallback.md) — that file owns
the decision, this one just flags that the guards above are no longer the only ones.

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

Status alone does not place a booking on a screen; **dates** also apply:

- Deliveries: today + (`CONFIRMED` | `MOBILISED`) — see [list-filters.md](list-filters.md)
- Returns: today + (`MOBILISED` | `COMPLETED`) — see [list-filters.md](list-filters.md)

A booking can appear on **both** lists only if `startDate` and `endDate` are both today and status is `MOBILISED` (edge case; mock data usually uses multi-day hires).

---

## Request payload

Status updates send:

```json
{
  "bookingStatus": "MOBILISED"
}
```

or

```json
{
  "bookingStatus": "COMPLETED"
}
```

Schema: API `StatusUpdateRequest` in `specification/api/heavyrental-openapi.yaml`.  
DTO: `network/dto/BookingDtos.kt` — `StatusUpdateRequest`.

---

## Test cases (domain)

| # | Current | Requested | Result |
|---|---------|-----------|--------|
| 1 | `CONFIRMED` | `MOBILISED` | Accept → `MOBILISED` |
| 2 | `CONFIRMED` | `COMPLETED` | Reject (no change) |
| 3 | `MOBILISED` | `COMPLETED` | Accept → `COMPLETED` |
| 4 | `MOBILISED` | `CONFIRMED` | Reject |
| 5 | `COMPLETED` | `MOBILISED` | Reject |
| 6 | missing id | any | Reject |
