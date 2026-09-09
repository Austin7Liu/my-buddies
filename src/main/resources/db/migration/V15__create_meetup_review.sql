CREATE TABLE meetup_review (
    id BIGINT NOT NULL,
    meetup_id BIGINT NOT NULL,
    reviewer_account_id BIGINT NOT NULL,
    reviewee_account_id BIGINT NOT NULL,
    rating TINYINT NOT NULL,
    comment VARCHAR(500) NULL,
    status VARCHAR(16) NOT NULL,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(3) NOT NULL,
    updated_at TIMESTAMP(3) NOT NULL,
    CONSTRAINT pk_meetup_review PRIMARY KEY (id),
    CONSTRAINT uk_meetup_review UNIQUE (meetup_id, reviewer_account_id, reviewee_account_id),
    CONSTRAINT fk_meetup_review_meetup FOREIGN KEY (meetup_id) REFERENCES meetup (id),
    CONSTRAINT fk_meetup_review_reviewer FOREIGN KEY (reviewer_account_id) REFERENCES user_account (id),
    CONSTRAINT fk_meetup_review_reviewee FOREIGN KEY (reviewee_account_id) REFERENCES user_account (id),
    CONSTRAINT ck_meetup_review_rating CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT ck_meetup_review_accounts CHECK (reviewer_account_id <> reviewee_account_id),
    CONSTRAINT ck_meetup_review_status CHECK (status IN ('VISIBLE', 'HIDDEN'))
);

CREATE INDEX idx_meetup_review_reviewee ON meetup_review (reviewee_account_id, status, created_at);
CREATE INDEX idx_meetup_review_reviewer ON meetup_review (reviewer_account_id, meetup_id);

CREATE TABLE meetup_review_audit_log (
    id BIGINT NOT NULL,
    review_id BIGINT NOT NULL,
    operator_account_id BIGINT NOT NULL,
    action VARCHAR(16) NOT NULL,
    reason VARCHAR(255) NOT NULL,
    occurred_at TIMESTAMP(3) NOT NULL,
    CONSTRAINT pk_meetup_review_audit_log PRIMARY KEY (id),
    CONSTRAINT fk_meetup_review_audit_review FOREIGN KEY (review_id) REFERENCES meetup_review (id),
    CONSTRAINT fk_meetup_review_audit_operator FOREIGN KEY (operator_account_id) REFERENCES user_account (id),
    CONSTRAINT ck_meetup_review_audit_action CHECK (action IN ('HIDE', 'RESTORE'))
);

CREATE INDEX idx_meetup_review_audit_review ON meetup_review_audit_log (review_id, occurred_at);
