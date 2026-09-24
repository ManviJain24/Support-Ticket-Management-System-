CREATE TABLE ticket (
    id UUID PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    priority VARCHAR(16) NOT NULL,
    assignee VARCHAR(200),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ticket_status_check
        CHECK (status IN ('OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED', 'CANCELLED')),
    CONSTRAINT ticket_priority_check
        CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH'))
);

CREATE INDEX idx_ticket_status ON ticket (status);

CREATE TABLE comment (
    id UUID PRIMARY KEY,
    ticket_id UUID NOT NULL REFERENCES ticket (id) ON DELETE CASCADE,
    body TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);
