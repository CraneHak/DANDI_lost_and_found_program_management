-- 신고자 이름이 메모 끝에 붙어 공개 물품 설명(lost_item.contact)으로 복사된 기존 데이터 정리 (MySQL 8).
-- 이전 형식: "신고자: 이름" 또는 "메모 (신고자: 이름)". 실행 전 SELECT로 대상 확인 권장.
--   SELECT id, contact FROM lost_item WHERE contact LIKE '%신고자:%';

UPDATE lost_item
SET contact = NULLIF(TRIM(REGEXP_REPLACE(contact, '[[:space:]]*[(]?신고자: [^)]*[)]?$', '')), '')
WHERE contact LIKE '%신고자:%';

UPDATE report
SET memo = NULLIF(TRIM(REGEXP_REPLACE(memo, '[[:space:]]*[(]?신고자: [^)]*[)]?$', '')), '')
WHERE memo LIKE '%신고자:%';
