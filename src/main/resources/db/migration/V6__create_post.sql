CREATE TABLE post (
    id BIGINT NOT NULL,
    author_account_id BIGINT NOT NULL,
    topic_id BIGINT NULL,
    circle_id BIGINT NULL,
    content VARCHAR(2000) NOT NULL,
    status VARCHAR(32) NOT NULL,
    moderation_reason VARCHAR(255) NULL,
    moderated_by BIGINT NULL,
    moderated_at TIMESTAMP(3) NULL,
    deleted_at TIMESTAMP(3) NULL,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(3) NOT NULL,
    updated_at TIMESTAMP(3) NOT NULL,
    CONSTRAINT pk_post PRIMARY KEY (id),
    CONSTRAINT fk_post_author FOREIGN KEY (author_account_id) REFERENCES user_account (id),
    CONSTRAINT fk_post_topic FOREIGN KEY (topic_id) REFERENCES topic (id),
    CONSTRAINT fk_post_circle FOREIGN KEY (circle_id) REFERENCES circle (id),
    CONSTRAINT fk_post_moderator FOREIGN KEY (moderated_by) REFERENCES user_account (id),
    CONSTRAINT ck_post_status CHECK (status IN ('PENDING_REVIEW', 'PUBLISHED', 'REJECTED', 'OFFLINE', 'DELETED')),
    CONSTRAINT ck_post_circle_topic CHECK (circle_id IS NULL OR topic_id IS NOT NULL),
    CONSTRAINT ck_post_moderation CHECK (
        (status = 'PENDING_REVIEW' AND moderated_by IS NULL AND moderated_at IS NULL AND moderation_reason IS NULL)
        OR (status = 'PUBLISHED' AND moderated_by IS NOT NULL AND moderated_at IS NOT NULL AND moderation_reason IS NULL)
        OR (status IN ('REJECTED', 'OFFLINE') AND moderated_by IS NOT NULL AND moderated_at IS NOT NULL AND moderation_reason IS NOT NULL)
        OR status = 'DELETED'
    )
);

CREATE INDEX idx_post_status_created ON post (status, created_at);
CREATE INDEX idx_post_topic_status_created ON post (topic_id, status, created_at);
CREATE INDEX idx_post_circle_status_created ON post (circle_id, status, created_at);
CREATE INDEX idx_post_author_created ON post (author_account_id, created_at);

CREATE TABLE post_audit_log (
    id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    operator_account_id BIGINT NOT NULL,
    action VARCHAR(16) NOT NULL,
    from_status VARCHAR(32) NULL,
    to_status VARCHAR(32) NOT NULL,
    reason VARCHAR(255) NULL,
    occurred_at TIMESTAMP(3) NOT NULL,
    CONSTRAINT pk_post_audit_log PRIMARY KEY (id),
    CONSTRAINT fk_post_audit_post FOREIGN KEY (post_id) REFERENCES post (id),
    CONSTRAINT fk_post_audit_operator FOREIGN KEY (operator_account_id) REFERENCES user_account (id),
    CONSTRAINT ck_post_audit_action CHECK (action IN ('SUBMIT', 'UPDATE', 'RESUBMIT', 'APPROVE', 'REJECT', 'DELETE', 'OFFLINE', 'RESTORE'))
);

CREATE INDEX idx_post_audit_post ON post_audit_log (post_id, occurred_at);
