CREATE TABLE user_profile (
    account_id BIGINT NOT NULL,
    nickname VARCHAR(30) NOT NULL,
    avatar_code VARCHAR(16) NOT NULL,
    bio VARCHAR(200) NULL,
    city VARCHAR(64) NULL,
    district VARCHAR(64) NULL,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(3) NOT NULL,
    updated_at TIMESTAMP(3) NOT NULL,
    CONSTRAINT pk_user_profile PRIMARY KEY (account_id),
    CONSTRAINT fk_user_profile_account FOREIGN KEY (account_id) REFERENCES user_account (id),
    CONSTRAINT ck_user_profile_nickname CHECK (nickname <> ''),
    CONSTRAINT ck_user_profile_avatar CHECK (avatar_code IN ('PANDA', 'CAT', 'DOG', 'FOX', 'RABBIT'))
);
