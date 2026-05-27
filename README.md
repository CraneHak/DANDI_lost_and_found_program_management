# DANDI Backend

단국대학교(죽전) 분실물 통합 관리 서비스 백엔드입니다.

## 개요

- Spring Boot API 서버
- Firebase 인증 기반 사용자/관리자 흐름
- AWS S3 연동 이미지 업로드
- 크롤러(`crawler/`)를 통한 분실물 데이터 수집

## 공개 저장소 운영 원칙

- 이 저장소는 Public입니다. 비밀정보(API 키, DB 비밀번호, 서비스 계정 JSON)는 절대 커밋하지 않습니다.
- `.env`, `crawler/.env`, `*.pem`, 서비스 계정 JSON은 로컬에만 두고 git 추적에서 제외합니다.
- 키/비밀번호가 노출되었거나 의심되면 삭제보다 먼저 재발급(rotate)합니다.

## 요구사항

- Java 17
- Python 3.10+
- Docker Desktop (선택: 로컬 MySQL 테스트 시)

## 빠른 시작

### 1) 프로젝트 클론

```bash
git clone https://github.com/CraneHak/DANDI_Backend.git
cd DANDI_Backend
```

### 2) `gradle.properties` 생성 (권장)

프로젝트 루트에 `gradle.properties` 파일을 생성합니다.

```properties
org.gradle.java.installations.paths=C:\\Users\\<your-user>\\AppData\\Local\\Programs\\Eclipse Adoptium\\jdk-17.0.x.x-hotspot
```

### 3) 루트 `.env` 생성

`.env.example`을 참고해 프로젝트 루트에 `.env`를 생성합니다.

필수/주요 변수:

- `DB_PASSWORD`
- `AWS_ACCESS_KEY`
- `AWS_SECRET_KEY`
- `S3_BUCKET_NAME`
- `FIREBASE_ADMIN_SDK_PATH`
- `FIREBASE_ALLOWED_DOMAIN`
- `FIREBASE_ADMIN_UIDS`
- `FIREBASE_ADMIN_EMAILS`
- `GOOGLE_APPLICATION_CREDENTIALS`
- `LLM_API_KEY`
- `LLM_MODEL`
- `LLM_BASE_URL`
- `CHATBOT_RATE_LIMIT_MINUTE`
- `CHATBOT_RATE_LIMIT_HOUR`
- `CHATBOT_RATE_LIMIT_DAY`
- `CHATBOT_SANCTION_FIRST_BLOCK_MINUTES`
- `CHATBOT_SANCTION_SECOND_BLOCK_HOURS`

Windows 예시 경로:

```dotenv
FIREBASE_ADMIN_SDK_PATH=C:\Users\<your-user>\.secrets\firebase\myauth.json
GOOGLE_APPLICATION_CREDENTIALS=C:\Users\<your-user>\.secrets\cloudvisionapi\cloudvisionapi_cash.json
```

### 4) 백엔드 실행

```bash
./gradlew bootRun
```

정상 실행 시 `Started Main` 로그가 출력됩니다.

## 로컬 MySQL (선택)

기본 구성은 AWS RDS를 사용합니다. 로컬 DB로 독립 테스트가 필요할 때만 사용하세요.

```bash
docker-compose up -d
docker ps
```

## 크롤러 실행 (`crawler/`)

### 1) 의존성 설치

```bash
cd crawler
pip install -r requirements.txt
playwright install chromium
```

### 2) `crawler/.env` 생성

`crawler/.env.example`을 참고해 `crawler/.env`를 생성합니다.

주요 변수:

- `DANKOOK_ID`
- `DANKOOK_PW`
- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USER`
- `DB_PASSWORD`
- `AWS_ACCESS_KEY`
- `AWS_SECRET_KEY`
- `AWS_REGION`
- `S3_BUCKET_NAME`

### 3) 실행

```bash
python crawl.py
python upload_s3.py
```

## 주요 API

기본 URL: `http://localhost:8080`

- `GET /api/lost-items`
- `GET /api/lost-items/{id}`
- `POST /api/lost-items` (multipart/form-data)
- `DELETE /api/lost-items/{id}`

## 트러블슈팅

### Gradle이 Java 17을 찾지 못할 때

`gradle.properties`에 Java 17 설치 경로를 직접 지정했는지 확인하세요.

### bash 환경에서 `gradlew.bat` 오류가 날 때

WSL/Git Bash에서는 아래를 사용합니다.

```bash
./gradlew bootRun
```

## 협업 권장 흐름

```bash
git pull origin main
git checkout -b feature/<topic>
# 작업
git push origin feature/<topic>
```
