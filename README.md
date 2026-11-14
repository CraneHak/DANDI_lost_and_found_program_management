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

운영 전 점검과 실제 사용 중에 겪은 문제입니다.

### 1. 신고 ID와 물품 ID를 같은 값으로 비교해 엉뚱한 항목이 선택됨
- **문제**: 물품 20을 삭제하면 물품 10이 대상이 되는 경우가 있었음. 실제 DB에 "한 물품의 신고 ID가 다른 물품의 ID와 같은" 사례가 16건
- **해결**: 같은 종류의 ID끼리만 비교, QR 응답의 `reportId`는 실제 신고 ID로 수정하고 `lostItemId` 추가
- **결과**: 같은 번호의 신고와 물품이 각각 올바른 화면으로 열림

### 2. QR 인증의 동시 처리와 상태 불일치
- **문제**: 인증이 토큰의 만료·사용만 확인하고 `findByTokenForUpdate`에 실제 잠금이 없어, 같은 물품의 QR 두 개가 동시에 통과하거나 이미 수령된 물품에 QR이 재발급될 수 있었음. 인증은 물품만 바꿔 연결 신고는 그대로였음
- **해결**: QR·물품 행을 `PESSIMISTIC_WRITE`로 잠그고 만료·사용·수령 여부를 검사한 뒤, 물품·신고·이력을 한 트랜잭션으로 확정
- **결과**: 같은 토큰 재인증과 수령된 물품의 QR 재발급은 409로 거부, 인증 후 물품 `RECEIVED` · 신고 `picked_up` · 알림 1건 확인

### 3. 관리자 API에 권한 검사가 없었음
- **문제**: 로그인만 요구해서 물품 등록·수정·삭제와 신고 상태 변경을 일반 로그인 사용자도 호출할 수 있었음. 신고 조회 응답에는 신고자 이메일이 포함됐음
- **해결**: 관리자 전용 경로에 `hasRole("ADMIN")` 적용, 일반 사용자는 본인 신고만 조회·삭제
- **결과**: 관리자 전용 접근과 본인 데이터 범위를 통합 테스트로 검증

### 4. 사진 원본이 공개 경로에 남음
- **문제**: Vision 분석 때 원본과 가림 결과를 `uploads/` 아래에 저장하고 공개 URL을 만들어, 주소를 알면 가리기 전 원본에 접근할 수 있는 구조였음
- **해결**: 원본은 디스크에 저장하지 않고 메모리에서만 분석, 가림 결과만 저장. 업로드는 사진 형식만 받고 S3 파일명은 무작위로 저장
- **결과**: 공개 경로에 원본이 남지 않고, 파일명에 포함될 수 있는 이름·학번이 저장 경로에 남지 않음

### 5. 챗봇을 제한 없이 호출할 수 있었음
- **문제**: 누구나 반복 호출할 수 있어 LLM 비용과 남용 위험이 있었음
- **해결**: 분·시간·일 단위 호출 제한과 위반 시 단계별 차단(15분 → 24시간)을 적용하고 값은 환경변수로 조정, 프론트 라우트는 로그인 사용자 기준으로 제한
- **결과**: 제한을 넘으면 차단되고, 한도는 재배포 없이 설정값으로 바꿀 수 있음

### 6. 여러 수정을 한꺼번에 배포했다가 문제가 생겨 전체를 되돌림
- **문제**: 수정 여러 개를 묶어 배포하자 문제가 생겼고 원인을 가리기 어려워 전체를 되돌려야 했음
- **해결**: 수정을 하나씩 배포하고 확인하는 방식으로 변경. 이후 GitHub Actions로 테스트를 통과해야 배포하고, 헬스체크가 실패하면 이전 버전으로 자동 롤백. 저장소가 공개라서 실패 로그는 서버 파일에만 남김
- **결과**: 테스트 실패 시 배포되지 않고, 배포 실패 시 이전 버전이 유지됨

### 7. API 도메인의 HTTPS 인증서 발급이 지연됨
- **문제**: `api.dandilost.life` 인증서를 발급할 때 도메인 업체 네임서버(DNSSEC)가 레코드 등록 전의 "없음" 응답을 캐시하고 있어 발급이 막힘
- **해결**: 캐시가 풀린 뒤 발급하고 Let's Encrypt 자동 갱신으로 설정. 새 서브도메인은 레코드를 등록하기 전에 조회하지 않는 것을 규칙으로 정함
- **결과**: `https://api.dandilost.life` 정상 응답

### 8. 습득 시각이 00:00으로 저장됨
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
