# Domain: Booking status state machine

**Status:** Implemented (v1)  
**Code:** `data/models/BookingStatus.kt`; transitions enforced in `viewmodel/AppViewModel.kt`

---

## Status values

```text
CONFIRMED  →  MOBILISED  →  COMPLETED
```

| Status | Meaning |
|--------|---------|
| `CONFIRMED` | Booking accepted; equipment not yet sent to site |
| `MOBILISED` | Equipment delivered / on hire at project location |
| `COMPLETED` | Equipment returned; hire closed |

Enum names are **uppercase** strings on the wire (`"CONFIRMED"`, `"MOBILISED"`, `"COMPLETED"`).  
Kotlin: `enum class BookingStatus { CONFIRMED, MOBILISED, COMPLETED }`.  
Wire serialisation uses `newStatus.name` in `BookingRepository`.

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
  - booking.bookingStatus = MOBILISED (local, even if PATCH failed)
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
  - booking.bookingStatus = COMPLETED (local, even if PATCH failed)
  - refresh derived delivery/return lists
```

Shared implementation: `AppViewModel.updateBookingStatus(id, expectedCurrent, expectedNew, newStatus, apiCall)`.

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
