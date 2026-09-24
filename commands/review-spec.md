# Review spec

Check whether the **implementation** satisfies a **given spec**. The user must name the spec (e.g. `@spec/state-machine.md`). If they name several, run this once per file. If they name none, ask which spec — do not review all of `spec/` by default.

Do not rewrite code unless they ask. Do not treat `.cursor/rules` as the spec (use `commands/review-code.md` for that). Do not add behaviour the spec does not state.

## How to review

1. Read the spec end to end. Extract every concrete requirement (tables, endpoints, fields, status codes, transitions, screens, test obligations). Number them if the spec does not.
2. Find the matching code and tests (search the repo; do not assume).
3. For each requirement mark **satisfied / partial / missing / extra**.
   - **Satisfied** — behaviour and tests match the spec.
   - **Partial** — some of the requirement exists (note what is missing).
   - **Missing** — no implementation or no proof (no test where the spec demands one).
   - **Extra** — implementation does something the spec does not ask for (call it out; only fail if it contradicts the spec).
4. Quote the spec line or section next to each gap. Cite code with path + symbol.

## Spec-specific checks

Apply the block that matches the file under review. Skip the others.

**`spec/requirements.md`** — each numbered FR and each acceptance criterion maps to UI and/or API behaviour. Persistence across restart and “no secrets committed” included.

**`spec/architecture.md`** — two processes (Next.js → `/api/v1` → Spring Boot → PostgreSQL); controller → service → repository; state machine in the service; errors `{ code, message, details }`; no extra services.

**`spec/data-model.md`** — `Ticket` and `Comment` only; columns, types, nullability, FKs, cascade; `status`/`priority` CHECKs; JPA enum **names**; assignee is a string, not a user table; graph **not** a DB constraint.

**`spec/api-contract.md`** — every endpoint exists with the documented method, path, request/response shape, and status codes. Search/filter are query params on `GET /tickets`. `PATCH` has no `status`. Comments nested. Pagination envelope. Error shape.

**`spec/state-machine.md`** — allow-list in the service matches the five valid edges exactly. `PATCH` cannot change status. Invalid pair: no `save`, `AppException` / `INVALID_TRANSITION`, HTTP **`422`**. Self-transitions and skipped steps rejected.

**`spec/ui-flow.md`** — only list `/`, create `/tickets/new`, detail `/tickets/[id]`. Search + status filter on list. Detail owns edit, comments, and allowed status actions. API errors shown on the same screen.

**`spec/test-strategy.md`** — unit vs integration split as specified. **Every valid and every invalid `(from, to)` pair** has its own unit case **and** its own integration case. Testcontainers PostgreSQL, not H2.

## Output

1. **Spec** — path reviewed
2. **Verdict** — satisfies / does not satisfy
3. **Traceability table** — requirement | status | evidence (code/test) or gap
4. **Contradictions** — code that violates the spec (not merely extra)
5. **Unproven** — implemented but no test the spec required
