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

### 5. MySQL 실행

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

### 6. Spring Boot 실행

```bash
./gradlew bootRun
```

아래 로그가 출력되면 정상 실행된 것입니다:
```
Started Main in x.xxx seconds
```

브라우저에서 확인: [http://localhost:8080](http://localhost:8080)

> 브라우저에서 **404 Whitelabel Error Page**가 뜨는 것은 정상입니다. 아직 루트 엔드포인트가 없기 때문이며, 서버는 정상 동작 중입니다.

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