CREATE TABLE post_like (
    id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    created_at TIMESTAMP(3) NOT NULL,
    CONSTRAINT pk_post_like PRIMARY KEY (id),
    CONSTRAINT uk_post_like_post_account UNIQUE (post_id, account_id),
    CONSTRAINT fk_post_like_post FOREIGN KEY (post_id) REFERENCES post (id),
    CONSTRAINT fk_post_like_account FOREIGN KEY (account_id) REFERENCES user_account (id)
);

CREATE INDEX idx_post_like_account_created ON post_like (account_id, created_at, id);
CREATE INDEX idx_post_like_post_created ON post_like (post_id, created_at, id);

CREATE TABLE post_bookmark (
    id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    created_at TIMESTAMP(3) NOT NULL,
    CONSTRAINT pk_post_bookmark PRIMARY KEY (id),
    CONSTRAINT uk_post_bookmark_post_account UNIQUE (post_id, account_id),
    CONSTRAINT fk_post_bookmark_post FOREIGN KEY (post_id) REFERENCES post (id),
    CONSTRAINT fk_post_bookmark_account FOREIGN KEY (account_id) REFERENCES user_account (id)
);

CREATE INDEX idx_post_bookmark_account_created ON post_bookmark (account_id, created_at, id);

ALTER TABLE user_notification DROP CONSTRAINT ck_user_notification_type;
ALTER TABLE user_notification ADD CONSTRAINT ck_user_notification_type CHECK (notification_type IN (
    'MEETUP_APPLICATION_ACCEPTED', 'MEETUP_APPLICATION_REJECTED',
    'MEETUP_CANCELLED', 'MEETUP_TERMINATED', 'FULFILLMENT_ADJUSTED',
    'REVIEW_HIDDEN', 'REVIEW_RESTORED', 'RISK_RESTRICTION_CREATED',
    'RISK_RESTRICTION_REVOKED', 'POST_COMMENTED', 'COMMENT_REPLIED',
    'REPORT_RESOLVED', 'REPORT_REJECTED', 'REPORTED_CONTENT_OFFLINED',
    'REPORTED_COMMENT_HIDDEN', 'CONTENT_APPEAL_APPROVED',
    'CONTENT_APPEAL_REJECTED', 'CONTENT_APPEAL_CLOSED',
    'CONTENT_VIOLATION_CONFIRMED', 'CONTENT_VIOLATION_REVOKED',
    'POST_LIKED'
));

ALTER TABLE user_notification DROP CONSTRAINT ck_user_notification_reference;
ALTER TABLE user_notification ADD CONSTRAINT ck_user_notification_reference CHECK (
    reference_type IN ('MEETUP', 'MEETUP_REVIEW', 'RISK_RESTRICTION', 'POST_COMMENT',
        'CONTENT_REPORT', 'CONTENT_APPEAL', 'CONTENT_VIOLATION', 'POST_LIKE')
);
