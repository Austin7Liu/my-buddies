ALTER TABLE search_outbox_event
    ADD COLUMN event_type VARCHAR(30) NOT NULL DEFAULT 'REFRESH';

ALTER TABLE search_outbox_event
    ADD CONSTRAINT ck_search_outbox_event_type CHECK (
        event_type IN ('REFRESH', 'CASCADE_REFRESH')
    );
