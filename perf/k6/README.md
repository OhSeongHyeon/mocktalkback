# k6 부하 테스트 가이드

## 1) 목적

- 게시글/댓글/검색 도메인의 단위 부하 테스트를 분리해 검증한다.
- 도메인 혼합 부하(통합 시나리오)에서 정합성과 응답지연을 확인한다.

## 2) 스크립트 구성

### 단위(Unit) 스크립트
- `perf/k6/article.unit.load.js`
  - 게시글 상세 조회
  - 게시글 반응 토글
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
- 테스트 대상 데이터 준비
  - `K6_ARTICLE_ID`
  - `K6_COMMENT_ID`
- 필요 시 검색어 준비
  - `K6_SEARCH_QUERY`

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

## 5) 기본 임계치

- `http_req_failed < 1%`
- `http_req_duration p95 < 800ms`
- `http_req_duration p99 < 1200ms`
- 도메인별 실패율 메트릭(`*_failures`) `== 0`

## 6) 클라우드 무료 플랜/저사양 환경 권장값

- 기본은 매우 낮은 VU부터 시작
  - `1 ~ 2 VU`, `10 ~ 20s`
- 단계적으로 증분
  - `2 -> 3 -> 5 VU`
- 에러율/지연 급증 시 즉시 중단

## 7) 주석 컨벤션

- 이 저장소의 k6 스크립트 주석은 `Given/When/Then` 방식 사용
  - `Given`: 전제/입력/준비 데이터
  - `When`: 실제 API 호출(부하 행위)
  - `Then`: 응답/정합성/임계치 검증
- 이유:
  - 테스트 의도를 빠르게 파악 가능
  - 시나리오 수정 시 회귀 지점 확인이 쉬움

## 8) 주요 환경변수

- 공통
  - `BASE_URL` (기본값: `http://localhost:8082`)
  - `K6_LOGIN_ID` (필수)
  - `K6_PASSWORD` (필수)
- 도메인 대상
  - `K6_ARTICLE_ID` (기본값: `1`)
  - `K6_COMMENT_ID` (기본값: `1`)
  - `K6_SEARCH_QUERY` (기본값: `공지`)

## 9) 결과 해석 포인트

- 에러율 증가:
  - 인증/권한/ID 유효성 먼저 확인
  - DB/Redis 레이트리밋 여부 확인
- p95/p99 증가:
  - DB CPU/IO, 커넥션 풀, 외부 네트워크 지연 확인
- `*_failures > 0`:
  - 응답 구조/카운트 음수/엔벨로프 실패 케이스 우선 점검

## 10) 운영 안전 모드 프리셋(리눅스)

- 기본 프리셋 파일: `perf/k6/.env.k6.safe`
- 권장 절차:
  - 원본은 그대로 두고 로컬 복사본(`.env.k6.safe.local`)을 만들어 값 수정
  - 특히 `K6_LOGIN_ID`, `K6_PASSWORD`, `BASE_URL`, 대상 ID를 실제 값으로 변경

### 10.1 프리셋 로딩
```bash
cd mocktalkback
cp perf/k6/.env.k6.safe perf/k6/.env.k6.safe.local
vi perf/k6/.env.k6.safe.local

set -a
source perf/k6/.env.k6.safe.local
set +a
```

### 10.1-1 운영 맞춤 템플릿 사용(권장)
```bash
cd mocktalkback
cp perf/k6/.env.k6.safe.local.example perf/k6/.env.k6.safe.local
vi perf/k6/.env.k6.safe.local

set -a
source perf/k6/.env.k6.safe.local
set +a
```

### 10.2 실행 예시(통합)
```bash
k6 run perf/k6/integration.load.js
```

### 10.3 실행 예시(단위)
```bash
k6 run perf/k6/article.unit.load.js
k6 run perf/k6/comment.unit.load.js
k6 run perf/k6/search.unit.load.js
```

## 11) 운영 안전 모드 프리셋(Windows PowerShell)

- 기본 프리셋 파일: `perf/k6/.env.k6.safe`
- 권장 절차:
  - 원본은 그대로 두고 로컬 복사본(`.env.k6.safe.local`)을 만들어 값 수정
  - 특히 `K6_LOGIN_ID`, `K6_PASSWORD`, `BASE_URL`, 대상 ID를 실제 값으로 변경

### 11.1 프리셋 로딩
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

### 11.1-1 운영 맞춤 템플릿 사용(권장)
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

### 11.2 실행 예시(통합)
```powershell
k6 run perf/k6/integration.load.js
```

### 11.3 실행 예시(단위)
```powershell
k6 run perf/k6/article.unit.load.js
k6 run perf/k6/comment.unit.load.js
k6 run perf/k6/search.unit.load.js
```
