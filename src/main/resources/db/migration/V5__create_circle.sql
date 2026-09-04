CREATE TABLE circle (
    id BIGINT NOT NULL,
    topic_id BIGINT NOT NULL,
    creator_account_id BIGINT NOT NULL,
    name VARCHAR(64) NOT NULL,
    description VARCHAR(500) NULL,
    city VARCHAR(64) NOT NULL,
    district VARCHAR(64) NULL,
    status VARCHAR(32) NOT NULL,
    rejection_reason VARCHAR(255) NULL,
    reviewed_by BIGINT NULL,
    reviewed_at TIMESTAMP(3) NULL,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(3) NOT NULL,
    updated_at TIMESTAMP(3) NOT NULL,
    CONSTRAINT pk_circle PRIMARY KEY (id),
    CONSTRAINT uk_circle_topic_name UNIQUE (topic_id, name),
    CONSTRAINT fk_circle_topic FOREIGN KEY (topic_id) REFERENCES topic (id),
    CONSTRAINT fk_circle_creator FOREIGN KEY (creator_account_id) REFERENCES user_account (id),
    CONSTRAINT fk_circle_reviewer FOREIGN KEY (reviewed_by) REFERENCES user_account (id),
    CONSTRAINT ck_circle_status CHECK (status IN ('PENDING_REVIEW', 'APPROVED', 'REJECTED', 'DISABLED')),
    CONSTRAINT ck_circle_review_result CHECK (
        (status = 'PENDING_REVIEW' AND reviewed_by IS NULL AND reviewed_at IS NULL AND rejection_reason IS NULL)
        OR (status = 'APPROVED' AND reviewed_by IS NOT NULL AND reviewed_at IS NOT NULL AND rejection_reason IS NULL)
        OR (status = 'REJECTED' AND reviewed_by IS NOT NULL AND reviewed_at IS NOT NULL AND rejection_reason IS NOT NULL)
        OR (status = 'DISABLED' AND reviewed_by IS NOT NULL AND reviewed_at IS NOT NULL AND rejection_reason IS NULL)
    )
);

CREATE INDEX idx_circle_topic_status ON circle (topic_id, status, created_at);
CREATE INDEX idx_circle_creator ON circle (creator_account_id, created_at);
CREATE INDEX idx_circle_review_queue ON circle (status, created_at);

CREATE TABLE circle_audit_log (
    id BIGINT NOT NULL,
    circle_id BIGINT NOT NULL,
    operator_account_id BIGINT NOT NULL,
    action VARCHAR(16) NOT NULL,
    from_status VARCHAR(32) NULL,
    to_status VARCHAR(32) NOT NULL,
    reason VARCHAR(255) NULL,
    occurred_at TIMESTAMP(3) NOT NULL,
    CONSTRAINT pk_circle_audit_log PRIMARY KEY (id),
    CONSTRAINT fk_circle_audit_circle FOREIGN KEY (circle_id) REFERENCES circle (id),
    CONSTRAINT fk_circle_audit_operator FOREIGN KEY (operator_account_id) REFERENCES user_account (id),
    CONSTRAINT ck_circle_audit_action CHECK (action IN ('SUBMIT', 'UPDATE', 'RESUBMIT', 'APPROVE', 'REJECT', 'DISABLE', 'RESTORE'))
);

CREATE INDEX idx_circle_audit_circle ON circle_audit_log (circle_id, occurred_at);

