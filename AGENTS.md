# DANDI 백엔드 작업 규칙 (Codex/AI 도구용)

Spring Boot 3.4 · Java 17 · Gradle. 운영 서버: https://api.dandilost.life (EC2), DB: AWS RDS MySQL.

## main에 푸시하면 바로 운영 서버에 배포된다
- `.github/workflows/deploy.yml`이 테스트 → 빌드 → EC2 교체 → 헬스체크까지 자동으로 한다. 헬스체크가 실패하면 이전 버전으로 자동 롤백된다.
- 푸시 전에 반드시 `./gradlew clean test bootJar`가 로컬에서 통과해야 한다. 테스트가 실패하면 배포되지 않는다.
- 결과는 GitHub 저장소의 Actions 탭에서 확인한다. 빨간색이면 운영은 이전 버전 그대로다.
- 수동 재배포: Actions → "백엔드 배포" → Run workflow.

## DB 표/칸을 바꾸는 변경은 먼저 알린다
- 운영은 `spring.jpa.hibernate.ddl-auto=validate`라서 엔티티와 RDS 구조가 다르면 서버가 뜨지 않는다(→ 자동 롤백).
- 엔티티에 표/칸을 추가·변경하면 `src/main/resources/sql/`에 SQL 파일을 함께 커밋하고, 푸시 전에 저장소 관리자(CraneHak)에게 RDS 반영을 요청한다. RDS 반영이 끝난 뒤 푸시한다.

## 하지 말 것
- `.env`, 키 파일(`*.json` 서비스 계정, `*.pem`), 비밀번호를 커밋하지 않는다.
- `.github/workflows/`와 `scripts/deploy/`는 배포 설정이라 관리자와 상의 없이 바꾸지 않는다.
- 운영 DB에 직접 데이터 삭제/DDL을 실행하지 않는다.

## 커밋
- 제목은 한국어로 무엇을 바꿨는지 평범하게 쓴다 (예: `신고 목록 정렬 기준 수정`).
