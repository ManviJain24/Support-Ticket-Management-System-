# Test strategy

JUnit 5, Mockito, AssertJ. Integration tests use PostgreSQL (`@Tag("integration")`), never H2. Prefer Testcontainers (`postgres:16-alpine`) when the Docker daemon is usable. If Docker is not available (missing daemon or no `docker.sock` access), the same tests run against a dedicated local database `ticketing_test` — not the app database `ticketing`. Naming and style: `.cursor/rules/testing.mdc`. Graph under test: `spec/state-machine.md`.

Automated tests are **backend**. UI error display is checked against the API error body; it is not a substitute for these tests.

## Unit vs integration

| | Unit | Integration |
|---|---|---|
| **Where** | `TicketService` (and other services) with `@ExtendWith(MockitoExtension.class)` | `@SpringBootTest` + HTTP (`MockMvc` or TestRestTemplate) + real DB |
| **IO** | No Spring context, no database. `@Mock` repositories | One shared Postgres per class (Testcontainers, or `ticketing_test`) |
| **What** | Domain rules, allow-list, validation decisions, search/filter query construction | Persistence, `CHECK` constraints, REST status codes and error JSON, data still there after a new request |
| **What not** | Controllers, JPA, JSON | Re-testing service if/else that has no HTTP or DB effect |

**Unit — cover**

- Create: new ticket is `OPEN`; required fields.
- Update: title, description, priority, assignee; `status` on PATCH is rejected.
- Comments: append body; missing ticket → `TICKET_NOT_FOUND`.
- Search/filter: service passes `q` / `status` through (repository mocked).
- **Every status transition** (see below).

**Integration — cover**

- Create / list / get / PATCH / comment endpoints: `201`/`200`/`400`/`404` as in `spec/api-contract.md`.
- `GET /api/v1/tickets?q=` and `?status=` return the right rows from Postgres.
- Error body shape `{ code, message, details }` for validation and not-found.
- Restart is represented by a second HTTP call against the same DB (row still present).
- **Every status transition** through `POST /api/v1/tickets/{id}/status` (see below).

Do not use `@SpringBootTest` only to assert a service if/else. Do not mock the class under test.

## State machine: one test per transition

Five statuses. **Every directed pair `(from, to)` — including self-transitions — has its own test case.** That is 5 valid edges and 20 invalid edges (25 cases). Collapsing “all illegal from `CLOSED`” into a single unparameterized test is not enough.

Valid pairs (each is its own case):

- `OPEN` → `IN_PROGRESS`
- `OPEN` → `CANCELLED`
- `IN_PROGRESS` → `RESOLVED`
- `IN_PROGRESS` → `CANCELLED`
- `RESOLVED` → `CLOSED`

Invalid pairs (each is its own case), including but not limited to `CLOSED` → `OPEN`, `RESOLVED` → `OPEN`, `CANCELLED` → `OPEN`, skipped steps (`OPEN` → `RESOLVED`, `OPEN` → `CLOSED`, `IN_PROGRESS` → `CLOSED`), all self-transitions, and every other cell not in the allow-list.

`@ParameterizedTest` + `@CsvSource` / `@MethodSource` is allowed **only if each `(from, to)` is a separate invocation**. If `spec/state-machine.md` lists an edge (or forbids one) and there is no matching test case, add the test before changing production code.

### Unit (`TicketServiceTest`)

For **each valid** `(from, to)`:

- Ticket in DB mock is `from`; request `to`.
- Result status is `to`; `save` is called once.

For **each invalid** `(from, to)`:

- Throws `AppException` with `ErrorCode.INVALID_TRANSITION`.
- `save` is **not** called.

### Integration (state-machine)

For **each valid** `(from, to)`:

- Seed a ticket at `from`.
- `POST /api/v1/tickets/{id}/status` with `{ "status": "<to>" }` → **`200`**.
- Follow-up `GET` shows `to` in Postgres.

For **each invalid** `(from, to)`:

- Seed a ticket at `from`.
- Same `POST` → **`422`**, body `code` is `INVALID_TRANSITION`.
- Follow-up `GET` still shows `from` (row unchanged).

Acceptance criterion “state-machine integration tests pass” means this matrix is green, not a single happy-path sample.
