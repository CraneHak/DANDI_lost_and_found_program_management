# 단디 (DANDI) 백엔드

> 단국대학교 교내 분실물 통합 웹 서비스. 습득물 신고부터 QR 수령 완료까지

---

## 팀원

| 학번 | 이름 |
|------|------|
| 32213459 | 이정우 |
| 32213564 | 이지학 |
| 32214796 | 한산 |

---

## 프로젝트 소개

단디는 교내에 흩어진 분실물 정보를 한곳에서 확인하는 웹 서비스입니다.
습득한 학생이 사진과 장소를 신고하면 관리자가 검수해 공개하고, 물품을 잃어버린 학생은 검색한 뒤 QR을 발급받아 보관 거점을 방문합니다. 실제 인계는 현장에서 본인과 물품을 확인한 후 관리자가 QR로 인증합니다.

이 저장소는 인증, 신고·물품 API, QR 수령 처리, 이미지 분석, 알림을 담당하는 **Spring Boot 서버**입니다.

---

## 서비스

| 구분 | 링크 |
|------|------|
| 웹 서비스 | https://www.dandilost.life (임시 운영 2026-11-06 ~ 2026-12-20) |
| API 서버 | https://api.dandilost.life |

---

## 기술 스택

| 구분 | 기술 |
|------|------|
| Frontend | Next.js · React · TypeScript (Vercel) |
| Backend | Java 17 · Spring Boot 3.4 · Spring Security · Spring Data JPA |
| Database | MySQL (AWS RDS) |
| Storage | AWS S3 (물품 사진) |
| 인증 | Firebase Authentication (학교 Google 계정) |
| 이미지 분석 | Google Cloud Vision |
| 배포 | AWS EC2 · GitHub Actions (자동 배포) |

---

## 저장소

| 구분 | 링크 |
|------|------|
| Frontend | [DANDI-lost-and-found-program](https://github.com/hprays/DANDI-lost-and-found-program) |
| Backend (이 저장소) | [DANDI_lost_and_found_program_management](https://github.com/CraneHak/DANDI_lost_and_found_program_management) |

---

## 시스템 구조

![시스템 구조도](docs/architecture.png)

---

## 포스터

(제출 전 추가)

## 시연 영상

(제출 전 추가)

## 결과 보고서

(제출 전 추가)

## 발표 자료

(제출 전 추가)

---

## 실행 방법

```bash
git clone https://github.com/CraneHak/DANDI_lost_and_found_program_management.git
cd DANDI_lost_and_found_program_management
cp .env.example .env     # DB, AWS, Firebase, Vision, LLM 값을 채웁니다
./gradlew bootRun        # http://localhost:8080
```

- 필요: Java 17
- 테스트와 빌드: `./gradlew clean test bootJar`
- `.env`와 키 파일은 커밋하지 않습니다.
