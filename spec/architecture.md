# Architecture

Small two-process app: a **Spring Boot 3** REST API (Java 21) and a **React / Next.js** UI. One relational database behind the API. No extra services.

## Runtime

```
Browser  →  Next.js (React)  →  HTTP JSON  →  Spring Boot  →  PostgreSQL
```

- **UI:** Next.js app. Pages for list, create, and ticket detail (view, edit fields, comments, status actions). Calls the backend; does not own ticket rules or persistence.
- **API:** Spring Boot, base path `/api/v1`. Single deployable. Enforces validation and the status state machine.
- **DB:** PostgreSQL via Spring Data JPA. Data survives process restart. Connection settings from environment variables — no secrets in the repo.

## Backend layers

Standard layering under `com.ticketing`. Requests flow controller → service → repository.

| Layer | Role |
|---|---|
| **Controller** | HTTP only: map requests/responses, bean-validation on DTOs, call one service method. |
| **Service** | Create/list/get/update, comments, keyword search, status filter, **status transitions**. Reject illegal transitions with `AppException`. `@Transactional` here. |
| **Repository** | Spring Data JPA. Tickets and comments. No business rules. |
| **Domain** | Entities and enums (`TicketStatus`, priority). |
| **DTO** | Request/response records. Entities are not returned to the client. |
| **Exception** | `GlobalExceptionHandler` → `{ code, message, details }` for 4xx/5xx. |

Invalid transitions (`CLOSED` → `OPEN`, `RESOLVED` → `OPEN`, `CANCELLED` → `OPEN`, and any other edge not in the allowed graph) are rejected in the service, not in the UI and not in the database as the primary check.

## Frontend ↔ API

The Next.js app uses `fetch` (or a thin client wrapper) against the Spring origin, e.g. `NEXT_PUBLIC_API_BASE_URL`.

- List: `GET /api/v1/tickets` with query params for keyword search and `status` filter.
- Detail: `GET /api/v1/tickets/{id}`.
- Create / field updates / comments / status changes: `POST` / `PATCH` / action sub-resources as in `spec/api-contract.md`.
- CORS allowed for the Next.js origin in local and deployed setups.
- On non-2xx, the UI reads `message` (and `details` for field errors) and shows them on the page. It does not invent a second error format.

The UI may hide illegal status actions, but the backend remains the authority; a direct API call with an invalid transition must still fail.

## Tests

Unit tests cover services (especially the state machine) without Spring. Integration tests hit the API with Testcontainers PostgreSQL so legal and illegal transitions are verified end-to-end.
