# Flyway `CREATE INDEX CONCURRENTLY` 운영 메모

이 문서는 Mocktalk 백엔드에서 PostgreSQL `CREATE INDEX CONCURRENTLY`를 Flyway와 함께 사용할 때 알아야 할 동작 방식과 운영상 주의점을 정리한 문서입니다.

관련 파일:

- `src/main/resources/db/migration/V10__article_author_search_index.sql`
- `src/main/resources/db/migration/V10__article_author_search_index.sql.conf`
- `src/main/resources/db/migration/V12__repair_article_author_search_index.sql`
- `src/main/resources/db/migration/V12__repair_article_author_search_index.sql.conf`
- `src/main/resources/application-dev.yml`
- `src/main/resources/application-prod.yml`

## 왜 이 문서를 별도로 남겼는가

이번 이슈의 핵심 설명은 원래 `V12` SQL 상단 주석으로 넣을 수도 있습니다. 하지만 Flyway의 versioned migration은 이미 한 번 적용된 뒤 파일 내용이 바뀌면 checksum 검증에 걸릴 수 있습니다.

즉:

- 아직 아무 환경에도 적용되지 않은 migration 파일은 주석 수정이 가능하다.
- 이미 dev/prod 중 한 곳이라도 적용된 versioned migration은 주석만 바꿔도 validate 실패가 날 수 있다.

그래서 이번 설명은 `V12` 본문을 더 이상 수정하지 않고, `database/` 아래 별도 문서로 유지합니다.

## 이번 프로젝트에서 실제로 발생한 문제

클린 설치 직후 Flyway가 아래 순서로 진행됩니다.

1. `V9`에서 `tb_articles.author_search_text` 컬럼과 트리거/함수를 추가한다.
2. `V10`에서 `author_search_text`용 trigram GIN 인덱스를 `CREATE INDEX CONCURRENTLY`로 생성한다.
3. Flyway는 마이그레이션 직렬화를 위해 PostgreSQL advisory lock을 사용한다.

문제는 이 조합에서 발생했습니다.

- 겉으로는 `V10`에서 멈춘 것처럼 보인다.
- 재시도 전에 프로세스가 종료되면 같은 이름의 invalid 인덱스가 남을 수 있다.
- 다음 재실행 때는 `IF NOT EXISTS` 때문에 이름만 보고 넘어가면서, invalid 인덱스가 계속 남을 수 있다.

이 문제를 해결하기 위해 다음 두 가지를 적용했습니다.

- `spring.flyway.postgresql.transactional-lock=false`
- `V12__repair_article_author_search_index.sql`로 invalid 인덱스 복구

## 일반 `CREATE INDEX`와 `CREATE INDEX CONCURRENTLY` 차이

### 일반 `CREATE INDEX`

일반 인덱스 생성은 상대적으로 단순합니다.

- 보통 하나의 트랜잭션 안에서 작업을 마칠 수 있다.
- 작업 중 테이블 쓰기 작업을 오래 막을 수 있다.
- 운영 중 큰 테이블에 바로 쓰면 서비스 영향이 커질 수 있다.

예시:

```sql
BEGIN;
CREATE INDEX ix_tb_articles_title ON tb_articles (title);
COMMIT;
```

이 방식은 "한 번에 빠르게 만들고 대신 강하게 잠그는" 쪽에 가깝습니다.

### `CREATE INDEX CONCURRENTLY`

`CONCURRENTLY`는 운영 중 쓰기 차단을 최소화하려고 설계된 방식입니다.

- 인덱스를 만들 동안 `INSERT/UPDATE/DELETE`를 최대한 계속 받는다.
- 대신 내부 절차가 단순하지 않다.
- 여러 단계로 나누어 테이블을 읽고, 중간에 다른 트랜잭션이 끝나길 기다린다.
- 실패 시 invalid 인덱스 흔적이 남을 수 있다.

예시:

```sql
CREATE INDEX CONCURRENTLY IF NOT EXISTS ix_tb_articles_author_search_text_trgm
ON tb_articles USING GIN (author_search_text extensions.gin_trgm_ops);
```

즉, `CONCURRENTLY`는 "덜 막는 대신 더 복잡한 절차를 따르는 인덱스 생성"입니다.

## 왜 `CONCURRENTLY`가 트랜잭션과 충돌하는가

이 부분이 가장 중요합니다.

### 1. `CONCURRENTLY`는 단일 트랜잭션 작업이 아니다

일반 `CREATE INDEX`는 하나의 트랜잭션 안에서 다음처럼 이해할 수 있습니다.

- 인덱스 생성 시작
- 테이블 읽기
- 인덱스 채우기
- 완료 후 커밋

하지만 `CREATE INDEX CONCURRENTLY`는 PostgreSQL 내부에서 더 복잡하게 동작합니다.

개념적으로는 다음과 같습니다.

1. 시스템 카탈로그에 인덱스를 먼저 등록한다.
2. 이 시점의 인덱스는 아직 완성본이 아니므로 `invalid` 상태일 수 있다.
3. 1차 테이블 스캔으로 기존 행들을 인덱스에 채운다.
4. 그 사이에 동시에 쓰기를 수행하던 트랜잭션들이 끝나길 기다린다.
5. 2차 스캔으로 누락될 수 있는 변경분을 다시 따라잡는다.
6. 마지막에 인덱스를 `valid` 상태로 전환한다.

즉, 내부적으로 "한 번에 끝나는 한 덩어리 작업"이 아니라:

- 스캔
- 대기
- 재스캔
- 상태 전환

같은 여러 단계를 거칩니다.

### 2. 바깥에서 `BEGIN ... COMMIT`으로 감싸면 이 절차와 맞지 않는다

PostgreSQL은 `CREATE INDEX CONCURRENTLY`를 일반 트랜잭션 블록 안에서 허용하지 않습니다.

아래는 불가능한 예입니다.

```sql
BEGIN;
CREATE INDEX CONCURRENTLY ix_tb_articles_title_trgm
ON tb_articles USING GIN (title gin_trgm_ops);
COMMIT;
```

이게 안 되는 이유는, `CONCURRENTLY`가 내부적으로 중간 단계와 대기 지점을 필요로 하기 때문입니다.

트랜잭션 블록은 바깥에서 보면 "시작 후 마지막에 한 번 커밋"이라는 일관된 경계를 강제합니다.  
반면 `CONCURRENTLY`는 내부적으로 여러 시점의 데이터를 안전하게 맞춰야 하고, 다른 트랜잭션이 끝나는 것도 기다려야 합니다.  
그래서 PostgreSQL은 아예 이 구문을 트랜잭션 블록 안에서 실행하지 못하게 막습니다.

### 3. 실패 시 invalid 인덱스가 남을 수 있다

`CONCURRENTLY`의 가장 실무적인 함정은 여기입니다.

인덱스 빌드 도중 아래와 같은 일이 생기면:

- 세션 강제 종료
- 애플리케이션 프로세스 종료
- lock wait 장기화
- deadlock
- 운영 타임아웃

인덱스가 도중 상태로 남을 수 있습니다.

이 경우 PostgreSQL 카탈로그에는 인덱스 이름이 존재하지만:

- `indisvalid = false`
- 쿼리 최적화에는 사용되지 않음
- 유지비용은 남음
- 같은 이름 재생성도 바로 되지 않음

이라는 상태가 됩니다.

이번 프로젝트의 `V12`는 바로 이 흔적을 복구하기 위한 migration입니다.

## Flyway와 함께 쓸 때 왜 추가 설정이 필요한가

많이 헷갈리는 부분이 이것입니다.

### `executeInTransaction=false`만으로는 충분하지 않다

`V10__article_author_search_index.sql.conf`에 들어 있는:

```text
executeInTransaction=false
```

이 설정은 "이 SQL 파일 자체를 Flyway가 일반 트랜잭션으로 감싸지 말라"는 뜻입니다.

하지만 이 설정만으로 Flyway의 PostgreSQL 잠금 방식까지 바뀌는 것은 아닙니다.

### Flyway는 별도로 advisory lock을 사용한다

Flyway는 같은 DB에 여러 인스턴스가 동시에 migration을 실행하지 못하도록 advisory lock을 사용합니다.

기본 동작은 PostgreSQL의 transaction-level advisory lock 쪽입니다.

개념상 차이는 아래와 같습니다.

- transaction-level lock
  - 트랜잭션 수명에 묶인다.
  - 트랜잭션이 끝나면 자동 해제된다.
  - PostgreSQL 함수로 보면 `pg_try_advisory_xact_lock(...)` 계열이다.

- session-level lock
  - DB 연결 세션 수명에 묶인다.
  - 연결이 끝나거나 명시적으로 unlock 할 때 해제된다.
  - PostgreSQL 함수로 보면 `pg_try_advisory_lock(...)` 계열이다.

### 이번 프로젝트에서 필요한 설정

Mocktalk에서는 아래 설정을 추가했습니다.

```yaml
spring:
  flyway:
    postgresql:
      transactional-lock: false
```

의미는:

- Flyway 잠금을 끄는 것이 아니다.
- Flyway 잠금 방식을 transaction-level에서 session-level로 바꾸는 것이다.

이 설정을 넣은 이유는 `CREATE INDEX CONCURRENTLY` 같은 비트랜잭션 DDL과 Flyway의 기본 PostgreSQL 잠금 조합에서 대기/정지처럼 보이는 문제가 있었기 때문입니다.

즉, `CONCURRENTLY`를 안전하게 쓰려면 이 프로젝트에서는 두 조건이 같이 필요합니다.

1. `.sql.conf`에서 `executeInTransaction=false`
2. `application-*.yml`에서 `spring.flyway.postgresql.transactional-lock=false`

## Supabase 세션 풀러와의 관계

운영 환경에서 Supabase를 사용할 때는 pooler mode를 구분해야 합니다.

### 세션 풀러(session mode)

세션 풀러는 연결 세션 성격이 유지되는 편이라, session-level advisory lock과 의미상 잘 맞습니다.

즉:

- 현재처럼 Flyway에서 `transactional-lock=false`
- 그리고 운영 DB가 Supabase session pooler

조합은 일반적으로 크게 문제되지 않습니다.

### 트랜잭션 풀러(transaction mode)

트랜잭션 풀러는 세션이 고정되지 않고 요청 단위로 다른 실제 DB 세션에 붙을 수 있습니다.

이 모드에서는 아래처럼 세션 상태에 의존하는 동작이 불안정해질 수 있습니다.

- session-level advisory lock
- `SET search_path`
- 임시 테이블
- 세션 변수

그래서 migration 경로는:

- 최우선: direct connection
- 차선: session pooler
- 비권장: transaction pooler

순으로 보는 것이 안전합니다.

## Mocktalk에서 적용한 대응

### 1. `V10`

- `author_search_text`용 trigram GIN 인덱스를 `CREATE INDEX CONCURRENTLY`로 생성
- `.sql.conf`에서 `executeInTransaction=false`

목적:

- 운영 중 테이블 쓰기 차단 최소화

### 2. `application-dev.yml`, `application-prod.yml`

- `spring.flyway.postgresql.transactional-lock=false`

목적:

- Flyway PostgreSQL 기본 잠금과 `CONCURRENTLY` 조합에서 발생하는 정지/대기 문제 완화

### 3. `V12`

- invalid 상태 인덱스가 남아 있으면 먼저 삭제
- 다시 `CREATE INDEX CONCURRENTLY IF NOT EXISTS ...`
- `ANALYZE tb_articles`

목적:

- 이미 꼬여버린 DB도 다음 기동에서 자동 복구

## 앞으로 `CONCURRENTLY` 마이그레이션을 추가할 때 체크리스트

1. 정말 운영 중 쓰기 차단 최소화가 필요한 인덱스인지 확인한다.
2. SQL 파일과 별도로 `.sql.conf`를 만들고 `executeInTransaction=false`를 넣는다.
3. PostgreSQL/Flyway 잠금 설정이 현재 환경과 맞는지 확인한다.
4. 실패 시 invalid 인덱스가 남을 수 있음을 전제로 복구 전략을 준비한다.
5. 이미 적용된 versioned migration은 주석만이라도 수정하지 않는다.
6. 운영 연결이 direct인지, session pooler인지, transaction pooler인지 확인한다.

## 참고 자료

- PostgreSQL `CREATE INDEX`
  - https://www.postgresql.org/docs/current/sql-createindex.html
- PostgreSQL Advisory Locks
  - https://www.postgresql.org/docs/current/explicit-locking.html
- Flyway PostgreSQL `transactional-lock`
  - https://documentation.red-gate.com/flyway/reference/configuration/flyway-namespace/flyway-postgresql-namespace/flyway-postgresql-transactional-lock-setting
- Flyway Versioned migrations
  - https://documentation.red-gate.com/fd/versioned-migrations-273973333.html
- Flyway Validate
  - https://documentation.red-gate.com/flyway/reference/commands/validate
- Flyway Repeatable migrations
  - https://documentation.red-gate.com/fd/repeatable-migrations-273973335.html
- Supabase PostgreSQL 연결 가이드
  - https://supabase.com/docs/guides/database/connecting-to-postgres
