UPDATE content_report
SET appeal_deadline_at = handled_at
WHERE status = 'RESOLVED'
  AND appeal_deadline_at IS NULL;

CREATE TABLE content_violation_restriction (
    violation_id BIGINT NOT NULL,
    restriction_id BIGINT NOT NULL,
    restriction_type VARCHAR(48) NOT NULL,
    created_at TIMESTAMP(3) NOT NULL,
    CONSTRAINT pk_content_violation_restriction PRIMARY KEY (violation_id, restriction_id),
    CONSTRAINT uk_content_violation_restriction_type UNIQUE (violation_id, restriction_type),
    CONSTRAINT uk_content_violation_restriction_id UNIQUE (restriction_id),
    CONSTRAINT fk_violation_restriction_violation FOREIGN KEY (violation_id)
        REFERENCES account_content_violation (id),
    CONSTRAINT fk_violation_restriction_restriction FOREIGN KEY (restriction_id)
        REFERENCES account_business_restriction (id),
    CONSTRAINT ck_violation_restriction_type CHECK (restriction_type IN (
        'POST_CREATE_DISABLED', 'COMMENT_CREATE_DISABLED'
    ))
);
