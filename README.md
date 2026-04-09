# DANDI Backend

## 개발 환경 요구사항

- Java 17
- Docker Desktop

---

## 환경 구성 순서

### 1. Java 17 설치

[https://adoptium.net](https://adoptium.net) 접속 후 **Temurin 17** 다운로드 및 설치

설치 확인:
```bash
java -version
# java version "17.x.x" 출력되면 정상
```

---

### 2. Docker Desktop 설치

[https://www.docker.com/products/docker-desktop](https://www.docker.com/products/docker-desktop) 접속 후 운영체제에 맞는 버전 다운로드 및 설치

설치 후 Docker Desktop 실행 (백그라운드에서 실행 중이어야 함)

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

### 4. MySQL 실행

```bash
docker-compose up -d
```

실행 확인:
```bash
docker ps
# dandi_mysql 컨테이너가 실행 중이면 정상
```

---

### 5. Spring Boot 실행

**Mac / Linux:**
```bash
./gradlew bootRun
```

**Windows:**
```bash
gradlew.bat bootRun
```

아래 로그가 출력되면 정상 실행된 것입니다:
```
Started Main in x.xxx seconds
```

브라우저에서 확인: [http://localhost:8080](http://localhost:8080)

---

## DB 접속 정보

| 항목 | 값 |
|------|-----|
| Host | localhost |
| Port | 3306 |
| DB 이름 | dandidb |
| 사용자 | dandi |
| 비밀번호 | dandi1234 |

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
