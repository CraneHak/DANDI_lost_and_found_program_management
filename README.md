# 단디(DANDI) 백엔드

> 단국대학교 교내 분실물 통합 웹 서비스의 **Spring Boot API 서버**입니다.
> 신고 접수 → 관리자 검수 → 공개 → QR 발급 → 현장 인계 확인까지의 업무와 데이터를 처리합니다.

## 제출 자료

| 구분 | 링크 |
|------|------|
| 서비스 | https://www.dandilost.life (임시 운영 2026-11-06 ~ 2026-12-20) |
| API 서버 | https://api.dandilost.life (`GET /api/public/health`) |
| 프론트엔드 저장소 | https://github.com/hprays/DANDI-lost-and-found-program |
| 백엔드 저장소 (이 저장소) | https://github.com/CraneHak/DANDI_lost_and_found_program_management |
| 프로젝트 결과보고서 PDF | (제출 전 링크 추가) |
| 포스터 PDF | (제출 전 링크 추가) |
| 시연 영상 | (제출 전 링크 추가) |

팀: 이정우(32213459), 이지학(32213564), 한산(32214796) · 단국대학교 소프트웨어학과

---

## 서비스 흐름

```
학생 신고 ─▶ 검수 대기(pending) ─▶ 관리자 검수·공개 ─▶ 공개 물품(lost_item)
                                                          │
          찾는 학생이 검색·상세 확인 ◀────────────────────┘
                     │
                     ▼
        QR 발급(10분 유효) ─▶ 현장에서 신분·물품 특징 확인 ─▶ 관리자 QR 인증
                                                          │
                                  물품 RECEIVED + 신고 picked_up + 수령 이력 (한 트랜잭션)
```

- 신고 상태와 물품 상태는 **서로 다른 값**으로 관리합니다. QR을 발급하는 것만으로 인계가 끝나지 않습니다.
- 실제 인계는 현장에서 사람이 확인합니다. 서버의 행 잠금과 트랜잭션은 중복 처리를 막는 장치이며 현장 확인을 대신하지 않습니다.

## 시스템 구조

![시스템 구조도](docs/architecture.png)

| 구성 | 역할 |
|------|------|
| Vercel | 프론트엔드 (Next.js) |
| AWS EC2 | 이 서버 (Spring Boot, systemd) |
| AWS RDS (MySQL) | 업무 데이터 (SSL 연결, `ddl-auto=validate`) |
| AWS S3 | 물품 사진 (파일명은 무작위로 저장) |
| Firebase Authentication | 학교 Google 계정 로그인, ID 토큰 발급 |
| Google Cloud Vision | 사진의 객체·색상 추출, 문서 OCR, 민감 영역 가림 |
| LLM 호환 API | 백엔드 안내·챗봇 API (모델과 주소는 환경변수) |

## 기술 스택

| 분류 | 기술 | 비고 |
|------|------|------|
| 언어 / 프레임워크 | Java 17, Spring Boot 3.4, Gradle | |
| 웹 / 검증 | Spring Web, Bean Validation | |
| 데이터 | Spring Data JPA, MySQL 8 (RDS) | 운영은 `ddl-auto=validate`. 스키마 SQL은 `src/main/resources/sql/` |
| 인증 / 권한 | Spring Security, Firebase Admin SDK | `@dankook.ac.kr` 도메인과 이메일 인증 여부 확인 |
| 파일 | AWS SDK S3 | 사진 형식만 허용 |
| 이미지 분석 | Google Cloud Vision | |
| 배포 | GitHub Actions, S3, AWS SSM | 헬스체크 실패 시 자동 롤백 |

## 코드 구조

`src/main/java/org/example/`

| 패키지 | 내용 |
|--------|------|
| `auth` | Firebase 토큰 필터와 검증, 보안 설정, 사용자 프로필·역할 |
| `report` | 신고 접수와 검수 상태 (`pending` → `resolved` → `picked_up`) |
| `controller`, `service`, `entity`, `repository` | 공개 물품(`lost_item`)과 카테고리, 사진 업로드, S3 |
| `pickup` | QR 발급·인증, 수령 이력(`collection_log`) |
| `vision` | Vision 분석과 가림 처리 이미지 저장 |
| `favorite`, `keyword`, `notice` | 관심 물품, 관심 키워드, 서비스 내부 알림 |
| `adminlog` | 관리자 작업 이력 |
| `chatbot`, `aiguidance`, `ai` | LLM 호출과 호출 제한 정책 |
| `common` | 공통 예외 처리, 시간 처리, 이미지 URL |

데이터는 11개 테이블입니다: `app_user`, `category`, `lost_item`, `report`, `pickup_pass`, `collection_log`, `lost_item_analysis`, `keyword`, `favorite_item`, `notice`, `admin_action_log`.
`lost_item.reportId`처럼 ID 값으로 잇는 연결과 JPA 연관관계는 구분해서 설계했습니다.

## 인증과 권한

1. 사용자가 학교 Google 계정으로 Firebase에 로그인합니다.
2. 클라이언트가 ID 토큰을 `Authorization: Bearer ...`로 보냅니다.
3. 서버가 토큰, 이메일 인증 여부, `@dankook.ac.kr` 도메인을 확인합니다.
4. 관리자는 환경변수(`FIREBASE_ADMIN_UIDS`, `FIREBASE_ADMIN_EMAILS`)에 등록된 계정입니다.

| 구분 | 허용 업무 |
|------|-----------|
| 일반 사용자 | 신고 등록, 본인 신고 조회, 본인 찜·키워드·알림, QR 발급 |
| 관리자 | 신고 검수, 물품 등록·수정·삭제, 사진 업로드와 분석, QR 최종 인증, 관리자 이력 조회 |

학교 이메일 인증은 계정 소유를 확인하는 절차입니다. 학사 시스템에서 재학 여부를 조회하지는 않습니다.

## 주요 API

| 메서드와 경로 | 설명 |
|---------------|------|
| `GET /api/public/health` | 상태 확인 |
| `POST /api/auth/login`, `GET /api/auth/me` | 로그인 연계, 현재 사용자 |
| `GET /api/users/me`, `PATCH /api/users/me/profile`, `PATCH /api/users/me/preferences` | 프로필, 알림 선호 |
| `GET·POST /api/reports` | 신고 조회·등록 |
| `PATCH /api/reports/{id}/status`, `PATCH /api/reports/{id}` | 관리자 검수 (상태, 내용) |
| `GET /api/lost-items`, `GET /api/lost-items/{id}` | 공개 물품 목록, 상세 |
| `POST /api/lost-items`, `PATCH·DELETE /api/lost-items/{id}` | 관리자 물품 등록·변경 |
| `POST /api/pickup-passes`, `GET /api/pickup-passes/me` | QR 발급, 내 QR 목록 |
| `POST /api/pickup-passes/verify` | 관리자 최종 수령 인증 |
| `GET /api/admin/pickup-passes`, `GET /api/admin/action-logs` | 수령 코드, 작업 이력 |
| `GET /api/users/favorites`, `PUT·DELETE /api/users/favorites/{lostItemId}` | 관심 물품 |
| `GET·POST /api/users/keywords`, `DELETE /api/users/keywords/{id}` | 관심 키워드 |
| `GET·DELETE /api/notices`, `PATCH /api/notices/{id}/read`, `DELETE /api/notices/{id}` | 알림 |
| `POST /api/admin/vision/analyze`, `POST /api/uploads` | 이미지 분석, 사진 업로드 |
| `POST /api/chatbot`, `POST /api/ai-guidance` | 챗봇, 안내 (프론트 서버 라우트에도 같은 이름이 있어 호스트로 구분) |

---

## 설계 포인트

### QR 인증은 한 트랜잭션에서 행 잠금과 함께 처리
- QR 발급은 공개된 물품에만 허용합니다. 이미 수령된 물품은 409로 거부합니다. 발급만으로 물품 상태를 바꾸지 않습니다.
- 인증할 때 QR과 물품 행을 비관적 잠금(`PESSIMISTIC_WRITE`)으로 잡고 검사합니다. 만료, 사용 여부, 이미 수령된 물품, 같은 물품의 다른 QR을 확인합니다.
- 통과하면 물품 `RECEIVED`, 연결 신고 `picked_up`, QR 사용 시각과 인증자, 수령 이력, 관리자 이력, 알림을 함께 반영합니다.
- QR 유효시간은 10분입니다. 같은 사용자와 물품에 유효한 QR이 있으면 기존 QR을 돌려줍니다.

### 이미지 분석과 개인정보
- 신분증, 카드처럼 이름과 번호가 보이는 사진은 Vision OCR 결과로 민감 영역을 가린 이미지를 만듭니다.
- 원본은 메모리에서 처리합니다. 분석 결과는 `lost_item_analysis`에 따로 기록합니다.
- 업로드는 사진 형식만 받고 S3 파일명은 무작위로 저장합니다. 원래 파일명에 들어 있을 수 있는 이름과 학번이 경로에 남지 않습니다.
- 자동 분석을 거치지 않는 사진도 관리자가 검수하면서 공개 범위를 확인합니다.

### 챗봇 호출 제한
- 질문 길이, 분·시간·일 단위 호출 횟수, 위반 시 차단 시간을 환경변수로 조정합니다. (`CHATBOT_RATE_LIMIT_*`, `CHATBOT_SANCTION_*`)

---

## 트러블슈팅

실제 사용 흐름을 점검하면서 찾아 고친 문제입니다. 검토 기록은 팀 내부 문서(코드 리뷰, 수정 내역)로 남겼습니다.

### 1. 신고 ID와 물품 ID가 섞여 엉뚱한 항목이 선택됨
- **문제**: 신고 ID(`reportId`)와 물품 ID(`id`)를 같은 값처럼 비교했습니다. 물품 20을 삭제하면 물품 10이 대상이 되는 경우가 있었습니다.
- **해결**: 같은 종류의 ID끼리만 비교하도록 규칙을 정리했습니다. 승인 직후 임시 카드는 `report-<신고ID>` 형식으로 구분했습니다. QR 응답의 `reportId`에는 실제 신고 ID를 담고, 인증 응답에 `lostItemId`를 추가했습니다.
- **결과**: 실제 DB에서 "한 물품의 신고 ID가 다른 물품의 물품 ID와 같은" 사례가 16건 있었습니다. 수정 후 `/lost/70`과 `/lost/82`가 각각 올바른 물품으로 열립니다.

### 2. 저장이 실패해도 화면은 성공으로 표시됨
- **문제**: 습득 처리, 물품 수정·삭제, 신고 삭제가 서버 응답 전에 화면을 바꾸고 성공 메시지를 보였습니다. 실패한 요청의 오류도 버렸습니다.
- **해결**: 서버 응답을 받은 뒤에만 화면을 바꿉니다. 실패하면 오류 사유를 보이고 입력 내용을 유지합니다. 시간 초과 시에는 목록을 다시 불러와 확인하도록 안내합니다.
- **결과**: 요청을 일부러 실패시켜도 저장, 삭제, 읽음 처리 모두 "실패했습니다" 안내가 나오고 화면과 서버 상태가 어긋나지 않았습니다.

### 3. QR 인증의 상태 불일치와 중복 처리
- **문제**: QR을 발급하면 이미 수령된 물품도 다시 수령 대기 상태가 됐습니다. 인증은 물품만 바꾸고 연결 신고는 그대로였습니다. `findByTokenForUpdate`라는 이름의 조회에 실제 잠금이 없었습니다.
- **해결**: 위 "설계 포인트"의 구조로 바꿨습니다. 잠금 조회를 추가하고 물품, 신고, QR, 이력을 한 트랜잭션에서 반영합니다. 내 QR 목록은 브라우저가 아니라 서버(`GET /api/pickup-passes/me`)에서 조회합니다.
- **결과**: 같은 토큰으로 다시 인증하면 409, 수령된 물품에 QR을 다시 발급해도 409였습니다. 인증 후 물품 `RECEIVED`, 신고 `picked_up`, 알림 1건이 서버에서 확인됐습니다.

### 4. 만료된 QR을 다시 발급할 수 없음
- **문제**: 화면이 유효기간을 보지 않아 만료된 QR도 "확인 대기 중"으로 보이고 재발급 버튼이 없었습니다.
- **해결**: QR 상태를 `active`, `expired`, `used`로 계산하고, 화면이 열려 있어도 15초마다 다시 계산합니다.
- **결과**: 새로고침 없이 만료 안내와 재발급 버튼으로 바뀝니다. 만료된 토큰으로 인증하면 400으로 거부됩니다.

### 5. 습득 시각이 0시 0분으로 저장됨
- **문제**: 프론트가 보내는 UTC ISO 시각(`...Z`)을 백엔드가 해석하지 못해 날짜만 남겼습니다. (18:30 → 00:00)
- **해결**: 시간대가 붙은 시각을 한국 시각(Asia/Seoul)으로 변환해 저장합니다. (`LostItemService.parseStoredDate`)
- **결과**: `2026-10-10T18:30:00`으로 저장되고 화면에도 18시 30분으로 표시됩니다.

---

## 테스트와 배포

### 자동 테스트
```bash
./gradlew clean test bootJar
```
- 2026-11-14 최종 점검에서 65개 테스트가 모두 성공했습니다. (실패, 오류, 건너뜀 0)
- 인증 토큰과 학교 이메일 조건, 관리자 전용 접근, 본인 데이터 범위, 찜·알림 수신 대상, QR 만료·사용·중복 인증, 이미지 저장을 검사합니다.
- **한계**: 자동 테스트는 모의 객체를 쓰며 실제 DB와 외부 서비스(Firebase, Vision, LLM) 연결은 포함하지 않습니다. 외부 연동은 팀의 최종 기능 시연으로 확인했습니다.

### 자동 배포
`main`에 푸시하면 `.github/workflows/deploy.yml`이 실행됩니다.

```
테스트 + 빌드 ─▶ jar를 S3에 업로드 ─▶ SSM으로 EC2에서 교체 ─▶ 헬스체크 ─▶ 실패 시 이전 버전으로 롤백
```

- 테스트가 실패하면 배포되지 않습니다. 결과는 저장소의 Actions 탭에서 봅니다.
- `**.md`와 `crawler/**`만 바뀐 푸시는 배포하지 않습니다.
- 운영은 `ddl-auto=validate`입니다. 표나 칸을 바꾸면 `src/main/resources/sql/`에 SQL을 함께 올리고 RDS에 먼저 반영한 뒤 푸시합니다.

---

## 로컬 실행

필요: Java 17, (선택) Docker Desktop

```bash
git clone https://github.com/CraneHak/DANDI_lost_and_found_program_management.git
cd DANDI_lost_and_found_program_management
```

1. `gradle.properties`에 Java 17 경로를 지정합니다. (필요한 경우)
   ```properties
   org.gradle.java.installations.paths=C:\\Users\\<your-user>\\AppData\\Local\\Programs\\Eclipse Adoptium\\jdk-17.0.x.x-hotspot
   ```
2. `.env.example`을 참고해 `.env`를 만듭니다. 핵심 변수:
   `DB_PASSWORD`, `AWS_ACCESS_KEY`, `AWS_SECRET_KEY`, `S3_BUCKET_NAME`, `CORS_ALLOWED_ORIGINS`,
   `FIREBASE_ADMIN_SDK_PATH`, `FIREBASE_ALLOWED_DOMAIN`, `FIREBASE_ADMIN_UIDS`, `FIREBASE_ADMIN_EMAILS`,
   `GOOGLE_APPLICATION_CREDENTIALS`, `LLM_API_KEY`, `LLM_MODEL`, `LLM_BASE_URL`
3. 실행합니다.
   ```bash
   ./gradlew bootRun
   ```
   `Started Main`이 보이면 정상입니다. 기본 주소는 `http://localhost:8080`, 상태 확인은 `GET /api/public/health`입니다.

로컬 DB가 필요하면 `docker-compose up -d`로 MySQL 8을 띄웁니다. 기본 설정은 AWS RDS를 가리키므로 로컬 DB를 쓰려면 `application-local.properties`를 확인하세요.

### 저장소 운영 기준
- 이 저장소는 Public입니다. `.env`, 서비스 계정 JSON, 개인 키는 커밋하지 않습니다.
- 키 노출이 의심되면 즉시 재발급하고 교체합니다.
- 배포 설정(`.github/workflows/`, `scripts/deploy/`)은 관리자와 상의 후 바꿉니다.

---

## AI 코딩 도구 사용 안내

> 학과 공지(2026 캡스톤 페스티벌 FAQ Q9)에 따라 AI 코딩 도구의 사용 범위를 밝힙니다.
> 아래 빈칸은 팀원이 **실제 내용으로** 채워야 합니다. 추정치는 "추정"이라고 표시하세요.

- 이 저장소의 일부 코드와 문서는 AI 코딩 도구의 도움으로 작성했습니다. `AGENTS.md`는 AI 도구가 이 저장소에서 따를 규칙을 정리한 파일입니다.
- 사용한 도구: (팀 확인 후 기입)

| 영역 | 직접 작성한 내용 | AI를 사용한 내용 | AI를 사용한 이유 | 팀원이 검토·수정한 내용 |
|------|------------------|------------------|------------------|--------------------------|
| 인증·권한 | | | | |
| 신고·검수·물품 API | | | | |
| QR 발급·인증 | | | | |
| 이미지 분석·업로드 | | | | |
| 알림·찜·키워드 | | | | |
| 테스트 | | | | |
| 배포 자동화 | | | | |
| 문서(README·보고서) | | | | |

직접 작성한 코드의 전체 비율: (팀 확인 후 기입, 산정 방법도 함께 적기)

---

## 저장소의 다른 폴더

| 폴더·파일 | 설명 |
|-----------|------|
| `dandi-pages/` | 개발 초기의 화면 시안(HTML)과 발표·계획 자료. 서비스 서버와 무관한 참고 자료입니다. |
| `crawler/` | 초기에 시험한 별도 Python 스크립트입니다. 이 서버에 포함되지 않고 배포 대상도 아닙니다. |
| `scripts/` | 배포 스크립트와 DB 정리용 SQL |
| `JIHAK_API_BACKEND_ONLY_CHANGES_2026-05-17.md` | 신고·알림·키워드 API 개선 내역 기록 |
| `AGENTS.md` | AI 도구용 작업 규칙 |
