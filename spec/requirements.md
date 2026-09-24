# Requirements

Support Ticket Management System.

## Functional requirements

1. A ticket can be created.
2. Tickets can be listed.
3. Ticket details can be viewed.
4. A ticket’s title, description, priority, and assignee can be updated.
5. Comments can be added to a ticket.
6. Tickets can be searched by keyword.
7. Tickets can be filtered by status.
8. Ticket data is persisted in a database and survives application restart.
9. The backend validates input.
10. The UI displays meaningful errors.
11. The backend enforces this ticket status state machine:
    - `OPEN` → `IN_PROGRESS` → `RESOLVED` → `CLOSED`
    - `OPEN` → `CANCELLED`
    - `IN_PROGRESS` → `CANCELLED`
12. Invalid status transitions are rejected by the backend. Examples of invalid transitions:
    - `CLOSED` → `OPEN`
    - `RESOLVED` → `OPEN`
    - `CANCELLED` → `OPEN`

## Acceptance criteria

The solution is complete when all of the following are true:

1. A ticket can be created from the UI.
2. Tickets can be listed.
3. Ticket details can be viewed.
4. Ticket fields can be updated.
5. Assignee can be changed.
6. Comments can be added.
7. Search works.
8. Status filter works.
9. Valid status transitions work.
10. Invalid status transitions are rejected by the backend.
11. Data survives application restart.
12. Backend validation works.
13. The UI shows meaningful errors.
14. State-machine integration tests pass.
15. No secrets are committed.

Live verification of each criterion is recorded in [`spec/acceptance-status.md`](acceptance-status.md).
