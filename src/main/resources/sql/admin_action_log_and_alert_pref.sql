-- 2026-11-05: 관리자 작업 이력 테이블 + 키워드 알림 받기 설정 칸
-- spring.jpa.hibernate.ddl-auto=validate 라서 이 SQL을 먼저 실행한 뒤 새 서버를 띄워야 한다.
-- (로컬 개발 DB도 같은 SQL을 실행해야 서버가 뜬다)

CREATE TABLE IF NOT EXISTS admin_action_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    action VARCHAR(30) NOT NULL,
    message VARCHAR(500) NOT NULL,
    target_id VARCHAR(64),
    actor_uid VARCHAR(128),
    actor_email VARCHAR(255),
    created_at DATETIME NOT NULL
) CHARACTER SET utf8mb4;

CREATE INDEX idx_admin_action_log_created_at ON admin_action_log (created_at);

-- 기존 사용자는 모두 '켜짐'으로 시작
ALTER TABLE app_user
    ADD COLUMN keyword_alert_enabled TINYINT(1) NOT NULL DEFAULT 1;
