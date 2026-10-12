-- 관심 물품 (기기 간 동기화). ddl-auto=validate 이므로 서버 배포 전에 DB에 먼저 실행한다.
CREATE TABLE IF NOT EXISTS favorite_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    lost_item_id INT NOT NULL,
    requester_uid VARCHAR(128) NOT NULL,
    created_at DATETIME NOT NULL,
    UNIQUE KEY uq_favorite_item_uid_item (requester_uid, lost_item_id)
) CHARACTER SET utf8mb4;
