# k6 부하 테스트 가이드

## 1) 목적

- 게시글/댓글/검색 도메인의 단위 부하 테스트를 분리해 검증한다.
- 도메인 혼합 부하(통합 시나리오)에서 정합성과 응답지연을 확인한다.

## 2) 스크립트 구성

### 단위(Unit) 스크립트
- `perf/k6/article.unit.load.js`
  - 게시글 상세 조회
  - 게시글 반응 토글
- `perf/k6/article.view.load.js`
  - 동일 로그인 사용자 반복 조회
  - 다중 로그인 사용자 fan-out
  - 익명 사용자 dedupe(IP/User-Agent 조합)
- `perf/k6/article.trending.load.js`
  - 조회/반응/북마크/댓글 혼합 쓰기
  - 공개 인기글 조회 API 동시 호출
- `perf/k6/article.recommended.load.js`
  - 비로그인 fallback 추천
  - warm user 개인화 추천
  - cold user fallback 추천
  - 홈 `최신글 -> 트렌딩 -> 추천` 탭 전환 혼합 호출
- `perf/k6/comment.unit.load.js`
  - 댓글 목록 조회
  - 댓글 스냅샷 조회
  - 댓글 반응 토글
- `perf/k6/search.unit.load.js`
  - 통합 검색(ALL)
  - 게시글 검색(ARTICLE)
  - 댓글 검색(COMMENT)

### 통합(Integration) 스크립트
- `perf/k6/integration.load.js`
  - 게시글 반응 + 댓글 반응 + 스냅샷 + 검색을 동시 실행

## 3) 실행 전 준비

- 테스트 계정 준비
  - `K6_LOGIN_ID`
  - `K6_PASSWORD`
  - 다중 사용자 시나리오 필요 시 `K6_LOGIN_USERS`
    - 형식: `login1:password1,login2:password2`
  - 추천 시나리오 계정군 분리 필요 시
    - `K6_RECOMMEND_WARM_USERS`
    - `K6_RECOMMEND_COLD_USERS`
- 테스트 대상 데이터 준비
  - `K6_ARTICLE_ID`
  - `K6_COMMENT_ID`
- 필요 시 검색어 준비
  - `K6_SEARCH_QUERY`
- 위험 시나리오 실행 전 점검
  - `tb_articles.hit` 사전 값 기록
  - Redis 상태 확인
  - 테스트 계정 수가 fan-out VU 수보다 많은지 확인

## 4) 실행 예시

### 4.1 게시글 단위 테스트
```bash
k6 run perf/k6/article.unit.load.js \
  -e BASE_URL=http://localhost:8082 \
  -e K6_LOGIN_ID=seed_user \
  -e K6_PASSWORD=123123123 \
  -e K6_ARTICLE_ID=1
```

```bash
k6 run perf/k6/article.unit.load.js -e BASE_URL=http://localhost:8082 -e K6_LOGIN_ID=seed_user -e K6_PASSWORD=123123123 -e K6_ARTICLE_ID=1
```

### 4.2 댓글 단위 테스트
```bash
k6 run perf/k6/comment.unit.load.js \
  -e BASE_URL=http://localhost:8082 \
  -e K6_LOGIN_ID=seed_user \
  -e K6_PASSWORD=123123123 \
  -e K6_ARTICLE_ID=1 \
  -e K6_COMMENT_ID=1
```

```bash
k6 run perf/k6/comment.unit.load.js -e BASE_URL=http://localhost:8082 -e K6_LOGIN_ID=seed_user -e K6_PASSWORD=123123123 -e K6_ARTICLE_ID=1 -e K6_COMMENT_ID=1
```

### 4.3 검색 단위 테스트
```bash
k6 run perf/k6/search.unit.load.js \
  -e BASE_URL=http://localhost:8082 \
  -e K6_LOGIN_ID=seed_user \
  -e K6_PASSWORD=123123123 \
  -e K6_SEARCH_QUERY=공지
```

```bash
k6 run perf/k6/search.unit.load.js -e BASE_URL=http://localhost:8082 -e K6_LOGIN_ID=seed_user -e K6_PASSWORD=123123123 -e K6_SEARCH_QUERY=공지
```

### 4.4 통합 테스트
```bash
k6 run perf/k6/integration.load.js \
  -e BASE_URL=http://localhost:8082 \
  -e K6_LOGIN_ID=seed_user \
  -e K6_PASSWORD=123123123 \
  -e K6_ARTICLE_ID=1 \
  -e K6_COMMENT_ID=1 \
  -e K6_SEARCH_QUERY=공지
```

```bash
k6 run perf/k6/integration.load.js -e BASE_URL=http://localhost:8082 -e K6_LOGIN_ID=seed_user -e K6_PASSWORD=123123123 -e K6_ARTICLE_ID=1 -e K6_COMMENT_ID=1 -e K6_SEARCH_QUERY=공지
```

### 4.5 조회수 dedupe 테스트
```bash
k6 run perf/k6/article.view.load.js \
  -e BASE_URL=http://localhost:8082 \
  -e K6_LOGIN_ID=seed_user \
  -e K6_PASSWORD=123123123 \
  -e K6_LOGIN_USERS=user1:pw1,user2:pw2,user3:pw3,user4:pw4,user5:pw5 \
  -e K6_ARTICLE_ID=1
```

```bash
k6 run perf/k6/article.view.load.js -e BASE_URL=http://localhost:8082 -e K6_LOGIN_ID=seed_user -e K6_PASSWORD=123123123 -e K6_LOGIN_USERS=user1:pw1,user2:pw2,user3:pw3,user4:pw4,user5:pw5 -e K6_ARTICLE_ID=1
```

### 4.6 트렌딩 혼합 부하 테스트
```bash
k6 run perf/k6/article.trending.load.js \
  -e BASE_URL=http://localhost:8082 \
  -e K6_LOGIN_USERS=user1:pw1,user2:pw2,user3:pw3 \
  -e K6_ARTICLE_ID=1 \
  -e K6_TREND_LIMIT=10
```

```bash
k6 run perf/k6/article.trending.load.js -e BASE_URL=http://localhost:8082 -e K6_LOGIN_USERS=user1:pw1,user2:pw2,user3:pw3 -e K6_ARTICLE_ID=1 -e K6_TREND_LIMIT=10
```

### 4.7 추천 API 부하 테스트
```bash
k6 run perf/k6/article.recommended.load.js \
  -e BASE_URL=http://localhost:8082 \
  -e K6_LOGIN_ID=seed_user \
  -e K6_PASSWORD=123123123 \
  -e K6_LOGIN_USERS=warm_user1:pw,warm_user2:pw,warm_user3:pw \
  -e K6_RECOMMEND_COLD_USERS=cold_user1:pw \
  -e K6_RECOMMEND_LIMIT=9
```

```bash
k6 run perf/k6/article.recommended.load.js -e BASE_URL=http://localhost:8082 -e K6_LOGIN_ID=seed_user -e K6_PASSWORD=123123123 -e K6_LOGIN_USERS=warm_user1:pw,warm_user2:pw,warm_user3:pw -e K6_RECOMMEND_COLD_USERS=cold_user1:pw -e K6_RECOMMEND_LIMIT=9
```

## 5) 기본 임계치

- `http_req_failed < 1%`
- `http_req_duration p95 < 800ms`
- `http_req_duration p99 < 1200ms`
- 도메인별 실패율 메트릭(`*_failures`) `== 0`
- 추천 시나리오 `p99 < 1500ms`

## 6) 결과 파일 저장

- 모든 스크립트는 종료 시 `handleSummary()`로 요약 파일을 자동 저장한다.
- 기본 저장 경로: `perf/k6/results/`
- 기본 생성 파일
  - `{script-name}-{timestamp}.json`
  - `{script-name}-{timestamp}.md`
- 경로/파일명 제어용 환경변수
  - `K6_SUMMARY_DIR`
  - `K6_SUMMARY_TAG`
  - `K6_SUMMARY_STAMP`
- raw 시계열 결과가 필요하면 기존처럼 `--out csv=...`, `--out json=...` 를 함께 사용할 수 있다.

## 7) 클라우드 무료 플랜/저사양 환경 권장값

- 기본은 매우 낮은 VU부터 시작
  - `1 ~ 2 VU`, `10 ~ 20s`
- 단계적으로 증분
  - `2 -> 3 -> 5 VU`
- 에러율/지연 급증 시 즉시 중단

## 8) 주석 컨벤션

- 이 저장소의 k6 스크립트 주석은 `Given/When/Then` 방식 사용
  - `Given`: 전제/입력/준비 데이터
  - `When`: 실제 API 호출(부하 행위)
  - `Then`: 응답/정합성/임계치 검증
- 이유:
  - 테스트 의도를 빠르게 파악 가능
  - 시나리오 수정 시 회귀 지점 확인이 쉬움

## 9) 주요 환경변수

- 공통
  - `BASE_URL` (기본값: `http://localhost:8082`)
  - `K6_LOGIN_ID` (필수)
  - `K6_PASSWORD` (필수)
  - `K6_LOGIN_USERS` (다중 사용자 시나리오용, 선택)
  - `K6_SUMMARY_DIR` (기본값: `perf/k6/results`)
  - `K6_SUMMARY_TAG` (파일명 태그)
  - `K6_SUMMARY_STAMP` (파일명 타임스탬프 직접 지정)
- 도메인 대상
  - `K6_ARTICLE_ID` (기본값: `1`)
  - `K6_COMMENT_ID` (기본값: `1`)
  - `K6_SEARCH_QUERY` (기본값: `공지`)
  - `K6_TREND_LIMIT` (기본값: `10`)
  - `K6_RECOMMEND_LIMIT` (기본값: `9`)
- 익명 시나리오
  - `K6_ANON_IP_FIXED`
  - `K6_ANON_USER_AGENT_FIXED`
  - `K6_ANON_IP_PREFIX`
  - `K6_ANON_USER_AGENT_PREFIX`
- 추천 시나리오
  - `K6_RECOMMEND_WARM_USERS` (`K6_LOGIN_USERS` fallback 가능)
  - `K6_RECOMMEND_COLD_USERS` (없으면 `K6_LOGIN_ID`, `K6_PASSWORD` fallback)
  - `K6_RECOMMEND_HOME_PAGE` (기본값: `0`)
  - `K6_RECOMMEND_HOME_RECENT_SIZE` (기본값: `15`)
  - `K6_RECOMMEND_HOME_TREND_WINDOW` (기본값: `DAY`)
  - `K6_RECOMMEND_HOME_TREND_LIMIT` (기본값: `9`)

## 10) 결과 해석 포인트

- 에러율 증가:
  - 인증/권한/ID 유효성 먼저 확인
  - DB/Redis 레이트리밋 여부 확인
- p95/p99 증가:
  - DB CPU/IO, 커넥션 풀, 외부 네트워크 지연 확인
- `*_failures > 0`:
  - 응답 구조/카운트 음수/엔벨로프 실패 케이스 우선 점검
- dedupe 검증:
  - 동일 사용자/동일 익명 시나리오 실행 전후 `hit` 증가량이 `1` 인지 확인
  - fan-out 시나리오 실행 전후 `hit` 증가량이 고유 사용자 수와 일치하는지 확인
- Redis 장애 검증:
  - 경고 로그 발생 여부 확인
  - 상세 조회는 성공하지만 `hit` 과 트렌딩 반영이 생략되는지 확인
- 추천 검증:
  - guest / cold / warm 경로 응답시간 차이 확인
  - `notice`, `inquiry` 비노출 확인
  - warm user에서 DB slow query 여부 확인

## 11) 운영 안전 모드 프리셋(리눅스)

- 기본 프리셋 파일: `perf/k6/.env.k6.safe`
- 권장 절차:
  - 원본은 그대로 두고 로컬 복사본(`.env.k6.safe.local`)을 만들어 값 수정
  - 특히 `K6_LOGIN_ID`, `K6_PASSWORD`, `BASE_URL`, 대상 ID를 실제 값으로 변경

### 11.1 프리셋 로딩
```bash
cd mocktalkback
cp perf/k6/.env.k6.safe perf/k6/.env.k6.safe.local
vi perf/k6/.env.k6.safe.local

set -a
source perf/k6/.env.k6.safe.local
set +a
```

### 11.1-1 운영 맞춤 템플릿 사용(권장)
```bash
cd mocktalkback
cp perf/k6/.env.k6.safe.local.example perf/k6/.env.k6.safe.local
vi perf/k6/.env.k6.safe.local

set -a
source perf/k6/.env.k6.safe.local
set +a
```

### 11.2 실행 예시(통합)
```bash
k6 run perf/k6/integration.load.js
```

### 11.3 실행 예시(단위)
```bash
k6 run perf/k6/article.unit.load.js
k6 run perf/k6/article.view.load.js
k6 run perf/k6/article.trending.load.js
k6 run perf/k6/article.recommended.load.js
k6 run perf/k6/comment.unit.load.js
k6 run perf/k6/search.unit.load.js
```

## 12) 운영 안전 모드 프리셋(Windows PowerShell)

- 기본 프리셋 파일: `perf/k6/.env.k6.safe`
- 권장 절차:
  - 원본은 그대로 두고 로컬 복사본(`.env.k6.safe.local`)을 만들어 값 수정
  - 특히 `K6_LOGIN_ID`, `K6_PASSWORD`, `BASE_URL`, 대상 ID를 실제 값으로 변경

### 12.1 프리셋 로딩
```powershell
Set-Location mocktalkback
Copy-Item perf/k6/.env.k6.safe perf/k6/.env.k6.safe.local
notepad perf/k6/.env.k6.safe.local

Get-Content perf/k6/.env.k6.safe.local | ForEach-Object {
  if ($_ -match '^\s*#' -or $_ -match '^\s*$') { return }
  $name, $value = $_ -split '=', 2
  [System.Environment]::SetEnvironmentVariable($name.Trim(), $value.Trim(), 'Process')
}
```

### 12.1-1 운영 맞춤 템플릿 사용(권장)
```powershell
Set-Location mocktalkback
Copy-Item perf/k6/.env.k6.safe.local.example perf/k6/.env.k6.safe.local
notepad perf/k6/.env.k6.safe.local

Get-Content perf/k6/.env.k6.safe.local | ForEach-Object {
  if ($_ -match '^\s*#' -or $_ -match '^\s*$') { return }
  $name, $value = $_ -split '=', 2
  [System.Environment]::SetEnvironmentVariable($name.Trim(), $value.Trim(), 'Process')
}
```

### 12.2 실행 예시(통합)
```powershell
k6 run perf/k6/integration.load.js
```

### 12.3 실행 예시(단위)
```powershell
k6 run perf/k6/article.unit.load.js
k6 run perf/k6/article.view.load.js
k6 run perf/k6/article.trending.load.js
k6 run perf/k6/comment.unit.load.js
k6 run perf/k6/search.unit.load.js
```

## 13) Redis 장애 시나리오 실행 메모

- `article.view.load.js` 또는 `article.trending.load.js` 를 실행한 상태에서 스테이징 Redis 연결을 의도적으로 중단한다.
- 기대 결과
  - 상세 조회와 인기글 조회 API는 계속 응답한다.
  - 조회수 증가와 트렌딩 점수 적재는 생략될 수 있다.
  - 서버에는 경고 로그가 남아야 한다.
- 운영 환경에서는 수행하지 않는다.
