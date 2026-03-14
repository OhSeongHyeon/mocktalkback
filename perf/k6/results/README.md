# k6 결과 폴더 안내

## 용도

- 이 폴더는 `k6` 부하테스트 실행 결과를 저장하는 위치입니다.
- 각 스크립트는 종료 시 `handleSummary()`를 통해 요약 파일을 자동 생성합니다.

## 자동 생성 파일

- 기본적으로 아래 두 파일이 생성됩니다.
  - `{script-name}-{timestamp}.json`
  - `{script-name}-{timestamp}.md`
- 예시
  - `article-view-2026-03-12T12-00-43-069Z.json`
  - `article-view-2026-03-12T12-00-43-069Z.md`

## 파일 의미

- `json`
  - `k6`가 제공하는 원본 summary 데이터입니다.
  - 메트릭, threshold 결과, 그룹/체크 정보가 포함됩니다.
- `md`
  - 사람이 빠르게 읽기 위한 요약 리포트입니다.
  - 주요 메트릭과 threshold 통과 여부를 간단히 정리합니다.

## 파일명 제어용 환경변수

- `K6_SUMMARY_DIR`
  - 결과 저장 디렉터리를 변경합니다.
  - 기본값: `perf/k6/results`
- `K6_SUMMARY_TAG`
  - 파일명에 태그를 추가합니다.
  - 예: `article-view-staging-2026-03-12T12-00-43-069Z.json`
- `K6_SUMMARY_STAMP`
  - 자동 타임스탬프 대신 직접 지정한 값을 사용합니다.

## 예시

```powershell
k6 run perf/k6/article.view.load.js `
  -e BASE_URL=http://localhost:8082 `
  -e K6_ARTICLE_ID=1 `
  -e K6_SUMMARY_TAG=local-smoke
```

```powershell
k6 run perf/k6/article.view.load.js `
  -e BASE_URL=http://localhost:8082 `
  -e K6_ARTICLE_ID=1 `
  -e K6_SUMMARY_DIR=perf/k6/results/local `
  -e K6_SUMMARY_TAG=staging `
  -e K6_SUMMARY_STAMP=20260312-article-view
```

## `--out` 옵션과 차이

- 이 폴더의 `json`, `md` 파일은 `테스트 종료 후 요약 결과`입니다.
- `k6 run --out csv=...` 또는 `k6 run --out json=...` 는 `실행 중 수집되는 raw 시계열 데이터` 저장용입니다.
- 둘은 용도가 다르므로 필요하면 함께 사용할 수 있습니다.

## Git 정책

- 이 폴더의 실행 결과 파일은 `.gitignore`로 제외됩니다.
- 저장소에는 폴더 유지를 위한 `.gitkeep` 과 설명 문서인 `README.md` 만 남깁니다.
- 오래된 결과 파일은 필요 시 수동으로 정리합니다.
