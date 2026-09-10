ALTER TABLE content_report ADD COLUMN appeal_deadline_at TIMESTAMP(3) NULL AFTER handled_at;

ALTER TABLE account_business_restriction DROP CONSTRAINT ck_account_business_restriction_type;
ALTER TABLE account_business_restriction ADD CONSTRAINT ck_account_business_restriction_type CHECK (
    restriction_type IN (
        'MEETUP_CREATE_DISABLED', 'MEETUP_JOIN_DISABLED',
        'POST_CREATE_DISABLED', 'COMMENT_CREATE_DISABLED'
    )
);

CREATE TABLE account_content_violation (
    id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    report_id BIGINT NOT NULL,
    severity VARCHAR(24) NOT NULL,
    penalty_type VARCHAR(32) NOT NULL,
    penalty_expires_at TIMESTAMP(3) NULL,
    note VARCHAR(500) NOT NULL,
    status VARCHAR(16) NOT NULL,
    confirmed_by BIGINT NOT NULL,
    confirmed_at TIMESTAMP(3) NOT NULL,
    revoked_by BIGINT NULL,
    revoked_at TIMESTAMP(3) NULL,
    revoke_reason VARCHAR(500) NULL,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(3) NOT NULL,
    updated_at TIMESTAMP(3) NOT NULL,
    CONSTRAINT pk_account_content_violation PRIMARY KEY (id),
    CONSTRAINT uk_account_content_violation_report UNIQUE (report_id),
    CONSTRAINT fk_content_violation_account FOREIGN KEY (account_id) REFERENCES user_account (id),
    CONSTRAINT fk_content_violation_report FOREIGN KEY (report_id) REFERENCES content_report (id),
    CONSTRAINT fk_content_violation_confirmer FOREIGN KEY (confirmed_by) REFERENCES user_account (id),
    CONSTRAINT fk_content_violation_revoker FOREIGN KEY (revoked_by) REFERENCES user_account (id),
    CONSTRAINT ck_content_violation_severity CHECK (severity IN ('MINOR', 'MODERATE', 'SEVERE')),
    CONSTRAINT ck_content_violation_penalty CHECK (penalty_type IN (
        'WARNING_ONLY', 'POST_DISABLED', 'COMMENT_DISABLED', 'CONTENT_CREATE_DISABLED'
    )),
    CONSTRAINT ck_content_violation_status CHECK (status IN ('ACTIVE', 'REVOKED'))
);

CREATE INDEX idx_content_violation_account ON account_content_violation (account_id, created_at, id);

CREATE TABLE account_content_violation_audit_log (
    id BIGINT NOT NULL,
    violation_id BIGINT NOT NULL,
    operator_account_id BIGINT NOT NULL,
    action VARCHAR(16) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    occurred_at TIMESTAMP(3) NOT NULL,
    CONSTRAINT pk_content_violation_audit PRIMARY KEY (id),
    CONSTRAINT fk_content_violation_audit_violation FOREIGN KEY (violation_id) REFERENCES account_content_violation (id),
    CONSTRAINT fk_content_violation_audit_operator FOREIGN KEY (operator_account_id) REFERENCES user_account (id),
    CONSTRAINT ck_content_violation_audit_action CHECK (action IN ('CONFIRM', 'REVOKE'))
);

ALTER TABLE user_notification DROP CONSTRAINT ck_user_notification_type;
ALTER TABLE user_notification ADD CONSTRAINT ck_user_notification_type CHECK (notification_type IN (
    'MEETUP_APPLICATION_ACCEPTED', 'MEETUP_APPLICATION_REJECTED',
    'MEETUP_CANCELLED', 'MEETUP_TERMINATED', 'FULFILLMENT_ADJUSTED',
    'REVIEW_HIDDEN', 'REVIEW_RESTORED', 'RISK_RESTRICTION_CREATED',
    'RISK_RESTRICTION_REVOKED', 'POST_COMMENTED', 'COMMENT_REPLIED',
    'REPORT_RESOLVED', 'REPORT_REJECTED', 'REPORTED_CONTENT_OFFLINED',
    'REPORTED_COMMENT_HIDDEN', 'CONTENT_APPEAL_APPROVED',
    'CONTENT_APPEAL_REJECTED', 'CONTENT_APPEAL_CLOSED',
    'CONTENT_VIOLATION_CONFIRMED', 'CONTENT_VIOLATION_REVOKED'
));

ALTER TABLE user_notification DROP CONSTRAINT ck_user_notification_reference;
ALTER TABLE user_notification ADD CONSTRAINT ck_user_notification_reference CHECK (
    reference_type IN ('MEETUP', 'MEETUP_REVIEW', 'RISK_RESTRICTION', 'POST_COMMENT',
        'CONTENT_REPORT', 'CONTENT_APPEAL', 'CONTENT_VIOLATION')
);
