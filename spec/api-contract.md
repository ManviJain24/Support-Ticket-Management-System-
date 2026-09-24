# API contract

Base path `/api/v1`. JSON. Ticket and comment ids are UUIDs. Error body and pagination match `.cursor/rules/api-standards.mdc`. Status values and transitions: `spec/state-machine.md`.

`PATCH` updates title, description, priority, and assignee only. Status changes use `POST /api/v1/tickets/{id}/status`.

## Shared shapes

### Ticket (list item)

```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "title": "Cannot reset password",
  "description": "Reset email never arrives.",
  "status": "OPEN",
  "priority": "HIGH",
  "assignee": "Alex Chen",
  "createdAt": "2026-09-24T05:00:00Z",
  "updatedAt": "2026-09-24T05:00:00Z"
}
```

`assignee` may be `null`. `status`: `OPEN` | `IN_PROGRESS` | `RESOLVED` | `CLOSED` | `CANCELLED`. `priority`: `LOW` | `MEDIUM` | `HIGH`.

### Ticket (detail)

List item plus `comments` (oldest first):

```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "title": "Cannot reset password",
  "description": "Reset email never arrives.",
  "status": "OPEN",
  "priority": "HIGH",
  "assignee": "Alex Chen",
  "createdAt": "2026-09-24T05:00:00Z",
  "updatedAt": "2026-09-24T06:10:00Z",
  "comments": [
    {
      "id": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
      "body": "Asked the user for screenshots.",
      "createdAt": "2026-09-24T06:10:00Z"
    }
  ]
}
```

### Error

```json
{
  "code": "TICKET_NOT_FOUND",
  "message": "Ticket 3fa85f64-5717-4562-b3fc-2c963f66afa6 not found",
  "details": [{ "field": "title", "message": "must not be blank" }]
}
```

`details` omitted or `[]` when there are no field errors.

---

## `POST /api/v1/tickets`

Create a ticket. Server sets `status` to `OPEN`. Do not send `status` or `id`.

**Request**

```json
{
  "title": "Cannot reset password",
  "description": "Reset email never arrives.",
  "priority": "HIGH",
  "assignee": "Alex Chen"
}
```

`title` required, 1–200 chars. `description` required, non-blank. `priority` required. `assignee` optional (omit or `null`).

| Status | When |
|---|---|
| `201 Created` | Body: ticket detail (`comments`: `[]`). Header `Location: /api/v1/tickets/{id}` |
| `400 Bad Request` | Missing/invalid fields (`VALIDATION_FAILED`) |

---

## `GET /api/v1/tickets`

List tickets. Keyword search and status filter are query params on this endpoint (combined with AND when both are present).

| Query | Meaning |
|---|---|
| `q` | Optional. Case-insensitive keyword match on `title` and `description` |
| `status` | Optional. Exact `TicketStatus`. Unknown value → `400` |
| `page` | 0-based, default `0` |
| `size` | Default `20`, max `100` (`size > 100` → `400`) |
| `sort` | Optional. Default `createdAt,desc` |

**Response `200`**

```json
{
  "content": [ { "id": "...", "title": "...", "description": "...", "status": "OPEN", "priority": "HIGH", "assignee": null, "createdAt": "...", "updatedAt": "..." } ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

List items **omit** `comments`. Empty result: `200` with `content: []`, not `404`.

| Status | When |
|---|---|
| `200 OK` | Page of tickets |
| `400 Bad Request` | Invalid `status`, `page`, or `size` |

---

## `GET /api/v1/tickets/{id}`

**Response `200`:** ticket detail including `comments`.

| Status | When |
|---|---|
| `200 OK` | Ticket found |
| `404 Not Found` | Unknown id (`TICKET_NOT_FOUND`) |

---

## `PATCH /api/v1/tickets/{id}`

Partial update of mutable fields. Omitted fields are left unchanged. **Must not include `status`.**

**Request** (any subset)

```json
{
  "title": "Cannot reset password (prod)",
  "description": "Still failing on prod SMTP.",
  "priority": "MEDIUM",
  "assignee": "Sam Lee"
}
```

To clear assignee, send `"assignee": null`. Same validation as create for any field that is present.

| Status | When |
|---|---|
| `200 OK` | Body: ticket detail |
| `400 Bad Request` | Invalid payload, or `status` present (`VALIDATION_FAILED`) |
| `404 Not Found` | Unknown id |

---

## `POST /api/v1/tickets/{id}/comments`

Add a comment. Append-only.

**Request**

```json
{ "body": "Asked the user for screenshots." }
```

`body` required, non-blank.

| Status | When |
|---|---|
| `201 Created` | Body: the new comment. `Location: /api/v1/tickets/{id}/comments/{commentId}` |
| `400 Bad Request` | Blank/missing `body` |
| `404 Not Found` | Unknown ticket id |

---

## `POST /api/v1/tickets/{id}/status`

Move the ticket to a new status. Backend enforces the graph; the UI must still expect `422` on illegal edges.

**Request**

```json
{ "status": "IN_PROGRESS" }
```

`status` required, one of the five enum values.

| Status | When |
|---|---|
| `200 OK` | Body: ticket detail after transition |
| `400 Bad Request` | Missing/unknown `status` |
| `404 Not Found` | Unknown ticket id |
| `422 Unprocessable Entity` | Illegal transition (`INVALID_TRANSITION`), e.g. `CLOSED` → `OPEN` |

Same status as current: `422` (`INVALID_TRANSITION`). There is no self-transition in the graph.
