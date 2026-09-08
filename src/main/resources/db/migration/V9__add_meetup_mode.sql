ALTER TABLE meetup
    ADD COLUMN meetup_mode VARCHAR(16) NULL;

UPDATE meetup
SET meetup_mode = 'OFFLINE'
WHERE meetup_mode IS NULL;

ALTER TABLE meetup
    MODIFY COLUMN meetup_mode VARCHAR(16) NOT NULL;

ALTER TABLE meetup
    MODIFY COLUMN city VARCHAR(64) NULL;

ALTER TABLE meetup
    MODIFY COLUMN district VARCHAR(64) NULL;

ALTER TABLE meetup
    MODIFY COLUMN location_name VARCHAR(128) NULL;

ALTER TABLE meetup
    MODIFY COLUMN address VARCHAR(255) NULL;

ALTER TABLE meetup
    ADD CONSTRAINT ck_meetup_mode CHECK (meetup_mode IN ('OFFLINE', 'ONLINE'));

ALTER TABLE meetup
    ADD CONSTRAINT ck_meetup_location CHECK (
        (meetup_mode = 'OFFLINE'
            AND city IS NOT NULL AND district IS NOT NULL AND location_name IS NOT NULL AND address IS NOT NULL)
        OR (meetup_mode = 'ONLINE'
            AND city IS NULL AND district IS NULL AND location_name IS NULL AND address IS NULL)
    );
