CREATE TABLE instructors_grades.published_protocols_outbox (
    id          BIGSERIAL PRIMARY KEY ,
    message_key VARCHAR NOT NULL,
    payload     TEXT,
    created_at  timestamptz NOT NULL DEFAULT now(),
    sent_at     timestamptz,
    attempts    INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_outbox_unsent ON instructors_grades.published_protocols_outbox (id) WHERE sent_at IS NULL;