# DANDI Backend

단국대학교(죽전) 분실물 통합 관리 서비스 백엔드입니다.

---

## 브랜치 구조

| 브랜치 | 설명 |
|--------|------|
| `main` | 기본 Spring Boot 프로젝트 설정 |
| `database` | AWS RDS MySQL 연결 설정 |
| `crawling` | 단국대 분실물 게시판 크롤러 + S3 이미지 업로드 + REST API |

> **주의:** 크롤링 관련 작업(crawl.py, upload_s3.py 등)은 반드시 `crawling` 브랜치에서만 진행합니다. 다른 브랜치에서 크롤러를 수정하거나 실행하지 않습니다.

---

## 개발 환경 요구사항

- Java 17
- Docker Desktop

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
git checkout database
```

---

### 4. gradle.properties 설정 (필수)

> Java 17이 설치되어 있어도 JAVA_HOME이 다른 버전으로 잡혀 있으면 Gradle이 Java 17을 찾지 못합니다.
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

**Step 2 — gradle.properties 파일 생성**

프로젝트 루트에 `gradle.properties` 파일을 생성하고 아래 내용을 작성합니다:

```properties
org.gradle.java.installations.paths=C:\\Users\\{사용자명}\\AppData\\Local\\Programs\\Eclipse Adoptium\\jdk-17.0.x.x-hotspot
```

> `{사용자명}`과 버전 번호는 Step 1에서 확인한 실제 경로로 수정하세요.
> 경로 구분자는 반드시 `\\` (역슬래시 두 개)를 사용해야 합니다.

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
./gradlew bootRun
```

정상 실행 시:
```
Started Main in x.xxx seconds
```

---

## DB 접속 정보

### 로컬 개발용 (Docker MySQL)

| 항목 | 값 |
|------|-----|
| Host | localhost |
| Port | 3306 |
| DB 이름 | dandidb |
| 사용자 | dandi |
| 비밀번호 | dandi1234 |

`docker-compose up -d` 실행 후 사용합니다.

---

### 공용 DB (AWS RDS)

단국대 분실물 게시판에서 크롤링한 데이터가 저장되어 있는 공용 DB입니다.

| 항목 | 값 |
|------|-----|
| Host | dandi-db.cdocgw0s68cj.ap-northeast-2.rds.amazonaws.com |
| Port | 3306 |
| DB 이름 | dandidb |
| 사용자 | dandi |
| 비밀번호 | 팀 채널 참고 |

#### MySQL 클라이언트로 접속

```bash
mysql -h dandi-db.cdocgw0s68cj.ap-northeast-2.rds.amazonaws.com -u dandi -p dandidb
```

#### DBeaver / DataGrip 등 GUI 툴로 접속

- Host: `dandi-db.cdocgw0s68cj.ap-northeast-2.rds.amazonaws.com`
- Port: `3306`
- Database: `dandidb`
- Username: `dandi`
- Password: (팀 채널 참고)
- **SSL 비활성화** (allowPublicKeyRetrieval=true, useSSL=false)

#### Spring Boot에서 RDS 연결 (prod 프로파일)

`src/main/resources/application-prod.properties` 파일을 생성하세요.
(`.gitignore`에 등록되어 있으므로 커밋되지 않습니다)

```properties
spring.datasource.url=jdbc:mysql://dandi-db.cdocgw0s68cj.ap-northeast-2.rds.amazonaws.com:3306/dandidb?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.username=dandi
spring.datasource.password=비밀번호_입력
spring.jpa.hibernate.ddl-auto=validate
```

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

#### 현재 저장된 데이터

| 테이블 | 설명 | 건수 |
|--------|------|------|
| `lost_item` | 단국대(죽전) 분실물 게시판 크롤링 데이터 | 50건 |

```sql
SELECT id, item_name, found_location, stored_date, image_url FROM lost_item LIMIT 10;
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
