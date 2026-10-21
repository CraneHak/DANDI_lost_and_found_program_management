-- DANDI 신규 RDS 부트스트랩
-- 엔티티 + sql/*.sql + migrate-lost-item-stored-date-datetime.sql 통합
-- 실행: mysql -h <RDS_ENDPOINT> -u dandi -p dandidb < 00_bootstrap_new_rds.sql

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS category (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    ocr_target TINYINT(1) NOT NULL DEFAULT 0
) CHARACTER SET utf8mb4;

INSERT INTO category (name, ocr_target) VALUES
    ('전자기기', 0),
    ('지갑', 0),
    ('카드', 1),
    ('신분증', 1),
    ('가방', 0),
    ('의류', 0)
ON DUPLICATE KEY UPDATE ocr_target = VALUES(ocr_target);

CREATE TABLE IF NOT EXISTS app_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    firebase_uid VARCHAR(128) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL,
    student_no VARCHAR(30),
    role VARCHAR(20) NOT NULL DEFAULT 'ROLE_USER',
    name VARCHAR(100),
    department VARCHAR(100),
    profile_completed TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    last_login_at DATETIME
) CHARACTER SET utf8mb4;

CREATE TABLE IF NOT EXISTS report (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    item_name VARCHAR(255) NOT NULL,
    category VARCHAR(255),
    lost_at VARCHAR(255),
    location VARCHAR(255),
    storage VARCHAR(255),
    memo TEXT,
    image LONGTEXT,
    reporter_uid VARCHAR(128),
    reporter_email VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at DATETIME NOT NULL,
    picked_up_at DATETIME NULL
) CHARACTER SET utf8mb4;

CREATE TABLE IF NOT EXISTS lost_item (
    id INT AUTO_INCREMENT PRIMARY KEY,
    post_no INT UNIQUE,
    report_id BIGINT NULL,
    item_name VARCHAR(255),
    found_location VARCHAR(255),
    lost_location VARCHAR(255),
    stored_location VARCHAR(255),
    stored_date DATETIME NULL,
    contact VARCHAR(100),
    color VARCHAR(50),
    item_type VARCHAR(255),
    category_id INT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'STORED',
    masking_flag TINYINT(1) NOT NULL DEFAULT 0,
    vision_dominant_colors_json TEXT NULL,
    vision_extracted_text TEXT NULL,
    image_url VARCHAR(1024) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_lost_item_category
        FOREIGN KEY (category_id) REFERENCES category(id),
    CONSTRAINT fk_lost_item_report
        FOREIGN KEY (report_id) REFERENCES report(id)
) CHARACTER SET utf8mb4;

CREATE UNIQUE INDEX IF NOT EXISTS uk_lost_item_report_id ON lost_item (report_id);
CREATE INDEX IF NOT EXISTS idx_lost_item_status ON lost_item(status);
CREATE INDEX IF NOT EXISTS idx_lost_item_category_id ON lost_item(category_id);

CREATE TABLE IF NOT EXISTS notice (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    message TEXT,
    `read` TINYINT(1) NOT NULL DEFAULT 0,
    requester_uid VARCHAR(128) NOT NULL,
    created_at DATETIME NOT NULL
) CHARACTER SET utf8mb4;

CREATE INDEX idx_notice_requester_uid_created_at
    ON notice (requester_uid, created_at);

CREATE TABLE IF NOT EXISTS keyword (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    keyword VARCHAR(255) NOT NULL,
    requester_uid VARCHAR(128) NOT NULL,
    created_at DATETIME NOT NULL,
    UNIQUE KEY uq_keyword_requester_uid_keyword (requester_uid, keyword)
) CHARACTER SET utf8mb4;

CREATE INDEX idx_keyword_requester_uid_created_at
    ON keyword (requester_uid, created_at);

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

CREATE INDEX idx_pickup_pass_lost_item_id ON pickup_pass(lost_item_id);
CREATE INDEX idx_pickup_pass_requester_uid ON pickup_pass(requester_uid);

CREATE TABLE IF NOT EXISTS collection_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    lost_item_id INT NOT NULL,
    pickup_pass_id BIGINT NULL,
    requester_uid VARCHAR(128) NOT NULL,
    requester_email VARCHAR(255) NOT NULL,
    manager_uid VARCHAR(128) NULL,
    manager_email VARCHAR(255) NULL,
    otp_token VARCHAR(120) NOT NULL,
    otp_expires_at DATETIME(6) NOT NULL,
    collected_at DATETIME(6) NULL,
    action VARCHAR(30) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_collection_log_lost_item
        FOREIGN KEY (lost_item_id) REFERENCES lost_item(id)
) CHARACTER SET utf8mb4;

CREATE INDEX idx_collection_log_lost_item_id ON collection_log(lost_item_id);
CREATE INDEX idx_collection_log_pickup_pass_id ON collection_log(pickup_pass_id);
CREATE INDEX idx_collection_log_requester_uid ON collection_log(requester_uid);
CREATE INDEX idx_collection_log_action ON collection_log(action);

CREATE TABLE IF NOT EXISTS lost_item_analysis (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    document_type VARCHAR(50) NOT NULL,
    ocr_applied TINYINT(1) NOT NULL,
    original_image_path VARCHAR(255) NOT NULL,
    mosaic_image_path VARCHAR(255),
    masked_text LONGTEXT,
    object_labels_json LONGTEXT,
    dominant_colors_json LONGTEXT,
    created_at DATETIME NOT NULL
) CHARACTER SET utf8mb4;
