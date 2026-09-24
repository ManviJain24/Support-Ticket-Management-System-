# Tasks

Implement in order. Each task is done when its **Test** passes on its own (earlier tasks staying green). Specs: `spec/*.md`. Conventions: `.cursor/rules/*.mdc`. No secrets in the repo.

## Implementation status

- [x] T1–T2 implemented; app smoke check and MVC error tests pass.
- [x] T3–T9 implemented; PostgreSQL integration tests pass (local `ticketing_test` because Docker socket is not accessible).
- [x] T10 implemented; all 25 transition unit cases pass.
- [x] T11 implemented; all 25 HTTP transition cases pass on PostgreSQL.
- [x] T12 implemented; API error parsing unit tests pass.
- [x] T13–T17 implemented; frontend tests pass. Screens load; API-backed list/create/detail flows verified 2026-09-24 (see `spec/acceptance-status.md`). Headed browser clicks remain optional.
- [x] T18 restart-persistence check: ticket created on `:8080` was returned by a new Spring Boot process on `:18081` against the same Postgres.
- [x] T19 secrets check: git repo initialized; `.env` is gitignored and not in `git ls-files`. No commit has been made. Docker-backed Testcontainers remains preferred when the daemon is usable.

---

## Backend foundation

### T1. Spring Boot 3 app skeleton (Java 21)

Package `com.ticketing`. Gradle/Maven, `application.yml` with DB URL/user/password from **environment variables** only. CORS for the Next.js origin. Empty `/api/v1` health or ticket controller stub is enough.

**Test:** App starts with env vars set. Repo has no passwords, `.env` is gitignored.

### T2. Shared exceptions and error JSON

`AppException`, `ErrorCode` (`VALIDATION_FAILED`, `TICKET_NOT_FOUND`, `INVALID_TRANSITION`, …), `GlobalExceptionHandler` → `{ code, message, details }` with `400` / `404` / `422` mappings. Bean validation → `400` + field `details`.

**Test:** `@WebMvcTest` (or a tiny controller) that throws each mapped exception; assert status + body shape (`spec/api-contract.md` Error).

### T3. Domain + schema

Entities `Ticket`, `Comment`; enums `TicketStatus`, `Priority` as **STRING**. Flyway (or equivalent) tables, FKs, cascade, CHECKs, status index (`spec/data-model.md`). Repositories only.

**Test:** `@DataJpaTest` + Testcontainers PostgreSQL: persist ticket+comment; invalid `status` string fails CHECK.

---

## Ticket HTTP API

### T4. Create ticket

`POST /api/v1/tickets`. Server sets `OPEN`. DTOs + validation (title 1–200, description non-blank, priority required, assignee optional). `201` + `Location` + detail with `comments: []`.

**Test:** Unit: service creates `OPEN`. Integration: `201` happy path; `400` missing title.

### T5. Get ticket

`GET /api/v1/tickets/{id}` returns detail including comments (oldest first). Unknown id → `404` `TICKET_NOT_FOUND`.

**Test:** Integration: create then GET `200`; random UUID → `404` + error body.

### T6. List tickets (paginated)

`GET /api/v1/tickets?page&size&sort`. Default `page=0`, `size=20`, `sort=createdAt,desc`. Envelope `{ content, page, size, totalElements, totalPages }`. List items omit comments. Empty list `200` + `[]`. `size > 100` → `400`.

**Test:** Integration: create two tickets; list `200` without `comments`; `size=101` → `400`.

### T7. Update ticket fields

`PATCH /api/v1/tickets/{id}` for title, description, priority, assignee (including `assignee: null`). Omit = leave unchanged. **Reject `status` in body** (`400` `VALIDATION_FAILED`). Unknown id → `404`.

**Test:** Unit + integration: change assignee; omit description stays; body with `status` → `400`; unknown id → `404`.

### T8. Add comment

`POST /api/v1/tickets/{id}/comments`. `201` + `Location`. Blank `body` → `400`. Unknown ticket → `404`. Append-only.

**Test:** Unit: missing ticket no save. Integration: comment on GET detail; blank body `400`.

### T9. Search and status filter

`GET /api/v1/tickets?q=&status=` (AND). `q` = case-insensitive match on title **or** description. Unknown `status` → `400`.

**Test:** Integration: seed tickets; `q` hits title/description; `status=OPEN` filters; both combined; bad status → `400`.

---

## State machine

Allow-list in **service only**. Endpoint `POST /api/v1/tickets/{id}/status`. See `spec/state-machine.md`, `spec/test-strategy.md`.

### T10. Transition service (unit matrix)

`Map<TicketStatus, Set<TicketStatus>>` for the five valid edges. Valid: set status, `save` once. Invalid (including self): `AppException(INVALID_TRANSITION)`, **no** `save`. Unknown ticket: `TICKET_NOT_FOUND`.

**Test:** `TicketServiceTest` — **25 parameterized cases**, one invocation per `(from, to)` (5 valid, 20 invalid). Plus not-found.

### T11. Transition HTTP (integration matrix)

Wire controller → service. Valid → `200` + GET shows `to`. Invalid → **`422`** `{ "code": "INVALID_TRANSITION" }` + GET still `from`. Missing/unknown enum in body → `400`. Unknown id → `404`.

**Test:** `@SpringBootTest` + Testcontainers — **25 cases**. This is acceptance criterion “state-machine integration tests pass”.

---

## Frontend

Next.js. `NEXT_PUBLIC_API_BASE_URL`. Thin fetch wrapper. No second error format (`spec/ui-flow.md`).

### T12. App shell + API client

Create Next.js app, three routes stubbed (`/`, `/tickets/new`, `/tickets/[id]`). Client: JSON, non-2xx parse `{ message, details }`.

**Test:** Manual or a small client unit test: fake `400` body surfaces `message` + `details`. App runs against a mock/base URL.

### T13. List screen

`/` : table (title, status, priority, assignee, updated). Search `q`, status filter including All, query string preserved. Empty: “No tickets”. Load error: `message`. Row → detail. **New ticket** → create.

**Test:** With API up: create via T4, reload list; search; filter; empty state; kill API → error message.

### T14. Create screen

`/tickets/new`: title, description, priority, optional assignee (no status). Success `201` → detail. Cancel → list. `400` field errors stay on form.

**Test:** Submit valid ticket → lands on detail. Submit blank title → inline errors, still on form.

### T15. Detail: view + edit fields

Load `GET`. `404` + link home. Edit title/description/priority/assignee, **Save** `PATCH`. Validation errors on page. Back to list keeps `?q=&status=` if stored.

**Test:** Open ticket, change assignee, save, refresh still new assignee. Bad PATCH shows API errors.

### T16. Detail: comments

Thread + add box. `POST` comment then append. Blank `400` at the box.

**Test:** Add comment, it appears; refresh still there. Empty submit shows error.

### T17. Detail: status actions

Show current status. Buttons only for **allowed** next states. `POST …/status`. Success updates badge. `422` shows `message`, badge unchanged.

**Test:** `OPEN` → In progress and Cancel only. Walk `OPEN → IN_PROGRESS → RESOLVED → CLOSED`. If you force a hidden action, UI still shows `422`.

---

## Close-out

### T18. Restart persistence

**Test:** Create ticket (+ comment). Restart **only** Spring Boot (same Postgres). List/detail still show the data.

### T19. Spec + secrets pass

Run `@commands/review-spec.md` against `spec/requirements.md` (and `api-contract` / `state-machine` / `test-strategy` if anything moved). Grep: no passwords, tokens, or `.env` committed.

**Test:** Review verdict “satisfies”. `git ls-files` has no secret files. All T10/T11 cases green.

---

## Suggested order

`T1 → T2 → T3 → T4 → T5 → T6 → T7 → T8 → T9 → T10 → T11 → T12 → T13 → T14 → T15 → T16 → T17 → T18 → T19`

T10 can start as soon as T3+T4 exist (service + ticket aggregate). T11 needs T5 (GET after POST). UI starts after T9 (list/search) and is richer after T11 (status).
