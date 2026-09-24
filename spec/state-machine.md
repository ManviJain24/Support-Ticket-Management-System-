# State machine

Ticket `status` is a closed set: `OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`, `CANCELLED`. New tickets are created as `OPEN`. The graph below is the **only** allowed movement. The database `CHECK` constraint only limits stored values; it does **not** enforce edges.

## Allowed transitions

```
OPEN → IN_PROGRESS → RESOLVED → CLOSED
OPEN → CANCELLED
IN_PROGRESS → CANCELLED
```

| From | To | Valid |
|---|---|---|
| `OPEN` | `IN_PROGRESS` | yes |
| `OPEN` | `CANCELLED` | yes |
| `IN_PROGRESS` | `RESOLVED` | yes |
| `IN_PROGRESS` | `CANCELLED` | yes |
| `RESOLVED` | `CLOSED` | yes |

There are no other edges. There are no self-transitions (`OPEN` → `OPEN` is invalid). `CLOSED` and `CANCELLED` are terminal: nothing leaves them.

## Invalid (all remaining edges)

Everything not in the table is rejected. Explicitly invalid examples:

| From | To | Valid |
|---|---|---|
| `CLOSED` | `OPEN` | **no** |
| `RESOLVED` | `OPEN` | **no** |
| `CANCELLED` | `OPEN` | **no** |
| `CLOSED` | `IN_PROGRESS` / `RESOLVED` / `CANCELLED` / `CLOSED` | **no** |
| `CANCELLED` | any status | **no** |
| `RESOLVED` | `IN_PROGRESS` / `CANCELLED` / `RESOLVED` | **no** |
| `OPEN` | `RESOLVED` / `CLOSED` / `OPEN` | **no** |
| `IN_PROGRESS` | `OPEN` / `CLOSED` / `IN_PROGRESS` | **no** |

Skipping steps (`OPEN` → `RESOLVED`, `OPEN` → `CLOSED`, `IN_PROGRESS` → `CLOSED`) is invalid. Reopening (`*` → `OPEN` except create) is invalid.

## Service-layer enforcement

Status changes go through `TicketService` only, via the API `POST /api/v1/tickets/{id}/status` with body `{ "status": "<target>" }`. `PATCH` on the ticket **must not** accept or apply `status`.

On a transition request the service:

1. Loads the ticket (missing id → `AppException` / `TICKET_NOT_FOUND` → `404`).
2. Reads current `status` and the requested target.
3. Looks up the pair in an allow-list (enum map or equivalent: `Map<TicketStatus, Set<TicketStatus>>`). **Allow-list only** — do not encode “what is forbidden”.
4. If the pair is allowed: set status, persist, return the updated ticket.
5. If the pair is not allowed: **do not** call `save`. Throw immediately.

The allow-list is the single source of truth in code and must match this document. Controllers do not branch on status. Repositories do not encode the graph.

## Invalid transition: exception and HTTP

```java
throw new AppException(ErrorCode.INVALID_TRANSITION,
    "Cannot transition ticket from " + from + " to " + to);
```

`GlobalExceptionHandler` maps `ErrorCode.INVALID_TRANSITION` to **`422 Unprocessable Entity`**.

**Response body**

```json
{
  "code": "INVALID_TRANSITION",
  "message": "Cannot transition ticket from CLOSED to OPEN"
}
```

No `details` unless we later attach field errors; the pair itself is the rule violation, not bean validation (`400` is only for missing/unknown `status` on the request). Do not use `400`, `409`, or `500` for a well-formed but illegal edge such as `CLOSED` → `OPEN`.
