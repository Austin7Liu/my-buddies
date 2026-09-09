ALTER TABLE meetup
    ADD COLUMN location_latitude DECIMAL(10, 7) NULL;

ALTER TABLE meetup
    ADD COLUMN location_longitude DECIMAL(10, 7) NULL;

ALTER TABLE meetup
    ADD COLUMN check_in_radius_meters INT NULL;

ALTER TABLE meetup
    ADD CONSTRAINT ck_meetup_check_in_location CHECK (
        (meetup_mode = 'ONLINE'
            AND location_latitude IS NULL
            AND location_longitude IS NULL
            AND check_in_radius_meters IS NULL)
        OR (meetup_mode = 'OFFLINE'
            AND ((location_latitude IS NULL
                    AND location_longitude IS NULL
                    AND check_in_radius_meters IS NULL)
                OR (location_latitude BETWEEN -90 AND 90
                    AND location_longitude BETWEEN -180 AND 180
                    AND check_in_radius_meters BETWEEN 50 AND 1000)))
    );

CREATE TABLE meetup_check_in (
    id BIGINT NOT NULL,
    meetup_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    distance_meters INT NOT NULL,
    checked_in_at TIMESTAMP(3) NOT NULL,
    created_at TIMESTAMP(3) NOT NULL,
    CONSTRAINT pk_meetup_check_in PRIMARY KEY (id),
    CONSTRAINT uk_meetup_check_in UNIQUE (meetup_id, account_id),
    CONSTRAINT fk_meetup_check_in_meetup FOREIGN KEY (meetup_id) REFERENCES meetup (id),
    CONSTRAINT fk_meetup_check_in_account FOREIGN KEY (account_id) REFERENCES user_account (id),
    CONSTRAINT ck_meetup_check_in_distance CHECK (distance_meters >= 0)
);

CREATE INDEX idx_meetup_check_in_meetup ON meetup_check_in (meetup_id, checked_in_at);

ALTER TABLE meetup_audit_log
    DROP CONSTRAINT ck_meetup_audit_action;

ALTER TABLE meetup_audit_log
    ADD CONSTRAINT ck_meetup_audit_action CHECK (
        action IN ('CREATE', 'UPDATE', 'PUBLISH', 'CONFIRM', 'COMPLETE', 'CHECK_IN',
                   'CANCEL', 'TERMINATE', 'APPLY', 'ACCEPT', 'REJECT', 'WITHDRAW')
    );
