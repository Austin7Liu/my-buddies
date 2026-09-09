CREATE TABLE content_appeal (
    id BIGINT NOT NULL,
    report_id BIGINT NOT NULL,
    appellant_account_id BIGINT NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    status VARCHAR(24) NOT NULL,
    reviewed_by BIGINT NULL,
    reviewed_at TIMESTAMP(3) NULL,
    review_note VARCHAR(500) NULL,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(3) NOT NULL,
    updated_at TIMESTAMP(3) NOT NULL,
    CONSTRAINT pk_content_appeal PRIMARY KEY (id),
    CONSTRAINT uk_content_appeal_report UNIQUE (report_id),
    CONSTRAINT fk_content_appeal_report FOREIGN KEY (report_id) REFERENCES content_report (id),
    CONSTRAINT fk_content_appeal_appellant FOREIGN KEY (appellant_account_id) REFERENCES user_account (id),
    CONSTRAINT fk_content_appeal_reviewer FOREIGN KEY (reviewed_by) REFERENCES user_account (id),
    CONSTRAINT ck_content_appeal_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CLOSED'))
);

CREATE INDEX idx_content_appeal_status ON content_appeal (status, created_at, id);
CREATE INDEX idx_content_appeal_appellant ON content_appeal (appellant_account_id, created_at, id);

CREATE TABLE content_appeal_audit_log (
    id BIGINT NOT NULL,
    appeal_id BIGINT NOT NULL,
    operator_account_id BIGINT NOT NULL,
    action VARCHAR(24) NOT NULL,
    from_status VARCHAR(24) NOT NULL,
    to_status VARCHAR(24) NOT NULL,
    note VARCHAR(500) NOT NULL,
    occurred_at TIMESTAMP(3) NOT NULL,
    CONSTRAINT pk_content_appeal_audit_log PRIMARY KEY (id),
    CONSTRAINT fk_content_appeal_audit_appeal FOREIGN KEY (appeal_id) REFERENCES content_appeal (id),
    CONSTRAINT fk_content_appeal_audit_operator FOREIGN KEY (operator_account_id) REFERENCES user_account (id),
    CONSTRAINT ck_content_appeal_audit_action CHECK (action IN ('APPROVE', 'REJECT', 'CLOSE'))
);

CREATE INDEX idx_content_appeal_audit_appeal ON content_appeal_audit_log (appeal_id, occurred_at);

ALTER TABLE user_notification DROP CONSTRAINT ck_user_notification_type;
ALTER TABLE user_notification ADD CONSTRAINT ck_user_notification_type CHECK (notification_type IN (
    'MEETUP_APPLICATION_ACCEPTED', 'MEETUP_APPLICATION_REJECTED',
    'MEETUP_CANCELLED', 'MEETUP_TERMINATED', 'FULFILLMENT_ADJUSTED',
    'REVIEW_HIDDEN', 'REVIEW_RESTORED',
    'RISK_RESTRICTION_CREATED', 'RISK_RESTRICTION_REVOKED',
    'POST_COMMENTED', 'COMMENT_REPLIED', 'REPORT_RESOLVED', 'REPORT_REJECTED',
    'REPORTED_CONTENT_OFFLINED', 'REPORTED_COMMENT_HIDDEN',
    'CONTENT_APPEAL_APPROVED', 'CONTENT_APPEAL_REJECTED', 'CONTENT_APPEAL_CLOSED'
));

ALTER TABLE user_notification DROP CONSTRAINT ck_user_notification_reference;
ALTER TABLE user_notification ADD CONSTRAINT ck_user_notification_reference CHECK (
    reference_type IN (
        'MEETUP', 'MEETUP_REVIEW', 'RISK_RESTRICTION', 'POST_COMMENT',
        'CONTENT_REPORT', 'CONTENT_APPEAL'
    )
);
