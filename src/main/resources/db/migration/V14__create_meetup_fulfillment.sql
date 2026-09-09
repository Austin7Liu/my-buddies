CREATE TABLE meetup_fulfillment (
    id BIGINT NOT NULL,
    meetup_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    result VARCHAR(16) NOT NULL,
    source VARCHAR(32) NOT NULL,
    settled_at TIMESTAMP(3) NOT NULL,
    adjusted_by BIGINT NULL,
    adjustment_reason VARCHAR(255) NULL,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(3) NOT NULL,
    updated_at TIMESTAMP(3) NOT NULL,
    CONSTRAINT pk_meetup_fulfillment PRIMARY KEY (id),
    CONSTRAINT uk_meetup_fulfillment UNIQUE (meetup_id, account_id),
    CONSTRAINT fk_meetup_fulfillment_meetup FOREIGN KEY (meetup_id) REFERENCES meetup (id),
    CONSTRAINT fk_meetup_fulfillment_account FOREIGN KEY (account_id) REFERENCES user_account (id),
    CONSTRAINT fk_meetup_fulfillment_adjusted_by FOREIGN KEY (adjusted_by) REFERENCES user_account (id),
    CONSTRAINT ck_meetup_fulfillment_result CHECK (result IN ('ATTENDED', 'ABSENT', 'EXCUSED')),
    CONSTRAINT ck_meetup_fulfillment_source CHECK (source IN ('CHECK_IN', 'SYSTEM', 'ADMIN_OVERRIDE')),
    CONSTRAINT ck_meetup_fulfillment_adjustment CHECK (
        (source = 'ADMIN_OVERRIDE' AND adjusted_by IS NOT NULL AND adjustment_reason IS NOT NULL)
        OR (source <> 'ADMIN_OVERRIDE' AND adjusted_by IS NULL AND adjustment_reason IS NULL)
    )
);

CREATE INDEX idx_meetup_fulfillment_meetup ON meetup_fulfillment (meetup_id, result);
CREATE INDEX idx_meetup_fulfillment_account ON meetup_fulfillment (account_id, settled_at);

ALTER TABLE meetup_audit_log
    DROP CONSTRAINT ck_meetup_audit_action;

ALTER TABLE meetup_audit_log
    ADD CONSTRAINT ck_meetup_audit_action CHECK (
        action IN ('CREATE', 'UPDATE', 'PUBLISH', 'CONFIRM', 'COMPLETE', 'CHECK_IN',
                   'ADJUST_RESULT', 'CANCEL', 'TERMINATE', 'APPLY', 'ACCEPT', 'REJECT', 'WITHDRAW')
    );
