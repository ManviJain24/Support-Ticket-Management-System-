# UI flow

Next.js. Three screens. No login. Errors from the API (`message`, `details`) are shown on the screen that issued the request.

```
List  ←→  Create form
  │
  └──→  Ticket detail (view / edit fields / comments / status)
           └── Back to list
```

## Screens

### 1. Ticket list — `/`

Default landing page.

- Table or list of tickets: title, status, priority, assignee, updated time.
- **Search:** keyword field; submits `GET /api/v1/tickets?q=…` (keeps current status filter).
- **Filter:** status dropdown including “All”; submits `GET /api/v1/tickets?status=…` (keeps current `q`).
- Empty list: a short “No tickets” message, not an error.
- Failed load: show `message`.
- Row click → ticket detail.
- Primary action **New ticket** → create form.

Search and filter may live in the URL query (`?q=&status=`) so refresh keeps the same view.

### 2. Create form — `/tickets/new`

- Fields: title, description, priority, assignee (optional). Status is not shown; the server sets `OPEN`.
- **Create** → `POST /api/v1/tickets`. On `201`, go to that ticket’s detail.
- **Cancel** → list.
- `400`: show `message` and field `details` next to the inputs. Stay on the form.

### 3. Ticket detail — `/tickets/[id]`

One page for view, edit, comments, and status. Load with `GET /api/v1/tickets/{id}`. `404`: show `message` and a link back to the list.

**Fields.** Title, description, priority, assignee are editable in place (or a short edit section on the same page). **Save** → `PATCH /api/v1/tickets/{id}`. Success refreshes the ticket. Validation errors stay on the page.

**Status.** Show current status and only the next allowed actions from `spec/state-machine.md` (e.g. Open: In progress / Cancel). Choosing one → `POST /api/v1/tickets/{id}/status`. Success updates the badge. If the backend returns `422 INVALID_TRANSITION`, show `message`; do not change the displayed status. The UI may hide illegal actions; the API still rejects them.

**Comments.** Thread under the fields (oldest first) plus a box to add one. **Add** → `POST /api/v1/tickets/{id}/comments`, then append the new comment. Blank-body `400` shown at the box. No separate comments route.

**Back to list** returns to `/` (preserve list query string if it was stored).

## Movement (happy path)

1. Open app → list.
2. Optionally type a keyword and/or pick a status → list reloads.
3. **New ticket** → fill form → **Create** → land on detail.
4. On detail: change title/assignee/priority → **Save**; add a comment; advance or cancel status.
5. **Back** → list, where the new ticket appears (including after restart).

There is no other navigation. Comments and status never leave the detail page.
