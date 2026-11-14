# 지학 담당 API 백엔드 개선 내역 (프론트/타 API 미수정)

기준:
- 프론트엔드 코드 미수정
- 지학 담당 API 범위만 수정
  - Reports: `/api/reports`
  - Notices: `/api/notices`
  - Keywords: `/api/users/keywords`
- Auth/Pickup/기타 타 백엔드 영역은 수정하지 않음

---

## 1) Reports API 개선

대상 파일:
- `src/main/java/org/example/report/ReportRepository.java`
- `src/main/java/org/example/report/ReportService.java`
- `src/main/java/org/example/report/ReportController.java`

변경 사항:
- `GET /api/reports` 정렬 보장: 최신 생성순(`created_at DESC`)
- `POST /api/reports` 입력 정규화/검증 강화
  - `itemName`, `location` 공백/빈값 방지
  - 문자열 trim 처리
- `PATCH /api/reports/{id}/status` 상태 전이 제약 추가
  - `picked_up -> 다른 상태` 되돌리기 금지
  - `unavailable -> picked_up` 직접 전이 금지
  - `picked_up` 전이 시 `picked_up_at` 자동 기록
- `DELETE /api/reports/{id}` 제약 추가
  - `picked_up` 상태 신고는 삭제 금지
- Report 이벤트 시 Notice 자동 생성
  - 신고 등록: `신고 접수됨`
  - 상태 변경: `습득 완료/최종 수령 완료/습득 불가` 알림 자동 생성

응답 코드 개선:
- 유효성 오류: `400 BAD_REQUEST`
- 상태 전이/삭제 충돌: `409 CONFLICT`
- 대상 미존재: `404 NOT_FOUND`

---

## 2) Keywords API 개선

대상 파일:
- `src/main/java/org/example/keyword/KeywordRepository.java`
- `src/main/java/org/example/keyword/KeywordService.java`
- `src/main/java/org/example/keyword/KeywordController.java`

변경 사항:
- 키워드 정규화: trim + 연속 공백 단일화
- 빈 문자열 금지
- 최대 길이 제한: 30자
- 중복 키워드 방지: 대소문자 무시 비교
- 최대 등록 개수 제한: 10개
- 위 제한 위반 시 `400 BAD_REQUEST` 반환

---

## 3) Notices API

대상 파일:
- `src/main/java/org/example/notice/*` (직접 로직 변경 없음)

설명:
- Reports API에서 생성/상태변경 이벤트 시 Notice를 추가하도록 연결했기 때문에
  기존 `GET /api/notices`, `PATCH /api/notices/{id}/read` API로 즉시 확인 가능.

---

## 기대 효과 (백엔드 단독)

- 신고 목록 조회 시 정렬 일관성 확보
- 신고 등록 데이터 품질 향상
- 상태 변경/삭제에 대한 서버 정합성 강화
- 신고/처리 이벤트가 알림함에 자동 누적되어 마이페이지 알림 체감 개선
- 키워드 중복/노이즈 데이터 방지

---

## 한계 (프론트 수정 없이는 완결 불가한 부분)

- 새로고침 후 사용자 화면 복원 자체는 프론트가 API 재조회 로직을 가져야 완전 해결됨
- 사용자 정보 수정 UI/QR 발급 UX 문제는 지학 담당 API 범위를 넘어서는 프론트/타 API 연계 이슈
