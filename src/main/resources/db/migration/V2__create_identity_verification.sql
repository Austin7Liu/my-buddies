CREATE TABLE identity_verification (
    id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    subject_fingerprint VARCHAR(64) NULL,
    birth_date DATE NULL,
    gender VARCHAR(16) NULL,
    provider VARCHAR(32) NOT NULL,
    provider_reference VARCHAR(128) NULL,
    failure_code VARCHAR(64) NULL,
    submitted_at TIMESTAMP(3) NOT NULL,
    verified_at TIMESTAMP(3) NULL,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(3) NOT NULL,
    updated_at TIMESTAMP(3) NOT NULL,
    CONSTRAINT pk_identity_verification PRIMARY KEY (id),
    CONSTRAINT uk_identity_verification_account UNIQUE (account_id),
    CONSTRAINT uk_identity_verification_subject UNIQUE (subject_fingerprint),
    CONSTRAINT fk_identity_verification_account FOREIGN KEY (account_id) REFERENCES user_account (id),
    CONSTRAINT ck_identity_verification_status CHECK (
        status IN ('VERIFYING', 'VERIFIED', 'FAILED')
    ),
    CONSTRAINT ck_identity_verification_gender CHECK (
        gender IS NULL OR gender IN ('MALE', 'FEMALE')
    ),
    CONSTRAINT ck_identity_verification_result CHECK (
        (status = 'VERIFIED' AND subject_fingerprint IS NOT NULL AND birth_date IS NOT NULL
            AND gender IS NOT NULL AND verified_at IS NOT NULL AND failure_code IS NULL)
        OR status <> 'VERIFIED'
    )
);

CREATE INDEX idx_identity_verification_status ON identity_verification (status);

