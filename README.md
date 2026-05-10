# DANDI Backend

단국대학교(죽전) 분실물 통합 관리 서비스 백엔드입니다.

---

## ⚡ 개인 설정 필수 항목 (처음 실행 전 반드시 완료)

> 아래 4가지는 git에 올라가지 않으므로, 클론 후 **각자 직접 생성·설정**해야 합니다.

---

### 1. `gradle.properties` — Java 17 경로 지정

프로젝트 루트에 `gradle.properties` 파일을 생성합니다.

```properties
org.gradle.java.installations.paths=C:\\Users\\{사용자명}\\AppData\\Local\\Programs\\Eclipse Adoptium\\jdk-17.0.x.x-hotspot
```

> `{사용자명}`과 버전 번호를 실제 설치 경로로 수정하세요. 경로 구분자는 `\\`를 사용합니다.

---

### 2. `.env` — 환경변수 파일

프로젝트 루트에 `.env` 파일을 생성하고 아래 값을 채웁니다. (팀 노션 참고)

```dotenv
# AWS
AWS_ACCESS_KEY=<팀 노션 참고>
AWS_SECRET_KEY=<팀 노션 참고>
S3_BUCKET_NAME=dandi-lost-items

# DB
DB_PASSWORD=<팀 노션 참고>

# Firebase Admin SDK
FIREBASE_ADMIN_SDK_PATH=C:\Users\{사용자명}\.secrets\firebase\myauth.json
FIREBASE_ALLOWED_DOMAIN=dankook.ac.kr
FIREBASE_ADMIN_UIDS=<팀 노션 참고>
FIREBASE_ADMIN_EMAILS=<팀 노션 참고>

# Google Cloud Vision API
GOOGLE_APPLICATION_CREDENTIALS=C:\Users\{사용자명}\.secrets\cloudvisionapi\cloudvisionapi_cash.json

# LLM (Gemini)
LLM_API_KEY=<팀 노션 참고>
LLM_MODEL=gemini-1.5-flash
LLM_BASE_URL=https://generativelanguage.googleapis.com/v1beta/openai

# 챗봇 제한
CHATBOT_RATE_LIMIT_MINUTE=12
CHATBOT_RATE_LIMIT_HOUR=20
CHATBOT_RATE_LIMIT_DAY=30
CHATBOT_SANCTION_FIRST_BLOCK_MINUTES=15
CHATBOT_SANCTION_SECOND_BLOCK_HOURS=24
```

---

### 3. Firebase Admin SDK JSON 파일 배치

팀 노션에서 Firebase Admin SDK 서비스 계정 키(`myauth.json`)를 다운로드하고, `.env`의 `FIREBASE_ADMIN_SDK_PATH`에 지정한 경로에 배치합니다.

```
예) C:\Users\{사용자명}\.secrets\firebase\myauth.json
```

---

### 4. Google Cloud Vision API 자격증명 JSON 배치 + 시스템 환경변수 등록

1. 팀 노션에서 Cloud Vision API 자격증명 JSON(`cloudvisionapi_cash.json`)을 다운로드해 지정 경로에 배치합니다.

   ```
   예) C:\Users\{사용자명}\.secrets\cloudvisionapi\cloudvisionapi_cash.json
   ```

2. 시스템 환경변수에 `GOOGLE_APPLICATION_CREDENTIALS`를 등록합니다 (OS 재시작 또는 터미널 재실행 필요).

   ```powershell
   # PowerShell (관리자)
   [System.Environment]::SetEnvironmentVariable("GOOGLE_APPLICATION_CREDENTIALS", "C:\Users\{사용자명}\.secrets\cloudvisionapi\cloudvisionapi_cash.json", "User")
   ```

---

---

## 브랜치 구조

| 브랜치 | 설명 |
|--------|------|
| `main` | Spring Boot API 서버 (통합 브랜치) |
| `database` | AWS RDS MySQL 연결 설정 |
| `crawling` | 단국대 분실물 게시판 크롤러 + S3 이미지 업로드 |

---

## 개발 환경 요구사항

- Java 17
- Docker Desktop
- Python 3.10+

---

## 환경 구성 순서

### 1. Java 17 설치

[https://adoptium.net](https://adoptium.net) 접속 후 **Temurin 17** 다운로드 및 설치

> 다른 버전의 Java(예: Java 25)가 이미 설치되어 있어도 Java 17을 **추가 설치**하면 됩니다. 기존 버전을 지울 필요 없습니다.

설치 확인:
```bash
java -version
# java version "17.x.x" 출력되면 정상
```

---

### 2. Docker Desktop 설치 및 실행

[https://www.docker.com/products/docker-desktop](https://www.docker.com/products/docker-desktop) 접속 후 운영체제에 맞는 버전 다운로드 및 설치

설치 후 **Docker Desktop을 실행**합니다 (백그라운드에서 실행 중이어야 함). 로그인은 필요하지 않습니다.

설치 확인:
```bash
docker -v
# Docker version xx.x.x 출력되면 정상
```

---

### 3. 프로젝트 클론

```bash
git clone https://github.com/CraneHak/DANDI_Backend.git
cd DANDI_Backend
```

---

### 4. gradle.properties 설정 (필수)

> Java 17이 설치되어 있어도 JAVA_HOME이 다른 버전으로 잡혀 있으면 Gradle이 Java 17을 찾지 못합니다.
> 아래 과정을 통해 프로젝트 루트에 `gradle.properties` 파일을 직접 만들어주세요.
> (이 파일은 `.gitignore`에 등록되어 있어 개인 로컬에서만 유지됩니다)

**Step 1 — Java 17 설치 경로 확인**

```bash
where java
```

출력 예시:
```
C:\Program Files\Java\jdk-25\bin\java.exe
C:\Users\사용자명\AppData\Local\Programs\Eclipse Adoptium\jdk-17.0.x.x-hotspot\bin\java.exe
```

`jdk-17`이 포함된 경로에서 `\bin\java.exe` 부분을 제외한 경로가 필요합니다.

**Step 2 — gradle.properties 파일 생성**

프로젝트 루트(`DANDI_Backend/`)에 `gradle.properties` 파일을 생성하고 아래 내용을 작성합니다:

```properties
org.gradle.java.installations.paths=C:\\Users\\{사용자명}\\AppData\\Local\\Programs\\Eclipse Adoptium\\jdk-17.0.x.x-hotspot
```

> `{사용자명}`과 버전 번호는 Step 1에서 확인한 실제 경로로 수정하세요.
> 경로 구분자는 반드시 `\\` (역슬래시 두 개)를 사용해야 합니다.

---

### 5. .env 파일 설정 (필수)

프로젝트 루트(`DANDI_Backend/`)에 `.env` 파일을 생성하고 아래 내용을 붙여넣습니다:

```
AWS_ACCESS_KEY=<팀 노션 참고>
AWS_SECRET_KEY=<팀 노션 참고>
S3_BUCKET_NAME=dandi-lost-items
DB_PASSWORD=<팀 노션 참고>
```

> `.env` 파일은 `.gitignore`에 등록되어 있어 git에 올라가지 않습니다.

---

### 6. MySQL 실행 (선택)

> 실제 서비스는 AWS RDS를 사용합니다. 로컬 MySQL은 RDS 없이 독립적으로 테스트할 때만 활용하세요.

```bash
docker-compose up -d
```

처음 실행 시 MySQL 이미지를 다운로드하므로 시간이 걸릴 수 있습니다.

실행 확인:
```bash
docker ps
# dandi_mysql 컨테이너가 실행 중이면 정상
```

---

### 7. Spring Boot 실행

```bash
./gradlew bootRun
```

아래 로그가 출력되면 정상 실행된 것입니다:
```
Started Main in x.xxx seconds
```

> 브라우저에서 **404 Whitelabel Error Page**가 뜨는 것은 정상입니다. 루트 엔드포인트가 없기 때문이며, 서버는 정상 동작 중입니다.

---

## REST API

서버 기본 URL: `http://localhost:8080`

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/api/lost-items` | 전체 분실물 목록 조회 |
| GET | `/api/lost-items/{id}` | 단일 분실물 조회 |
| POST | `/api/lost-items` | 분실물 등록 (multipart/form-data) |
| DELETE | `/api/lost-items/{id}` | 분실물 삭제 |

**POST 요청 파라미터:**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| itemName | String | 물건 이름 |
| foundLocation | String | 발견 위치 |
| storedLocation | String | 보관 위치 |
| storedDate | String | 보관 날짜 (yyyy-MM-dd) |
| contact | String | 연락처 |
| color | String | 색상 |
| itemType | String | 물건 종류 |
| image | MultipartFile | 이미지 파일 (선택) |

---

## DB 정보

### 로컬 개발용 (Docker MySQL)

| 항목 | 값 |
|------|-----|
| Host | localhost |
| Port | 3306 |
| DB 이름 | dandidb |
| 사용자 | dandi |
| 비밀번호 | dandi1234 |

### 공용 DB (AWS RDS)

| 항목 | 값 |
|------|-----|
| Host | dandi-db.cdocgw0s68cj.ap-northeast-2.rds.amazonaws.com |
| Port | 3306 |
| DB 이름 | dandidb |
| 사용자 | dandi |
| 비밀번호 | 팀 노션 참고 |

현재 저장된 데이터: 단국대(죽전) 분실물 게시판 크롤링 데이터 **48건**

#### DB 스키마

```sql
CREATE TABLE lost_item (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    post_no         INT UNIQUE,
    item_name       VARCHAR(255),
    found_location  VARCHAR(255),
    stored_location VARCHAR(255),
    stored_date     DATE,
    contact         VARCHAR(100),
    color           VARCHAR(50),
    item_type       VARCHAR(255),
    image_url       VARCHAR(500)
) CHARACTER SET utf8mb4;
```

---

## 크롤러 (crawler/)

단국대 포털 분실물 게시판에서 분실물 데이터를 수집합니다.

### 환경 설정

```bash
cd crawler
pip install -r requirements.txt
playwright install chromium
```

`crawler/.env` 파일을 생성하고 아래 내용을 작성합니다:

| 변수 | 설명 |
|------|------|
| DANKOOK_ID | 단국대 포털 학번 |
| DANKOOK_PW | 단국대 포털 비밀번호 |
| DB_HOST | RDS 호스트 |
| DB_PORT | DB 포트 (기본 3306) |
| DB_NAME | DB 이름 |
| DB_USER | DB 사용자 |
| DB_PASSWORD | DB 비밀번호 |
| AWS_ACCESS_KEY | AWS IAM 액세스 키 |
| AWS_SECRET_KEY | AWS IAM 시크릿 키 |
| AWS_REGION | AWS 리전 (ap-northeast-2) |
| S3_BUCKET_NAME | S3 버킷 이름 |

### 실행 순서

**1단계 — 분실물 데이터 및 이미지 크롤링:**
```bash
python crawl.py
```

**2단계 — S3 이미지 업로드:**
```bash
python upload_s3.py
```

---

## 문제 해결 (Troubleshooting)

### Gradle이 Java 17을 찾지 못하는 경우

**증상:**
```
Cannot find a Java installation on your machine matching: {languageVersion=17}
```

**해결:** 위 [gradle.properties 설정](#4-gradleproperties-설정-필수) 참고

---

### `gradlew.bat: command not found` 오류

**증상:**
```
bash: gradlew.bat: command not found
```

**해결:** bash 환경(WSL, Git Bash 등)에서는 `.bat` 대신 아래 명령을 사용합니다:
```bash
./gradlew bootRun
```

---

## 협업 시 작업 방법

```bash
# 작업 시작 전 최신 코드 받기
git pull origin main

# 기능 개발 시 브랜치 생성
git checkout -b feature/기능명

# 작업 완료 후 푸시
git push origin feature/기능명
```

이후 GitHub에서 Pull Request를 생성하여 main 브랜치에 병합합니다.
