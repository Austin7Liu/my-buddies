CREATE TABLE circle_member (
    id BIGINT NOT NULL,
    circle_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    role VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL,
    joined_at TIMESTAMP(3) NOT NULL,
    left_at TIMESTAMP(3) NULL,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(3) NOT NULL,
    updated_at TIMESTAMP(3) NOT NULL,
    CONSTRAINT pk_circle_member PRIMARY KEY (id),
    CONSTRAINT uk_circle_member_circle_account UNIQUE (circle_id, account_id),
    CONSTRAINT fk_circle_member_circle FOREIGN KEY (circle_id) REFERENCES circle (id),
    CONSTRAINT fk_circle_member_account FOREIGN KEY (account_id) REFERENCES user_account (id),
    CONSTRAINT ck_circle_member_role CHECK (role IN ('OWNER', 'MEMBER')),
    CONSTRAINT ck_circle_member_status CHECK (status IN ('ACTIVE', 'LEFT')),
    CONSTRAINT ck_circle_member_left_at CHECK (
        (status = 'ACTIVE' AND left_at IS NULL)
        OR (status = 'LEFT' AND left_at IS NOT NULL)
    )
);

CREATE INDEX idx_circle_member_circle_status_joined
    ON circle_member (circle_id, status, joined_at);

CREATE INDEX idx_circle_member_account_status_joined
    ON circle_member (account_id, status, joined_at);

INSERT INTO circle_member (
    id,
    circle_id,
    account_id,
    role,
    status,
    joined_at,
    left_at,
    version,
    created_at,
    updated_at
)
SELECT
    c.id,
    c.id,
    c.creator_account_id,
    'OWNER',
    'ACTIVE',
    c.created_at,
    NULL,
    0,
    c.created_at,
    c.updated_at
FROM circle c;
