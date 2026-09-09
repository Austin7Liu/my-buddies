CREATE TABLE post_comment (
    id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    author_account_id BIGINT NOT NULL,
    parent_comment_id BIGINT NULL,
    content VARCHAR(500) NOT NULL,
    status VARCHAR(32) NOT NULL,
    deleted_at TIMESTAMP(3) NULL,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(3) NOT NULL,
    updated_at TIMESTAMP(3) NOT NULL,
    CONSTRAINT pk_post_comment PRIMARY KEY (id),
    CONSTRAINT fk_post_comment_post FOREIGN KEY (post_id) REFERENCES post (id),
    CONSTRAINT fk_post_comment_author FOREIGN KEY (author_account_id) REFERENCES user_account (id),
    CONSTRAINT fk_post_comment_parent FOREIGN KEY (parent_comment_id) REFERENCES post_comment (id),
    CONSTRAINT ck_post_comment_status CHECK (status IN (
        'VISIBLE', 'DELETED_BY_AUTHOR', 'HIDDEN_BY_ADMIN'
    ))
);

CREATE INDEX idx_post_comment_post
    ON post_comment (post_id, created_at, id);

CREATE INDEX idx_post_comment_parent
    ON post_comment (parent_comment_id);

CREATE TABLE post_comment_audit_log (
    id BIGINT NOT NULL,
    comment_id BIGINT NOT NULL,
    operator_account_id BIGINT NOT NULL,
    action VARCHAR(24) NOT NULL,
    reason VARCHAR(500) NULL,
    occurred_at TIMESTAMP(3) NOT NULL,
    CONSTRAINT pk_post_comment_audit_log PRIMARY KEY (id),
    CONSTRAINT fk_post_comment_audit_comment FOREIGN KEY (comment_id) REFERENCES post_comment (id),
    CONSTRAINT fk_post_comment_audit_operator FOREIGN KEY (operator_account_id) REFERENCES user_account (id),
    CONSTRAINT ck_post_comment_audit_action CHECK (action IN ('HIDE', 'RESTORE'))
);

CREATE INDEX idx_post_comment_audit_comment
    ON post_comment_audit_log (comment_id, occurred_at);

ALTER TABLE user_notification DROP CONSTRAINT ck_user_notification_type;
ALTER TABLE user_notification ADD CONSTRAINT ck_user_notification_type CHECK (notification_type IN (
    'MEETUP_APPLICATION_ACCEPTED', 'MEETUP_APPLICATION_REJECTED',
    'MEETUP_CANCELLED', 'MEETUP_TERMINATED', 'FULFILLMENT_ADJUSTED',
    'REVIEW_HIDDEN', 'REVIEW_RESTORED',
    'RISK_RESTRICTION_CREATED', 'RISK_RESTRICTION_REVOKED',
    'POST_COMMENTED', 'COMMENT_REPLIED'
));

ALTER TABLE user_notification DROP CONSTRAINT ck_user_notification_reference;
ALTER TABLE user_notification ADD CONSTRAINT ck_user_notification_reference CHECK (
    reference_type IN ('MEETUP', 'MEETUP_REVIEW', 'RISK_RESTRICTION', 'POST_COMMENT')
);
