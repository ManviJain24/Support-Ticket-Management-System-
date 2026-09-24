# Support Ticket Management System

Small support-ticket app: a **Spring Boot 3** REST API (Java 21) and a **Next.js** UI against **PostgreSQL**. Agents create tickets, search and filter them, update fields and assignee, add comments, and move status through a backend-enforced state machine. There is no login.

```
Browser  →  Next.js (port 3000)  →  HTTP JSON  →  Spring Boot (port 8080)  →  PostgreSQL
```

Specs live in [`spec/`](spec/). Implementation checklist is [`tasks.md`](tasks.md). Acceptance evidence is [`spec/acceptance-status.md`](spec/acceptance-status.md).

## Features

- Create, list, and view tickets
- Update title, description, priority, and assignee
- Add comments (append-only)
- Search by keyword (title or description) and filter by status
- Status flow: `OPEN → IN_PROGRESS → RESOLVED → CLOSED`, plus `OPEN`/`IN_PROGRESS → CANCELLED`
- Invalid transitions (including `CLOSED → OPEN`) return **422** `INVALID_TRANSITION`
- Input validation and `{ code, message, details }` API errors shown in the UI
- Data stored in PostgreSQL (survives API restart)

### Screens

| Path | Screen |
|------|--------|
| `/` | List, search, status filter |
| `/tickets/new` | Create ticket |
| `/tickets/[id]` | Detail: edit fields, comments, allowed status actions |

## Stack

| Piece | Choice |
|-------|--------|
| API | Java 21, Spring Boot 3.4, Maven, Flyway, Spring Data JPA |
| UI | Next.js 16, React 19, Node ≥ 20.9 |
| Database | PostgreSQL 16 |
| Tests | JUnit 5, Mockito, Testcontainers (or local `ticketing_test`), Vitest |

Do not commit secrets. Connection settings come from environment variables. `.env` is gitignored; [`.env.example`](.env.example) is placeholders only.

## Run locally

### Prerequisites

- JDK 21
- Maven 3.9+
- PostgreSQL (local instance on `localhost:5432`)
- Node.js **≥ 20.9** (Next 16 will not start on Node 18)

### 1. Database

Create the app database:

```sql
CREATE DATABASE ticketing;
```

For integration tests without Docker, also create:

```sql
CREATE DATABASE ticketing_test;
```

### 2. Environment

From the repo root:

```bash
cp .env.example .env
```

Edit `.env` with your local Postgres user and password (never commit this file):

```
DATABASE_URL=jdbc:postgresql://localhost:5432/ticketing
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=replace-with-local-password
FRONTEND_ORIGIN=http://localhost:3000
```

Spring Boot loads `optional:file:.env`. You can also export the same variables in the shell.

### 3. API (port 8080)

```bash
mvn spring-boot:run
```

API base path: `http://localhost:8080/api/v1`.

### 4. UI (port 3000)

```bash
cd frontend
npm install
export NEXT_PUBLIC_API_BASE_URL=http://localhost:8080/api/v1   # optional; this is the default
npm run dev
```

Open [http://localhost:3000](http://localhost:3000).

### Tests

```bash
# API: unit + PostgreSQL integration tests
mvn test

# UI: API client error parsing
cd frontend && npm test
```

If Docker is available, integration tests use Testcontainers (`postgres:16-alpine`). If the Docker socket is not usable, they use the local `ticketing_test` database instead of H2.

## Tasks completed

All items in [`tasks.md`](tasks.md) are done:

| Task | What shipped |
|------|----------------|
| **T1** | Spring Boot 3 skeleton, env-based DB config, CORS for local Next |
| **T2** | Shared errors: `AppException`, `ErrorCode`, `{ code, message, details }` |
| **T3** | Ticket/Comment entities, STRING enums, Flyway + CHECKs |
| **T4–T9** | Create, get, paginated list, PATCH fields, comments, search and status filter |
| **T10** | Status allow-list; 25 unit cases (5 valid, 20 invalid) |
| **T11** | HTTP state-machine matrix (25 pairs, 200 / 422) |
| **T12** | Next.js shell and API client |
| **T13–T17** | List, create, and detail (edit, comments, status actions) |
| **T18** | Persistence across a new API process (same Postgres) |
| **T19** | Spec/secrets check: `.env` not in git |

## Project layout

```
spec/           Requirements, API contract, state machine, UI flow
src/main/java   Spring Boot API (`com.ticketing`)
src/test/java   JUnit tests
frontend/       Next.js UI
.env.example    Env var names (no real passwords)
```

