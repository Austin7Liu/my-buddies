ALTER TABLE meetup
    ADD COLUMN completed_at TIMESTAMP(3) NULL;

ALTER TABLE meetup
    DROP CONSTRAINT ck_meetup_status;

ALTER TABLE meetup
    DROP CONSTRAINT ck_meetup_closed;

ALTER TABLE meetup
    ADD CONSTRAINT ck_meetup_status CHECK (
        status IN ('DRAFT', 'OPEN', 'CONFIRMED', 'COMPLETED', 'CANCELLED', 'TERMINATED')
    );

ALTER TABLE meetup
    ADD CONSTRAINT ck_meetup_closed CHECK (
        (status IN ('DRAFT', 'OPEN', 'CONFIRMED', 'COMPLETED')
            AND closed_reason IS NULL AND closed_by IS NULL AND closed_at IS NULL)
        OR (status IN ('CANCELLED', 'TERMINATED')
            AND closed_reason IS NOT NULL AND closed_by IS NOT NULL AND closed_at IS NOT NULL)
    );

ALTER TABLE meetup
    ADD CONSTRAINT ck_meetup_completed CHECK (
        (status = 'COMPLETED' AND completed_at IS NOT NULL)
        OR (status <> 'COMPLETED' AND completed_at IS NULL)
    );

ALTER TABLE meetup_audit_log
    DROP CONSTRAINT ck_meetup_audit_action;

ALTER TABLE meetup_audit_log
    ADD CONSTRAINT ck_meetup_audit_action CHECK (
        action IN ('CREATE', 'UPDATE', 'PUBLISH', 'CONFIRM', 'COMPLETE', 'CANCEL',
                   'TERMINATE', 'APPLY', 'ACCEPT', 'REJECT', 'WITHDRAW')
    );
