CREATE TABLE user_notification (
    id BIGINT NOT NULL,
    recipient_account_id BIGINT NOT NULL,
    notification_type VARCHAR(48) NOT NULL,
    title VARCHAR(80) NOT NULL,
    content VARCHAR(500) NOT NULL,
    reference_type VARCHAR(32) NOT NULL,
    reference_id BIGINT NOT NULL,
    business_key VARCHAR(160) NOT NULL,
    read_at TIMESTAMP(3) NULL,
    created_at TIMESTAMP(3) NOT NULL,
    updated_at TIMESTAMP(3) NOT NULL,
    CONSTRAINT pk_user_notification PRIMARY KEY (id),
    CONSTRAINT uk_user_notification_business_key UNIQUE (business_key),
    CONSTRAINT fk_user_notification_recipient FOREIGN KEY (recipient_account_id) REFERENCES user_account (id),
    CONSTRAINT ck_user_notification_type CHECK (notification_type IN (
        'MEETUP_APPLICATION_ACCEPTED', 'MEETUP_APPLICATION_REJECTED',
        'MEETUP_CANCELLED', 'MEETUP_TERMINATED', 'FULFILLMENT_ADJUSTED',
        'REVIEW_HIDDEN', 'REVIEW_RESTORED',
        'RISK_RESTRICTION_CREATED', 'RISK_RESTRICTION_REVOKED'
    )),
    CONSTRAINT ck_user_notification_reference CHECK (
        reference_type IN ('MEETUP', 'MEETUP_REVIEW', 'RISK_RESTRICTION')
    )
);

CREATE INDEX idx_user_notification_recipient
    ON user_notification (recipient_account_id, read_at, created_at);
