# Generate tests

Generate backend tests for the current code (or the types/files the user names). Follow **`spec/test-strategy.md`** and **`.cursor/rules/testing.mdc`**. Also read **`spec/state-machine.md`** and **`spec/api-contract.md`** so assertions match the real graph and HTTP contract.

Do not change production code unless a test cannot compile without a seam the spec already requires. Do not add H2. Do not add `*IT.java`. Do not write frontend tests unless the user asks.

If `testing.mdc` examples use `TicketAction` / `ASSIGN` / `RESOLVE`, **ignore those names**. Transitions are `(from, to)` status pairs via `POST /api/v1/tickets/{id}/status`, as in the spec.

## Before writing

1. Read existing `*Test.java` and match package, imports, and assertion style.
2. List behaviours in the code under test that `test-strategy.md` says must be covered.
3. For the state machine, enumerate all **25** directed pairs (5 valid, 20 invalid, including self-transitions). Skip a pair only if a test case for that exact `(from, to)` already exists.

## What to generate

**Unit** — `{Class}Test`, `@ExtendWith(MockitoExtension.class)`, `@Mock` repositories, `@InjectMocks` service. No Spring context.

- Create → status `OPEN`
- PATCH fields; reject `status` on update
- Add comment; unknown ticket → `TICKET_NOT_FOUND`
- Search (`q`) and status filter delegated to the repository
- State machine on `TicketService`:
  - **Each valid** `(from, to)`: result is `to`; `save` once
  - **Each invalid** `(from, to)`: `AppException` / `INVALID_TRANSITION`; **never** `save`

Use `@ParameterizedTest` + `@MethodSource` / `@CsvSource` so **each pair is its own invocation**. One method that loops internally over statuses is not acceptable.

**Integration** — `*Test`, `@Tag("integration")`, `@SpringBootTest`, static `PostgreSQLContainer` + `@DynamicPropertySource`. Hit HTTP.

- Create / list / get / PATCH / comments: status codes from `api-contract.md`
- `GET /api/v1/tickets?q=` and `?status=`
- Error JSON `{ code, message, details }`
- Second GET after write (row still there)
- State machine:
  - **Each valid** pair: seed `from`, `POST …/status` → `200`, GET shows `to`
  - **Each invalid** pair: `422` + `INVALID_TRANSITION`, GET still `from`

Prefer `@SpringBootTest` for the transition matrix (HTTP + DB). Use `@WebMvcTest` only for controller-only cases with a mocked service — not as a replacement for the 25 integration transition cases.

## Style

- Class `{ClassUnderTest}Test`; methods `should{Expected}_when{Condition}`
- AssertJ (`assertThat`, `assertThatThrownBy`)
- No `assertEquals`, no expected-exception annotations
- Deterministic (no `sleep`, no wall clock); do not mock the class under test
- Do not test Lombok getters or framework wiring

## After generating

Print a matrix of the 25 `(from, to)` pairs and whether unit and integration cases exist. Call out any pair still missing.
