CREATE TABLE account_business_restriction (
    id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    restriction_type VARCHAR(32) NOT NULL,
    status VARCHAR(16) NOT NULL,
    reason VARCHAR(255) NOT NULL,
    starts_at TIMESTAMP(3) NOT NULL,
    expires_at TIMESTAMP(3) NOT NULL,
    created_by BIGINT NOT NULL,
    revoked_at TIMESTAMP(3) NULL,
    revoked_by BIGINT NULL,
    revoke_reason VARCHAR(255) NULL,
    active_marker TINYINT NULL,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(3) NOT NULL,
    updated_at TIMESTAMP(3) NOT NULL,
    CONSTRAINT pk_account_business_restriction PRIMARY KEY (id),
    CONSTRAINT uk_account_business_restriction_active
        UNIQUE (account_id, restriction_type, active_marker),
    CONSTRAINT fk_account_business_restriction_account
        FOREIGN KEY (account_id) REFERENCES user_account (id),
    CONSTRAINT fk_account_business_restriction_creator
        FOREIGN KEY (created_by) REFERENCES user_account (id),
    CONSTRAINT fk_account_business_restriction_revoker
        FOREIGN KEY (revoked_by) REFERENCES user_account (id),
    CONSTRAINT ck_account_business_restriction_type CHECK (
        restriction_type IN ('MEETUP_CREATE_DISABLED', 'MEETUP_JOIN_DISABLED')
    ),
    CONSTRAINT ck_account_business_restriction_status CHECK (
        status IN ('ACTIVE', 'REVOKED', 'EXPIRED')
    ),
    CONSTRAINT ck_account_business_restriction_time CHECK (expires_at > starts_at),
    CONSTRAINT ck_account_business_restriction_state CHECK (
        (status = 'ACTIVE' AND active_marker = 1
            AND revoked_at IS NULL AND revoked_by IS NULL AND revoke_reason IS NULL)
        OR (status = 'REVOKED' AND active_marker IS NULL
            AND revoked_at IS NOT NULL AND revoked_by IS NOT NULL AND revoke_reason IS NOT NULL)
        OR (status = 'EXPIRED' AND active_marker IS NULL
            AND revoked_at IS NULL AND revoked_by IS NULL AND revoke_reason IS NULL)
    )
);

CREATE INDEX idx_business_restriction_account
    ON account_business_restriction (account_id, status, expires_at);

CREATE TABLE account_business_restriction_audit_log (
    id BIGINT NOT NULL,
    restriction_id BIGINT NOT NULL,
    operator_account_id BIGINT NULL,
    action VARCHAR(16) NOT NULL,
    reason VARCHAR(255) NULL,
    occurred_at TIMESTAMP(3) NOT NULL,
    CONSTRAINT pk_business_restriction_audit_log PRIMARY KEY (id),
    CONSTRAINT fk_business_restriction_audit_restriction
        FOREIGN KEY (restriction_id) REFERENCES account_business_restriction (id),
    CONSTRAINT fk_business_restriction_audit_operator
        FOREIGN KEY (operator_account_id) REFERENCES user_account (id),
    CONSTRAINT ck_business_restriction_audit_action CHECK (
        action IN ('CREATE', 'REVOKE', 'EXPIRE')
    ),
    CONSTRAINT ck_business_restriction_audit_operator CHECK (
        (action = 'EXPIRE' AND operator_account_id IS NULL)
        OR (action <> 'EXPIRE' AND operator_account_id IS NOT NULL)
    )
);

CREATE INDEX idx_business_restriction_audit_restriction
    ON account_business_restriction_audit_log (restriction_id, occurred_at);
