# DANDI Backend

단국대학교(죽전) 분실물 통합 관리 서비스 백엔드입니다.

---

## 브랜치 구조

| 브랜치 | 설명 |
|--------|------|
| `main` | Spring Boot API 서버 |
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

> 다른 버전의 Java가 이미 설치되어 있어도 Java 17을 **추가 설치**하면 됩니다.

설치 확인:
```bash
java -version
# java version "17.x.x" 출력되면 정상
```

---

### 2. Docker Desktop 설치 및 실행

[https://www.docker.com/products/docker-desktop](https://www.docker.com/products/docker-desktop) 접속 후 설치

설치 후 Docker Desktop을 실행합니다 (백그라운드 실행 중이어야 함).

---

### 3. 프로젝트 클론

```bash
git clone https://github.com/CraneHak/DANDI_Backend.git
cd DANDI_Backend
```

---

### 4. gradle.properties 설정 (필수)

> Java 17이 설치되어 있어도 JAVA_HOME이 다른 버전으로 잡혀 있으면 Gradle이 Java 17을 찾지 못합니다.
> `gradle.properties`는 `.gitignore`에 등록되어 있어 개인 로컬에서만 유지됩니다.

**Java 17 경로 확인:**
```bash
where java
# jdk-17이 포함된 경로 확인
```

**프로젝트 루트에 `gradle.properties` 생성:**
```properties
org.gradle.java.installations.paths=C:\\Users\\{사용자명}\\AppData\\Local\\Programs\\Eclipse Adoptium\\jdk-17.0.x.x-hotspot
```

---

### 5. MySQL 실행 (로컬 개발용)

```bash
docker-compose up -d
```

실행 확인:
```bash
docker ps
# dandi_mysql 컨테이너가 실행 중이면 정상
```

---

### 6. Spring Boot 실행

**로컬 DB (Docker MySQL):**
```bash
./gradlew bootRun
```

**프로덕션 DB (AWS RDS):**

PowerShell:
```powershell
$env:SPRING_PROFILES_ACTIVE="prod"
$env:DB_PASSWORD="비밀번호"
$env:AWS_ACCESS_KEY="키"
$env:AWS_SECRET_KEY="시크릿키"
$env:S3_BUCKET_NAME="dandi-lost-items"
./gradlew bootRun
```

정상 실행 시:
```
Started Main in x.xxx seconds
```

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
| 비밀번호 | 팀 채널 참고 |

현재 저장된 데이터: 단국대(죽전) 분실물 게시판 크롤링 데이터 **50건**

---

## 크롤러 (crawler/)

단국대 포털 분실물 게시판(`https://portal.dankook.ac.kr/p/LOST01`)에서 분실물 데이터를 수집합니다.

### 환경 설정

```bash
cd crawler
pip install -r requirements.txt
playwright install chromium
```

`crawler/.env.example`을 복사하여 `crawler/.env` 생성 후 값을 채웁니다:
```bash
cp .env.example .env
# .env 파일을 열어 실제 값 입력
```

필요한 환경 변수:

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
- 단국대 포털에 로그인하여 분실물 게시글 수집
- 게시글 번호(post_no) 기준 증분 수집 (이미 수집된 것은 건너뜀)
- 이미지는 `crawler/{id}/` 폴더에 저장

**2단계 — S3 이미지 업로드:**
```bash
python upload_s3.py
```
- `image_url`이 없는 항목의 이미지를 S3에 업로드
- DB의 `image_url` 컬럼 업데이트

### S3 이미지 URL 형식

```
https://dandi-lost-items.s3.ap-northeast-2.amazonaws.com/lost-items/{id}.{ext}
```

---

## Troubleshooting

### Gradle이 Java 17을 찾지 못하는 경우

```
Cannot find a Java installation on your machine matching: {languageVersion=17}
```

→ [gradle.properties 설정](#4-gradleproperties-설정-필수) 참고

### `gradlew.bat: command not found`

bash 환경(WSL, Git Bash 등)에서는:
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
