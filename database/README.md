# Mocktalk Backend DB ERD 및 마이그레이션 메모 (V1~V12)

이 문서는 `mocktalkback` 데이터베이스 스키마와 주요 마이그레이션 변경 사항을 `V1`부터 `V12`까지 기준으로 정리한 메모입니다.

- `erd.mmd`, `erd.svg`는 현재 컬럼/관계 기준으로 `V1~V11` 내용을 반영합니다.
- `V10`, `V12`는 인덱스/복구 성격의 변경이므로 아래 요약과 참고 문서로 함께 관리합니다.

- 기준 마이그레이션
  - `mocktalkback/src/main/resources/db/migration/V1__init.sql`
  - `mocktalkback/src/main/resources/db/migration/V2__seed_insert.sql` (시드 데이터, 구조 변경 없음)
  - `mocktalkback/src/main/resources/db/migration/V3__create_search_FTS.sql`
  - `mocktalkback/src/main/resources/db/migration/V4__search_rank_functions.sql` (함수 추가, 테이블 구조 변경 없음)
  - `mocktalkback/src/main/resources/db/migration/V5__moderation_admin.sql`
  - `mocktalkback/src/main/resources/db/migration/V6__file_variants.sql`
  - `mocktalkback/src/main/resources/db/migration/V7__article_sync_version.sql`
  - `mocktalkback/src/main/resources/db/migration/V8__security_advisor_fix.sql` (보안 설정, 테이블 구조 변경 없음)
  - `mocktalkback/src/main/resources/db/migration/V9__article_author_search.sql`
  - `mocktalkback/src/main/resources/db/migration/V10__article_author_search_index.sql` (비트랜잭션 인덱스 추가)
  - `mocktalkback/src/main/resources/db/migration/V11__board_article_write_policy.sql`
  - `mocktalkback/src/main/resources/db/migration/V12__repair_article_author_search_index.sql` (invalid 인덱스 복구)

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
- `V9`: 게시글 작성자 검색 보조 컬럼/함수/트리거 추가
  - 컬럼: `tb_articles.author_search_text`
- `V10`: 게시글 작성자 검색용 trigram GIN 인덱스 추가
  - 인덱스: `ix_tb_articles_author_search_text_trgm`
- `V11`: 게시판별 게시글 작성 권한 정책 컬럼 추가
  - 컬럼: `tb_boards.article_write_policy`
- `V12`: `V10` 도중 중단으로 남을 수 있는 invalid 인덱스 복구
  - 구조 추가는 없고, 운영 복구용 인덱스 재생성 로직을 제공

## ERD

- 원본: [erd.mmd](./erd.mmd)
- 렌더 결과: [erd.svg](./erd.svg)
- 주의: `V10`, `V12`는 인덱스/복구 성격이라 ERD보다 변경 요약과 운영 메모가 더 중요합니다.

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
- `V9`, `V11` 컬럼 변경은 현재 ERD 산출물에 반영되어 있습니다.
- `V10`, `V12`는 인덱스/복구 성격의 변경으로 ERD 중심 변경은 아닙니다.
- `CREATE INDEX CONCURRENTLY`, Flyway PostgreSQL 잠금 방식, invalid 인덱스 복구 배경은 [flyway-concurrently-guide.md](./flyway-concurrently-guide.md) 문서를 참고합니다.
