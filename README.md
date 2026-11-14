# 단디(DANDI) 백엔드

단국대학교 교내 분실물 통합 웹 서비스의 **Spring Boot API 서버**입니다.
신고 접수, 관리자 검수, 공개, QR 발급, 현장 인계 확인까지의 업무와 데이터를 처리합니다.

| 구분 | 링크 |
|------|------|
| 서비스 | https://www.dandilost.life (임시 운영 2026-11-06 ~ 2026-12-20) |
| 프론트엔드 저장소 | https://github.com/hprays/DANDI-lost-and-found-program |
| 결과보고서 PDF | (제출 전 링크 추가) |
| 포스터 PDF | (제출 전 링크 추가) |
| 시연 영상 | (제출 전 링크 추가) |

팀: 이정우(32213459), 이지학(32213564), 한산(32214796) · 단국대학교 소프트웨어학과

## 시스템 구조

![시스템 구조도](docs/architecture.png)

## 기술 스택

Java 17 · Spring Boot 3.4 · Spring Security · Spring Data JPA · MySQL(AWS RDS) · AWS S3 · Firebase Authentication · Google Cloud Vision · GitHub Actions(자동 배포)

## 주요 기능

- **인증**: 학교 Google 계정(Firebase)으로 로그인하고, 서버가 토큰과 `@dankook.ac.kr` 도메인을 확인합니다.
- **신고와 검수**: 학생 신고는 검수 대기로 접수하고, 관리자가 확인한 뒤 공개합니다.
- **QR 인계**: QR은 10분간 유효합니다. 관리자가 현장 확인 후 인증하면 물품, 신고, 이력이 한 트랜잭션에서 함께 바뀝니다. (행 잠금 사용)
- **이미지 분석**: Vision으로 객체와 색상을 뽑고, 신분증·카드 등의 민감 영역을 가립니다.
- **개인화**: 관심 물품, 관심 키워드, 서비스 내부 알림, 관리자 작업 이력

## 주요 API

| 영역 | 경로 |
|------|------|
| 인증·프로필 | `/api/auth/*`, `/api/users/me/*` |
| 신고·검수 | `/api/reports` |
| 공개 물품 | `/api/lost-items` |
| 수령 QR | `/api/pickup-passes` |
| 관심·알림 | `/api/users/favorites`, `/api/users/keywords`, `/api/notices` |
| 관리자 | `/api/admin/*`, `/api/uploads` |
| 안내·챗봇 | `/api/chatbot`, `/api/ai-guidance` |
| 상태 확인 | `GET /api/public/health` |

## 실행 방법

```bash
git clone https://github.com/CraneHak/DANDI_lost_and_found_program_management.git
cd DANDI_lost_and_found_program_management
cp .env.example .env     # 값을 채웁니다 (DB, AWS, Firebase, Vision, LLM)
./gradlew bootRun        # http://localhost:8080
```

- 필요: Java 17
- 테스트와 빌드: `./gradlew clean test bootJar`
- `main`에 푸시하면 GitHub Actions가 테스트, 빌드, 배포, 헬스체크를 하고 실패하면 이전 버전으로 되돌립니다.
- `.env`와 키 파일은 커밋하지 않습니다. 이 저장소는 Public입니다.

## AI 코딩 도구 사용 안내

이 저장소의 일부 코드와 문서는 AI 코딩 도구의 도움으로 작성했습니다. `AGENTS.md`는 AI 도구가 따를 작업 규칙입니다.
영역별 사용 내용과 직접 작성 비율은 결과보고서에 적습니다. (팀 확인 후 기입)

## 폴더 구조

| 폴더 | 설명 |
|------|------|
| `src/` | 서버 소스(`main`)와 테스트(`test`), DB 스키마 SQL |
| `.github/workflows/` | 자동 배포 워크플로 |
| `scripts/` | EC2 배포 스크립트, DB 정리 SQL |
| `docs/` | README용 구조도 |
| `crawler/` | 초기에 시험한 Python 스크립트 (서버와 별개, 배포 제외) |
| `dandi-pages/` | 개발 초기의 화면 시안과 계획·발표 자료 |
