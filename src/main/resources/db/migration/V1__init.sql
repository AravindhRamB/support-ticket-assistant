CREATE TABLE tickets (
    id UUID PRIMARY KEY,
    customer_id VARCHAR(100) NOT NULL,
    subject VARCHAR(300) NOT NULL,
    description TEXT NOT NULL,
    priority VARCHAR(20) NOT NULL CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH')),
    product VARCHAR(150),
    analysis_status VARCHAR(20) NOT NULL CHECK (analysis_status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')),
    analysis_error TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_ticket_customer ON tickets(customer_id);
CREATE INDEX idx_ticket_status ON tickets(analysis_status);
CREATE INDEX idx_ticket_created ON tickets(created_at);

CREATE TABLE ticket_analyses (
    id UUID PRIMARY KEY,
    ticket_id UUID NOT NULL UNIQUE REFERENCES tickets(id) ON DELETE CASCADE,
    category VARCHAR(100) NOT NULL,
    summary TEXT NOT NULL,
    suggested_response TEXT NOT NULL,
    recommended_team VARCHAR(120) NOT NULL,
    confidence DOUBLE PRECISION NOT NULL CHECK (confidence >= 0 AND confidence <= 1),
    created_at TIMESTAMPTZ NOT NULL
);
