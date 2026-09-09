CREATE TABLE content_report (
    id BIGINT NOT NULL,
    reporter_account_id BIGINT NOT NULL,
    target_type VARCHAR(24) NOT NULL,
    reported_post_id BIGINT NULL,
    reported_comment_id BIGINT NULL,
    reason_type VARCHAR(32) NOT NULL,
    description VARCHAR(500) NULL,
    content_snapshot VARCHAR(2000) NOT NULL,
    status VARCHAR(24) NOT NULL,
    handled_by BIGINT NULL,
    handled_at TIMESTAMP(3) NULL,
    resolution_note VARCHAR(500) NULL,
    active_marker TINYINT NULL,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(3) NOT NULL,
    updated_at TIMESTAMP(3) NOT NULL,
    CONSTRAINT pk_content_report PRIMARY KEY (id),
    CONSTRAINT fk_content_report_reporter FOREIGN KEY (reporter_account_id) REFERENCES user_account (id),
    CONSTRAINT fk_content_report_post FOREIGN KEY (reported_post_id) REFERENCES post (id),
    CONSTRAINT fk_content_report_comment FOREIGN KEY (reported_comment_id) REFERENCES post_comment (id),
    CONSTRAINT fk_content_report_handler FOREIGN KEY (handled_by) REFERENCES user_account (id),
    CONSTRAINT uk_content_report_active_post UNIQUE (reporter_account_id, reported_post_id, active_marker),
    CONSTRAINT uk_content_report_active_comment UNIQUE (reporter_account_id, reported_comment_id, active_marker),
    CONSTRAINT ck_content_report_target_type CHECK (target_type IN ('POST', 'POST_COMMENT')),
    CONSTRAINT ck_content_report_target CHECK (
        (target_type = 'POST' AND reported_post_id IS NOT NULL AND reported_comment_id IS NULL)
        OR (target_type = 'POST_COMMENT' AND reported_post_id IS NULL AND reported_comment_id IS NOT NULL)
    ),
    CONSTRAINT ck_content_report_reason CHECK (reason_type IN (
        'SPAM', 'HARASSMENT', 'PORNOGRAPHY', 'VIOLENCE', 'FRAUD', 'PRIVACY', 'ILLEGAL', 'OTHER'
    )),
    CONSTRAINT ck_content_report_status CHECK (status IN ('PENDING', 'RESOLVED', 'REJECTED', 'DUPLICATE')),
    CONSTRAINT ck_content_report_active_marker CHECK (active_marker IS NULL OR active_marker = 1)
);

CREATE INDEX idx_content_report_status
    ON content_report (status, created_at, id);

CREATE INDEX idx_content_report_reporter
    ON content_report (reporter_account_id, created_at, id);

CREATE TABLE content_report_audit_log (
    id BIGINT NOT NULL,
    report_id BIGINT NOT NULL,
    operator_account_id BIGINT NOT NULL,
    action VARCHAR(24) NOT NULL,
    from_status VARCHAR(24) NOT NULL,
    to_status VARCHAR(24) NOT NULL,
    note VARCHAR(500) NOT NULL,
    occurred_at TIMESTAMP(3) NOT NULL,
    CONSTRAINT pk_content_report_audit_log PRIMARY KEY (id),
    CONSTRAINT fk_content_report_audit_report FOREIGN KEY (report_id) REFERENCES content_report (id),
    CONSTRAINT fk_content_report_audit_operator FOREIGN KEY (operator_account_id) REFERENCES user_account (id),
    CONSTRAINT ck_content_report_audit_action CHECK (action IN ('RESOLVE', 'REJECT', 'MARK_DUPLICATE'))
);

CREATE INDEX idx_content_report_audit_report
    ON content_report_audit_log (report_id, occurred_at);

ALTER TABLE user_notification DROP CONSTRAINT ck_user_notification_type;
ALTER TABLE user_notification ADD CONSTRAINT ck_user_notification_type CHECK (notification_type IN (
    'MEETUP_APPLICATION_ACCEPTED', 'MEETUP_APPLICATION_REJECTED',
    'MEETUP_CANCELLED', 'MEETUP_TERMINATED', 'FULFILLMENT_ADJUSTED',
    'REVIEW_HIDDEN', 'REVIEW_RESTORED',
    'RISK_RESTRICTION_CREATED', 'RISK_RESTRICTION_REVOKED',
    'POST_COMMENTED', 'COMMENT_REPLIED', 'REPORT_RESOLVED', 'REPORT_REJECTED'
));

ALTER TABLE user_notification DROP CONSTRAINT ck_user_notification_reference;
ALTER TABLE user_notification ADD CONSTRAINT ck_user_notification_reference CHECK (
    reference_type IN (
        'MEETUP', 'MEETUP_REVIEW', 'RISK_RESTRICTION', 'POST_COMMENT', 'CONTENT_REPORT'
    )
);
