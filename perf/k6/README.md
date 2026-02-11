# k6 부하 테스트 가이드

## 1) 목적

- 반응 토글 API의 동시성 경합 상황에서 오류율/응답시간/정합성 지표를 수집한다.
- Snapshot API가 동시 부하 상황에서도 안정적으로 응답하는지 확인한다.

## 2) 시나리오

- `article_reaction_toggle`
  - `POST /api/articles/{id}/reactions`
  - 여러 VU가 동일 게시글에 반응 토글을 동시에 반복
- `comment_reaction_toggle`
  - `POST /api/comments/{id}/reactions`
  - 여러 VU가 동일 댓글에 반응 토글을 동시에 반복
- `snapshot_resync`
  - `GET /api/articles/{id}/comments/snapshot`
  - 재동기화용 스냅샷 호출을 동시 수행

## 3) 실행 전 준비

- 테스트용 계정 1개 준비(로그인 가능해야 함)
- `K6_ARTICLE_ID`, `K6_COMMENT_ID` 대상 데이터 준비

## 4) 실행 예시

```bash
k6 run perf/k6/concurrency.js \
  -e BASE_URL=http://localhost:8082 \
  -e K6_LOGIN_ID=seed_user \
  -e K6_PASSWORD=123123123 \
  -e K6_ARTICLE_ID=1 \
  -e K6_COMMENT_ID=1
```
```bash
k6 run perf/k6/concurrency.js -e BASE_URL=http://localhost:8082 -e K6_LOGIN_ID=seed_user -e K6_PASSWORD=123123123 -e K6_ARTICLE_ID=1 -e K6_COMMENT_ID=1
```

## 5) 주요 환경변수

- `BASE_URL` (기본값: `http://localhost:8082`)
- `K6_LOGIN_ID` (필수)
- `K6_PASSWORD` (필수)
- `K6_ARTICLE_ID` (기본값: `1`)
- `K6_COMMENT_ID` (기본값: `1`)
- `K6_VUS_ARTICLE_REACTION` (기본값: `20`)
- `K6_DURATION_ARTICLE_REACTION` (기본값: `30s`)
- `K6_VUS_COMMENT_REACTION` (기본값: `20`)
- `K6_DURATION_COMMENT_REACTION` (기본값: `30s`)
- `K6_VUS_SNAPSHOT` (기본값: `5`)
- `K6_DURATION_SNAPSHOT` (기본값: `30s`)

## 6) 합격 기준(기본)

- `http_req_failed < 1%`
- `http_req_duration p95 < 800ms`
- `http_req_duration p99 < 1200ms`
- `consistency_failures == 0`

## 7) 결과 해석 포인트

- 에러율 증가 시:
  - 인증/권한/대상 ID 유효성 먼저 확인
  - 이후 DB 경합/락 대기 여부 확인
- p95/p99 증가 시:
  - DB CPU/IO, 슬로우쿼리, 커넥션 풀 사용량 점검
- `consistency_failures > 0` 시:
  - 응답 구조/반응값 범위/카운트 음수 여부를 우선 조사
