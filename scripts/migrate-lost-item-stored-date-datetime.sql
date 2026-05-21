-- lost_item.stored_date: DATE → DATETIME (습득 시·분 저장)
-- ddl-auto=validate 환경에서 엔티티 변경 전 RDS에서 1회 실행
ALTER TABLE lost_item
    MODIFY COLUMN stored_date DATETIME NULL;
