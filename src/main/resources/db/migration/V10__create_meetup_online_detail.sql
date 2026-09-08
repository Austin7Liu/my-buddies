CREATE TABLE meetup_online_detail (
    meetup_id BIGINT NOT NULL,
    online_platform VARCHAR(64) NOT NULL,
    server_region VARCHAR(64) NULL,
    access_instructions VARCHAR(500) NOT NULL,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(3) NOT NULL,
    updated_at TIMESTAMP(3) NOT NULL,
    CONSTRAINT pk_meetup_online_detail PRIMARY KEY (meetup_id),
    CONSTRAINT fk_meetup_online_detail_meetup FOREIGN KEY (meetup_id) REFERENCES meetup (id),
    CONSTRAINT ck_meetup_online_platform CHECK (online_platform <> ''),
    CONSTRAINT ck_meetup_access_instructions CHECK (access_instructions <> '')
);
