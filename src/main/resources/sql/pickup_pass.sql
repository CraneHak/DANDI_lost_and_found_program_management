CREATE TABLE IF NOT EXISTS pickup_pass (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token VARCHAR(120) NOT NULL UNIQUE,
    lost_item_id INT NOT NULL,
    requester_uid VARCHAR(128) NOT NULL,
    requester_email VARCHAR(255) NOT NULL,
    issued_at DATETIME(6) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    used_at DATETIME(6) NULL,
    verified_by_uid VARCHAR(128) NULL,
    verified_by_email VARCHAR(255) NULL,
    CONSTRAINT fk_pickup_pass_lost_item
        FOREIGN KEY (lost_item_id) REFERENCES lost_item(id)
);

CREATE INDEX IF NOT EXISTS idx_pickup_pass_lost_item_id ON pickup_pass(lost_item_id);
CREATE INDEX IF NOT EXISTS idx_pickup_pass_requester_uid ON pickup_pass(requester_uid);
