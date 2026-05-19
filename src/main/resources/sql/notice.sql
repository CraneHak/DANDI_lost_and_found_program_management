ALTER TABLE notice
    ADD COLUMN requester_uid varchar(128) NULL;

UPDATE notice
SET requester_uid = 'legacy'
WHERE requester_uid IS NULL;

ALTER TABLE notice
    MODIFY COLUMN requester_uid varchar(128) NOT NULL;

CREATE INDEX idx_notice_requester_uid_created_at
    ON notice (requester_uid, created_at);
