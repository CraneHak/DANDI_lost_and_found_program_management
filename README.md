# DANDI Backend

> 단국대학교 교내 분실물 통합 웹 서비스 **단디**의 백엔드. 습득물 신고, 관리자 검수, QR 수령 인증, 이미지 분석을 처리하는 Spring Boot 서버

[![Deploy](https://github.com/CraneHak/DANDI_lost_and_found_program_management/actions/workflows/deploy.yml/badge.svg?branch=main)](https://github.com/CraneHak/DANDI_lost_and_found_program_management/actions/workflows/deploy.yml)

| 구분 | 링크 |
|------|------|
| 서비스 | https://www.dandilost.life (임시 운영 2026-11-06 ~ 2026-12-20) |
| 프론트엔드 | https://github.com/hprays/DANDI-lost-and-found-program |
| 결과 보고서 · 포스터 · 시연 영상 | (제출 전 링크 추가) |

팀: 이정우(32213459) · 이지학(32213564) · 한산(32214796) — 단국대학교 소프트웨어학과

---

## System Architecture

![System Architecture](docs/architecture.png)

---

## Tech Stack

![Java](https://img.shields.io/badge/Java_17-ED8B00?style=flat-square&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot_3.4-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?style=flat-square&logo=springsecurity&logoColor=white)
![JPA](https://img.shields.io/badge/Spring_Data_JPA-6DB33F?style=flat-square&logo=spring&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL_8_(RDS)-4479A1?style=flat-square&logo=mysql&logoColor=white)
![AWS](https://img.shields.io/badge/AWS_EC2_·_S3-232F3E?style=flat-square&logo=amazonwebservices&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase_Auth-FFCA28?style=flat-square&logo=firebase&logoColor=black)
![Google Cloud](https://img.shields.io/badge/Cloud_Vision-4285F4?style=flat-square&logo=googlecloud&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-02303A?style=flat-square&logo=gradle&logoColor=white)
![GitHub Actions](https://img.shields.io/badge/GitHub_Actions-2088FF?style=flat-square&logo=githubactions&logoColor=white)

---

## Key Features

### 1. 신고 → 검수 → 공개
- 학생 신고는 `pending`으로 접수하고, 관리자가 사진과 습득 정보를 확인한 뒤 공개 물품으로 연결
- 신고 상태와 물품 상태를 분리해 관리 (`pending → resolved → picked_up` / `ACQUIRED → RECEIVED`)

### 2. QR 수령 인증
- QR 유효시간 10분, 같은 사용자·물품에 유효한 QR이 있으면 기존 QR 반환
- 관리자 인증 시 물품·신고·QR·수령 이력·알림을 **한 트랜잭션**에서 반영 (비관적 행 잠금)

### 3. 이미지 분석과 개인정보 보호
- Google Cloud Vision으로 객체·색상을 추출하고, 신분증·카드 등은 OCR로 민감 영역을 가린 이미지 생성
- 사진 업로드는 형식 검사 후 S3에 무작위 파일명으로 저장

### 4. 인증과 권한
- 학교 Google 계정(Firebase) 로그인, 서버가 ID 토큰과 `@dankook.ac.kr` 도메인 확인
- 일반 사용자 / 관리자 역할 분리, 본인 데이터만 조회

### 5. 개인화
- 관심 물품, 관심 키워드 일치 알림, 서비스 내부 알림, 관리자 작업 이력

### 6. 자동 배포
- `main` 푸시 → 테스트·빌드 → S3 → SSM으로 EC2 교체 → 헬스체크, 실패 시 이전 버전으로 자동 롤백

---

## Trouble Shooting

### 신고 ID와 물품 ID를 같은 값으로 비교해 엉뚱한 항목이 선택됨
- **문제**: 물품 20을 삭제하면 물품 10이 대상이 되는 경우가 있었음. 실제 DB에 "한 물품의 신고 ID가 다른 물품의 ID와 같은" 사례가 16건
- **해결**: 같은 종류의 ID끼리만 비교, QR 응답의 `reportId`는 실제 신고 ID로 수정하고 `lostItemId` 추가
- **결과**: 같은 번호의 신고와 물품이 각각 올바른 화면으로 열림

### QR 인증 후 물품·신고 상태가 어긋나고 중복 인증 가능
- **문제**: 이미 수령된 물품에도 QR이 발급되고, 인증은 물품만 바꿔 연결 신고는 그대로였음. `findByTokenForUpdate`에 실제 잠금이 없었음
- **해결**: QR·물품 행을 `PESSIMISTIC_WRITE`로 잠근 뒤 만료·사용·수령 여부를 검사하고, 물품·신고·이력을 한 트랜잭션으로 확정
- **결과**: 같은 토큰 재인증과 수령된 물품의 QR 재발급은 409로 거부, 인증 후 물품 `RECEIVED` · 신고 `picked_up` · 알림 1건 확인

### 습득 시각이 00:00으로 저장됨
- **문제**: 프론트가 보내는 UTC ISO 시각(`...Z`)을 해석하지 못해 날짜만 남김 (18:30 → 00:00)
- **해결**: 시간대가 붙은 시각을 Asia/Seoul로 변환해 저장
- **결과**: `2026-10-10T18:30:00`으로 저장되고 화면에도 18시 30분으로 표시

---

## Getting Started

```bash
git clone https://github.com/CraneHak/DANDI_lost_and_found_program_management.git
cd DANDI_lost_and_found_program_management
cp .env.example .env     # DB, AWS, Firebase, Vision, LLM 값을 채웁니다
./gradlew bootRun        # http://localhost:8080  (상태 확인: /api/public/health)
```

테스트와 빌드: `./gradlew clean test bootJar` (Java 17)
