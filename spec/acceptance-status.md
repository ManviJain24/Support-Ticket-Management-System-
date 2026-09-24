# Acceptance status

Maps `spec/requirements.md` acceptance criteria to the last verification on **2026-09-24**. This file is the live checklist; the numbered criteria themselves stay in `requirements.md`.

Docker: client is installed, but this user cannot talk to `unix:///var/run/docker.sock` (socket is `root:docker`). Integration tests therefore ran against a dedicated local database `ticketing_test`, not H2 and not the app database `ticketing`. See `spec/test-strategy.md` and `PostgresIntegrationTest`.

## Functional requirements

| FR | Result |
|----|--------|
| 1–7 Create, list, view, update, comments, search, filter | Pass — API 2026-09-24 |
| 8 Survive restart | Pass — new JVM on `:18081` read data written by `:8080` |
| 9 Backend validation | Pass — `400 VALIDATION_FAILED` on blank create |
| 10 UI errors | Pass — screens render; client unit tests parse `message`/`details`; forms show `role="alert"` and field errors |
| 11–12 State machine | Pass — unit + HTTP 25-pair matrix green |

## Acceptance criteria

| AC | Criterion | Result | Evidence |
|----|-----------|--------|----------|
| 1 | Create from UI | Pass (API + create page) | `POST /api/v1/tickets` **201**. `GET /tickets/new` **200**. No headed-browser submit. |
| 2 | List tickets | Pass | `GET /api/v1/tickets?size=20` **200**. `GET /` **200**. |
| 3 | View details | Pass | `GET /api/v1/tickets/{id}` **200**. Detail route **200**. |
| 4 | Update fields | Pass | `PATCH` title/description/priority **200**. |
| 5 | Change assignee | Pass | `PATCH {"assignee":"Sam Lee"}` **200**; still `Sam Lee` after second process. |
| 6 | Add comments | Pass | `POST …/comments` **201**; detail from second process had **1** comment. |
| 7 | Search | Pass | `GET /tickets?q=AC-verify` returned the created id. |
| 8 | Status filter | Pass | `GET /tickets?status=OPEN` **200**. |
| 9 | Valid transitions | Pass | `OPEN → IN_PROGRESS` **200**. T11: all 5 valid HTTP pairs green. |
| 10 | Invalid rejected | Pass | `IN_PROGRESS → OPEN` and skip to `CLOSED` **422**. T11: all 20 invalid HTTP pairs green. |
| 11 | Survive restart | Pass | Ticket `86980a3b-4999-47c8-9dd2-9f83d295937b` created on `:8080`; `GET` on a **new** Spring Boot process `:18081` (same Postgres) returned title, assignee, comment, `IN_PROGRESS`. `:8080` left running. |
| 12 | Backend validation | Pass | Blank create **400**. MVC + API integration tests green. |
| 13 | UI meaningful errors | Pass | Vitest: 400 body surfaces `message` + `details`. Create/detail/list render `role="alert"` / `.field-error`. |
| 14 | State-machine integration tests | Pass | `mvn test` green, including `TicketStateMachineIntegrationTest` (25 pairs) on `ticketing_test`. |
| 15 | No secrets committed | Pass (index) | `git init` on `cursor/acceptance-closeout`. `.env` matches `.gitignore:11:.env`. `git ls-files` has no `.env`. `application.yml` uses `${DATABASE_PASSWORD}` only. **No commit yet** — nothing is on a remote. |

## Remaining optional work

- Join the `docker` group (or run a rootless engine) so Testcontainers can use `postgres:16-alpine` instead of `ticketing_test`.
- Click through create/save/comment/status in a real browser if you want headed confirmation of AC 1–8 and 13.
- First `git commit` when you want history on disk; keep `.env` untracked.
