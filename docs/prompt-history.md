# Prompt history

## 2026-09-24 — Spec, backend, UI, and review

### What was asked

- Scaffold Cursor rules, commands, and `spec/` files, then fill them from product notes (Java 21 + Spring Boot 3 REST, JUnit 5 / Mockito / Testcontainers, REST standards).
- Numbered requirements, architecture, data model, API contract, state machine, UI flow, test strategy, then `tasks.md`.
- Implement the app against that spec: entities + Flyway, REST API, state machine, Next.js UI.
- Fix run issues (process exit, CORS, Node version, `next: not found`).
- Audit `tasks.md` vs `spec/requirements.md`, close gaps, log this session.

### What was built

- Rules: `.cursor/rules/{java-springboot,testing,api-standards}.mdc`
- Commands: `review-code.md`, `review-spec.md`, `generate-tests.md`
- Specs under `spec/`; implementation backlog in `tasks.md`
- Spring Boot 3.4 / Java 21 API (`com.ticketing`): Ticket/Comment, Flyway CHECKs, CRUD + comments + search/filter, `POST /api/v1/tickets/{id}/status`, `TicketStatusTransitionValidator`, `{ code, message, details }` errors, CORS for local Next
- Tests: 25-pair unit matrix, MVC error tests, PostgreSQL integration tests (`ticketing_test` when Docker is unavailable)
- Next.js app: list / create / detail, API client + Vitest error-parsing tests
- Local `.env` (gitignored) + `.env.example`; Node 22 via nvm

### Mistakes and how they were fixed

1. **Wrong task as “T1”.** The request was “task 1 — Ticket and Comment entities + migration,” which is **T3** in `tasks.md` (T1 is the app skeleton). There was no Spring Boot project yet, so a minimal app was added so entities could compile. **Fix:** Treat T1 as skeleton + env/CORS/web, T3 as domain/Flyway; keep that split in `tasks.md`.

2. **Hardcoded DB password in `application.yml`.** Local DB credentials were written into config, which violates “no secrets in the repo.” **Fix:** Password (and later URL/username) taken only from env; values live in gitignored `.env`; `.env.example` has placeholders only.

3. **App started then exited 0.** Only `spring-boot-starter-data-jpa` was on the classpath, so there was no Tomcat keep-alive. **Fix:** Add `spring-boot-starter-web`.

4. **CORS failed on ticket create.** Mapping allowed only `http://localhost:3000` and header `Content-Type`, so JSON POST preflight (`OPTIONS`, `127.0.0.1`, extra headers) failed. **Fix:** `allowedOriginPatterns` for localhost / 127.0.0.1 / `[::1]` on any port, methods including `OPTIONS`, `allowedHeaders("*")`.

5. **`next: not found` on `npm run build`.** `node_modules` was never installed. **Fix:** `npm install` in `frontend/`. Next 15.5.2 also had a known CVE; later moved to Next **16.3.6** (needs Node **≥ 20.9**).

6. **Shell still on Node 18.19.1 after installing 22.** nvm Node 22 was installed, but an already-open terminal still used `/usr/bin/node` (Ubuntu 18). **Fix:** `export PATH="$HOME/.nvm/versions/node/v22.23.3/bin:$PATH"` (and prepend that path in `.bashrc` / `.profile`).

7. **`tasks.md` overstated “T1–T10 tests pass.”** Unit/MVC tests passed; **Testcontainers never ran** because `docker.sock` is not accessible. **Fix:** Integration tests now use `PostgresIntegrationTest` (Testcontainers if Docker works, else local `ticketing_test`). Status in `tasks.md` / `spec/acceptance-status.md` matches that.

8. **`GlobalExceptionHandlerTest` got 500 for 404/400/422.** The stub `@RestController` was not registered, so requests hit static-resource handling and the catch-all 5xx mapper. **Fix:** `@ContextConfiguration` with `GlobalExceptionHandler` + `TestController`.
