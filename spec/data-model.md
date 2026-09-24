# Data model

PostgreSQL. Two entities: **Ticket** and **Comment**. No user table — `assignee` is a display string on the ticket (requirements allow changing it, not managing accounts).

JPA: `@Enumerated(EnumType.STRING)` for enums. Never persist ordinals.

## Ticket

| Column | Type | Constraints |
|---|---|---|
| `id` | `UUID` | PK, generated |
| `title` | `VARCHAR(200)` | `NOT NULL` |
| `description` | `TEXT` | `NOT NULL` |
| `status` | `VARCHAR(32)` | `NOT NULL`, default `OPEN`, **CHECK** (see below) |
| `priority` | `VARCHAR(16)` | `NOT NULL`, **CHECK** `IN ('LOW','MEDIUM','HIGH')` |
| `assignee` | `VARCHAR(200)` | nullable |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL` |
| `updated_at` | `TIMESTAMPTZ` | `NOT NULL` |

Java: `TicketStatus` (`OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`, `CANCELLED`), `Priority` (`LOW`, `MEDIUM`, `HIGH`). New tickets start as `OPEN`.

Indexes: `status` (filter); `title` / `description` used for keyword search (`ILIKE` or similar on both).

## Comment

| Column | Type | Constraints |
|---|---|---|
| `id` | `UUID` | PK, generated |
| `ticket_id` | `UUID` | `NOT NULL`, FK → `ticket.id`, `ON DELETE CASCADE` |
| `body` | `TEXT` | `NOT NULL` |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL` |

Comments are append-only. No edit/delete in this app.

## Relationships

```
Ticket 1 ──< Comment
```

- A ticket has many comments (`@OneToMany` / `comments`).
- A comment belongs to exactly one ticket (`@ManyToOne`, not optional).
- Load comments with the ticket on the detail view; do not eager-load them on list.

## Status enum at the DB

The **allowed values** are enforced in PostgreSQL with a `CHECK` constraint (not a native PG `ENUM` type — those are painful to migrate):

```sql
ALTER TABLE ticket ADD CONSTRAINT ticket_status_check
  CHECK (status IN ('OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED', 'CANCELLED'));
```

- Column is `VARCHAR`, `NOT NULL`.
- Hibernate writes the enum **name**.
- Invalid strings fail at insert/update with a constraint violation.

The **transition graph** is **not** a DB constraint. `CHECK` only bounds the set of values. `CLOSED → OPEN` is still a legal row mutation at SQL level; the service must reject it (`422`). Same for any other edge not listed in `spec/state-machine.md`.
