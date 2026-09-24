# Review code

Review the code just written (or the current uncommitted diff if the user does not point at files). Do not rewrite the feature unless they ask; report findings.

Read these **rules** and apply the ones that match the files under review:

- `.cursor/rules/java-springboot.mdc` — Java 21, Spring Boot 3, packages, layers, exceptions, Lombok, naming
- `.cursor/rules/testing.mdc` — JUnit 5, Mockito, Testcontainers, `*Test.java`
- `.cursor/rules/api-standards.mdc` — REST paths, status codes, error body, pagination

Read the **spec** that owns the change (skip files that are clearly unrelated):

| If the change is about… | Spec |
|---|---|
| Behaviour / acceptance | `spec/requirements.md` |
| Layers, UI ↔ API, DB | `spec/architecture.md` |
| Entities, columns, enums, FKs | `spec/data-model.md` |
| Endpoints, payloads, HTTP codes | `spec/api-contract.md` |
| Status graph, `AppException`, `422` | `spec/state-machine.md` |
| Screens and navigation | `spec/ui-flow.md` |
| Unit vs integration, transition matrix | `spec/test-strategy.md` |

## Checklist

Mark each item **pass / fail / n/a**. Failures need file + what to change. Do not invent extra product requirements.

**Rules**

- [ ] Package and type names match `java-springboot.mdc` (`com.ticketing`, controller/service/repository/DTO)
- [ ] Controllers are HTTP-only; services own transactions and rules; repositories have no business logic
- [ ] Entities are not request/response bodies; DTOs/records on the wire
- [ ] Expected failures are `AppException` + `ErrorCode`; one `GlobalExceptionHandler`; no `javax.*`
- [ ] Lombok: no `@Data` on entities, no `@SneakyThrows`, no field `@Autowired`
- [ ] REST: `/api/v1`, plural resources, `PATCH` for fields, `POST …/status` for transitions, no verbs in paths
- [ ] Status codes and `{ code, message, details }` match `api-standards.mdc` (illegal transition = `422`, not `400`/`500`)
- [ ] List endpoints paginated; no unbounded arrays
- [ ] Tests: JUnit 5 + Mockito unit tests; integration uses Testcontainers PostgreSQL, not H2; AssertJ
- [ ] No secrets in code, tests, or config committed to the repo

**Specs (use the relevant rows)**

- [ ] Implements the requirements that this change claims to implement — nothing extra
- [ ] State changes only through the service allow-list; `PATCH` does not set `status`
- [ ] Every **valid** and **invalid** `(from, to)` pair has its own unit **and** integration case (`spec/test-strategy.md`, `spec/state-machine.md`)
- [ ] Invalid transition: no `save`, `INVALID_TRANSITION`, HTTP `422`
- [ ] Search (`q`) and status filter match the API contract
- [ ] UI, if touched: list / create / detail only; errors shown from `message` / `details`

## Output

1. **Summary** — what was reviewed
2. **Verdict** — ready / fix then re-review
3. **Findings** — severity (block / should-fix / nit), rule or spec reference, location
4. **Gaps** — missing tests or spec mismatches, especially any transition pair with no test
