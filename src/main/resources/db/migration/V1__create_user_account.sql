CREATE TABLE user_account (
    id BIGINT NOT NULL,
    phone VARCHAR(32) NULL,
    account_status VARCHAR(32) NOT NULL,
    cancel_requested_at TIMESTAMP(3) NULL,
    cancelled_at TIMESTAMP(3) NULL,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(3) NOT NULL,
    updated_at TIMESTAMP(3) NOT NULL,
    CONSTRAINT pk_user_account PRIMARY KEY (id),
    CONSTRAINT uk_user_account_phone UNIQUE (phone),
    CONSTRAINT ck_user_account_status CHECK (
        account_status IN ('ACTIVE', 'CANCEL_PENDING', 'CANCELLED', 'BANNED')
    ),
    CONSTRAINT ck_user_account_phone_binding CHECK (
        account_status = 'CANCELLED' OR phone IS NOT NULL
    )
);

CREATE INDEX idx_user_account_status ON user_account (account_status);
CREATE INDEX idx_user_account_cancel_requested_at ON user_account (cancel_requested_at);
