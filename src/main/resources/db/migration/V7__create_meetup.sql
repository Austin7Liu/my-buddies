CREATE TABLE meetup (
    id BIGINT NOT NULL,
    creator_account_id BIGINT NOT NULL,
    topic_id BIGINT NOT NULL,
    circle_id BIGINT NULL,
    title VARCHAR(100) NOT NULL,
    description VARCHAR(2000) NOT NULL,
    start_time TIMESTAMP(3) NOT NULL,
    end_time TIMESTAMP(3) NOT NULL,
    application_deadline TIMESTAMP(3) NOT NULL,
    city VARCHAR(64) NOT NULL,
    district VARCHAR(64) NOT NULL,
    venue_name VARCHAR(128) NOT NULL,
    address VARCHAR(255) NOT NULL,
    capacity INT NOT NULL,
    minimum_age INT NOT NULL,
    maximum_age INT NOT NULL,
    gender_requirement VARCHAR(32) NOT NULL,
    skill_requirement VARCHAR(255) NULL,
    status VARCHAR(32) NOT NULL,
    closed_reason VARCHAR(255) NULL,
    closed_by BIGINT NULL,
    closed_at TIMESTAMP(3) NULL,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(3) NOT NULL,
    updated_at TIMESTAMP(3) NOT NULL,
    CONSTRAINT pk_meetup PRIMARY KEY (id),
    CONSTRAINT fk_meetup_creator FOREIGN KEY (creator_account_id) REFERENCES user_account (id),
    CONSTRAINT fk_meetup_topic FOREIGN KEY (topic_id) REFERENCES topic (id),
    CONSTRAINT fk_meetup_circle FOREIGN KEY (circle_id) REFERENCES circle (id),
    CONSTRAINT fk_meetup_closed_by FOREIGN KEY (closed_by) REFERENCES user_account (id),
    CONSTRAINT ck_meetup_status CHECK (status IN ('DRAFT', 'OPEN', 'CONFIRMED', 'CANCELLED', 'TERMINATED')),
    CONSTRAINT ck_meetup_gender CHECK (gender_requirement IN ('ANY', 'SAME_GENDER')),
    CONSTRAINT ck_meetup_time CHECK (end_time > start_time AND application_deadline < start_time),
    CONSTRAINT ck_meetup_capacity CHECK (capacity >= 2),
    CONSTRAINT ck_meetup_age CHECK (minimum_age >= 18 AND maximum_age >= minimum_age),
    CONSTRAINT ck_meetup_circle_topic CHECK (circle_id IS NULL OR topic_id IS NOT NULL),
    CONSTRAINT ck_meetup_closed CHECK (
        (status IN ('DRAFT', 'OPEN', 'CONFIRMED') AND closed_reason IS NULL AND closed_by IS NULL AND closed_at IS NULL)
        OR (status IN ('CANCELLED', 'TERMINATED') AND closed_reason IS NOT NULL AND closed_by IS NOT NULL AND closed_at IS NOT NULL)
    )
);

CREATE INDEX idx_meetup_public ON meetup (status, start_time);
CREATE INDEX idx_meetup_topic ON meetup (topic_id, status, start_time);
CREATE INDEX idx_meetup_circle ON meetup (circle_id, status, start_time);
CREATE INDEX idx_meetup_creator ON meetup (creator_account_id, created_at);

CREATE TABLE meetup_participant (
    id BIGINT NOT NULL,
    meetup_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    role VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL,
    application_message VARCHAR(500) NULL,
    decision_reason VARCHAR(255) NULL,
    decided_at TIMESTAMP(3) NULL,
    cancelled_at TIMESTAMP(3) NULL,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(3) NOT NULL,
    updated_at TIMESTAMP(3) NOT NULL,
    CONSTRAINT pk_meetup_participant PRIMARY KEY (id),
    CONSTRAINT uk_meetup_participant UNIQUE (meetup_id, account_id),
    CONSTRAINT fk_meetup_participant_meetup FOREIGN KEY (meetup_id) REFERENCES meetup (id),
    CONSTRAINT fk_meetup_participant_account FOREIGN KEY (account_id) REFERENCES user_account (id),
    CONSTRAINT ck_meetup_participant_role CHECK (role IN ('CREATOR', 'MEMBER')),
    CONSTRAINT ck_meetup_participant_status CHECK (status IN ('APPLIED', 'ACCEPTED', 'REJECTED', 'CANCELLED'))
);

CREATE INDEX idx_meetup_participant_queue ON meetup_participant (meetup_id, status, created_at);
CREATE INDEX idx_meetup_participant_account ON meetup_participant (account_id, status, created_at);

CREATE TABLE meetup_audit_log (
    id BIGINT NOT NULL,
    meetup_id BIGINT NOT NULL,
    operator_account_id BIGINT NOT NULL,
    participant_account_id BIGINT NULL,
    action VARCHAR(16) NOT NULL,
    reason VARCHAR(255) NULL,
    occurred_at TIMESTAMP(3) NOT NULL,
    CONSTRAINT pk_meetup_audit_log PRIMARY KEY (id),
    CONSTRAINT fk_meetup_audit_meetup FOREIGN KEY (meetup_id) REFERENCES meetup (id),
    CONSTRAINT fk_meetup_audit_operator FOREIGN KEY (operator_account_id) REFERENCES user_account (id),
    CONSTRAINT fk_meetup_audit_participant FOREIGN KEY (participant_account_id) REFERENCES user_account (id),
    CONSTRAINT ck_meetup_audit_action CHECK (action IN ('CREATE', 'UPDATE', 'PUBLISH', 'CONFIRM', 'CANCEL', 'TERMINATE', 'APPLY', 'ACCEPT', 'REJECT', 'WITHDRAW'))
);

CREATE INDEX idx_meetup_audit_meetup ON meetup_audit_log (meetup_id, occurred_at);
