CREATE TABLE topic_follow (
    id BIGINT NOT NULL,
    topic_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    created_at TIMESTAMP(3) NOT NULL,
    CONSTRAINT pk_topic_follow PRIMARY KEY (id),
    CONSTRAINT uk_topic_follow_topic_account UNIQUE (topic_id, account_id),
    CONSTRAINT fk_topic_follow_topic FOREIGN KEY (topic_id) REFERENCES topic (id),
    CONSTRAINT fk_topic_follow_account FOREIGN KEY (account_id) REFERENCES user_account (id)
);

CREATE INDEX idx_topic_follow_account_created ON topic_follow (account_id, created_at, id);
CREATE INDEX idx_topic_follow_topic_created ON topic_follow (topic_id, created_at, id);
