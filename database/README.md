# Mocktalk Backend DB ERD (V1~V8)

이 문서는 `mocktalkback` 데이터베이스 스키마를 `V1`부터 `V8`까지 기준으로 정리한 ERD입니다.

- 기준 마이그레이션
  - `mocktalkback/src/main/resources/db/migration/V1__init.sql`
  - `mocktalkback/src/main/resources/db/migration/V2__seed_insert.sql` (시드 데이터, 구조 변경 없음)
  - `mocktalkback/src/main/resources/db/migration/V3__create_search_FTS.sql`
  - `mocktalkback/src/main/resources/db/migration/V4__search_rank_functions.sql` (함수 추가, 테이블 구조 변경 없음)
  - `mocktalkback/src/main/resources/db/migration/V5__moderation_admin.sql`
  - `mocktalkback/src/main/resources/db/migration/V6__file_variants.sql`
  - `mocktalkback/src/main/resources/db/migration/V7__article_sync_version.sql`
  - `mocktalkback/src/main/resources/db/migration/V8__security_advisor_fix.sql` (보안 설정, 테이블 구조 변경 없음)

## 버전별 구조 변경 요약

- `V1`: 기본 테이블/PK/FK/UNIQUE/인덱스 생성
- `V3`: 검색용 `search_vector` 컬럼 추가
  - `tb_boards`, `tb_articles`, `tb_comments`, `tb_users`
- `V5`: 운영/모더레이션 테이블 추가
  - `tb_reports`, `tb_sanctions`, `tb_admin_audit_logs`
- `V6`: 파일 파생본 테이블 및 파일 컬럼 추가
  - 테이블: `tb_file_variants`
  - 컬럼: `tb_files.metadata_preserved`, `tb_files.temp_expires_at`
- `V7`: 게시글 동기화 버전 컬럼 추가
  - `tb_articles.sync_version`
- `V8`: `fts_*` 함수 `search_path` 고정, `pg_trgm` 스키마 조정 (`extensions`)

## ERD

- 원본: [erd.mmd](./erd.mmd)
- 렌더 결과: [erd.svg](./erd.svg)

![ERD](./erd.svg)

## 핵심 UNIQUE 제약 요약

- 사용자: `login_id`, `email`, `handle`
- 게시판: `board_name`, `slug`
- 파일: `tb_files.storage_key`, `tb_file_variants.storage_key`
- 멤버/구독: `(user_id, board_id)`
- 북마크/반응: `(user_id, article_id)`, `(user_id, comment_id)`
- 카테고리: `(board_id, category_name)`
- OAuth: `(provider, provider_id)`, `(user_id, provider)`
- 파일 매핑: `(article_id, file_id)`, `(comment_id, file_id)`, `(board_id, file_id)`, `(user_id, file_id)`

## 참고

- `V8`은 Supabase Security Advisor 경고 완화 목적의 보안 조치입니다.
- ERD 관점에서 엔터티/관계 변경은 `V3`, `V5`, `V6`, `V7`에서 발생했습니다.


