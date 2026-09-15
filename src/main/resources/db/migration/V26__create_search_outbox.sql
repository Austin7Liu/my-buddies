CREATE TABLE search_outbox_event (
    id BIGINT NOT NULL,
    aggregate_type VARCHAR(20) NOT NULL,
    aggregate_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    next_retry_at TIMESTAMP(3) NOT NULL,
    last_error VARCHAR(500) NULL,
    locked_by VARCHAR(100) NULL,
    locked_at TIMESTAMP(3) NULL,
    created_at TIMESTAMP(3) NOT NULL,
    processed_at TIMESTAMP(3) NULL,
    CONSTRAINT pk_search_outbox_event PRIMARY KEY (id),
    CONSTRAINT ck_search_outbox_aggregate_type CHECK (
        aggregate_type IN ('TOPIC', 'CIRCLE', 'POST', 'MEETUP')
    ),
    CONSTRAINT ck_search_outbox_status CHECK (
        status IN ('PENDING', 'PROCESSING', 'SUCCEEDED', 'DEAD')
    ),
    CONSTRAINT ck_search_outbox_retry_count CHECK (retry_count >= 0)
);

CREATE INDEX idx_search_outbox_pending
    ON search_outbox_event (status, next_retry_at, created_at, id);

CREATE INDEX idx_search_outbox_aggregate
    ON search_outbox_event (aggregate_type, aggregate_id, id);
