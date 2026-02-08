# Mocktalk Backend

Mocktalk 커뮤니티/갤러리 서비스의 백엔드 API 서버입니다.  
Spring Boot 기반으로 인증, 게시판, 댓글/대댓글, 알림, 파일 업로드 기능을 제공합니다.

## 한눈에 보기

- Base Path: `/api`
- 기본 포트: `8082` (`SERVER_PORT`)
- 관리 포트: `8083` (`MANAGEMENT_PORT`)
- 인증: JWT Access Token(Bearer) + Refresh Token(HttpOnly Cookie)
- DB 마이그레이션: Flyway (`src/main/resources/db/migration`)

## 기술 스택

- Java 21
- Spring Boot
- Spring Security 6
- Spring Data JPA
- QueryDSL
- PostgreSQL
- Redis
- Flyway

## 아키텍처

```text
Nginx(8081)
  ├─ /      -> Vue 정적 파일
  └─ /api   -> Spring Backend(8082)
               ├─ PostgreSQL
               └─ Redis
               └─ Object Storage(MinIO/OCI)
```

## 도메인 구성

`src/main/java/com/mocktalkback/domain`

- `article`: 게시글
- `board`: 게시판/포럼
- `comment`: 댓글/대댓글
- `file`: 파일 업로드
- `moderation`: 관리/제재
- `notification`: 알림
- `role`: 권한
- `search`: 검색
- `user`: 회원/인증 연계

공통 모듈은 `global` 패키지(`auth`, `common`, `config`, `exception`)에 위치합니다.

## 실행 방법

### 1) 환경 변수 준비

- 개발: `mocktalkback/.env.dev`
- 운영: `mocktalkback/.env.prod`
- 기본 키 목록: `mocktalkback/.env.example`

개발 프로파일(`application-dev.yml`)은 `DEV_*` 키(`DEV_DB_URL`, `DEV_REDIS_HOST` 등)를 사용합니다.

### 2) 애플리케이션 실행

Windows:

```powershell
.\gradlew.bat bootRun --args="--spring.profiles.active=dev"
```

macOS/Linux:

```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

DB/Redis/Object Storage는 사전에 실행되어 있어야 합니다.

로컬 개발에서 PostgreSQL/MinIO를 분리 실행하려면:

```powershell
docker compose -f docker-compose_postgres.yml up -d
docker compose -f docker-compose_minio.yml up -d
```

## 프로파일

- `dev`: `application-dev.yml` 사용, 개발용 DB/Redis 키(`DEV_*`) 사용
- `prod`: `application-prod.yml` 사용, 운영용 키(`DB_*`, `REDIS_*`) 사용

## 핵심 환경 변수

| 이름 | 설명 |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | 실행 프로파일 (`dev`, `prod`) |
| `SERVER_PORT` | API 서버 포트 |
| `MANAGEMENT_PORT` | Actuator 포트 |
| `JWT_SECRET` | JWT 서명 키 |
| `JWT_ISSUER` | JWT 발급자 |
| `DOMAIN` | OAuth2 리다이렉트 및 쿠키 정책 기준 도메인 |
| `SECURITY_ORIGIN_ALLOWLIST` | Refresh/Logout Origin 허용 목록 |
| `SECURITY_COOKIE_SECURE` | 쿠키 Secure 속성 사용 여부 |
| `OBJECT_STORAGE_ENDPOINT` | 오브젝트 스토리지 엔드포인트 |
| `OBJECT_STORAGE_REGION` | 오브젝트 스토리지 리전 |
| `OBJECT_STORAGE_BUCKET` | 버킷 이름 |
| `OBJECT_STORAGE_ACCESS_KEY` | 액세스 키 |
| `OBJECT_STORAGE_SECRET_KEY` | 시크릿 키 |
| `OBJECT_STORAGE_PATH_STYLE_ACCESS` | S3 호환 Path-Style 사용 여부 |
| `OBJECT_STORAGE_KEY_PREFIX` | 오브젝트 키 prefix |
| `OBJECT_STORAGE_PUBLIC_BASE_URL` | 퍼블릭 조회 URL 베이스(선택) |
| `OBJECT_STORAGE_PRESIGN_ENDPOINT` | Presigned URL 생성 기준 엔드포인트(선택) |
| `OBJECT_STORAGE_PRESIGN_EXPIRE_SECONDS` | Presigned URL 만료(초) |
| `DEV_DB_URL` / `DB_URL` | PostgreSQL 접속 URL(프로파일별) |
| `DEV_REDIS_HOST` / `REDIS_HOST` | Redis 호스트(프로파일별) |

## 파일 스토리지 환경 변수 상세

오브젝트 스토리지는 기본적으로 `private 버킷 + presigned URL` 구성을 권장합니다.

| 이름 | 필수 여부 | 설명 | 권장/예시 |
| --- | --- | --- | --- |
| `OBJECT_STORAGE_ENDPOINT` | 필수 | 오브젝트 스토리지 API 엔드포인트(업로드/조회/삭제 호출 기준) | dev(MinIO): `http://host.docker.internal:9000` / prod(OCI): `https://<namespace>.compat.objectstorage.<region>.oraclecloud.com` |
| `OBJECT_STORAGE_REGION` | 필수 | 스토리지 리전 | OCI 리전과 일치 (예: `ap-seoul-1`) |
| `OBJECT_STORAGE_BUCKET` | 필수 | 업로드 대상 버킷명 | 예: `mocktalk` |
| `OBJECT_STORAGE_ACCESS_KEY` | 필수 | 스토리지 접근 키 | OCI 사용 시 **Customer Secret Key의 Access Key** |
| `OBJECT_STORAGE_SECRET_KEY` | 필수 | 스토리지 비밀 키 | OCI 사용 시 **Customer Secret Key의 Secret Key** |
| `OBJECT_STORAGE_PATH_STYLE_ACCESS` | 선택 | S3 path-style 관련 설정 키 | 현재 구현에서는 직접 사용하지 않음 (`false` 권장) |
| `OBJECT_STORAGE_KEY_PREFIX` | 선택 | 오브젝트 키 prefix | 기본 `uploads` |
| `OBJECT_STORAGE_PUBLIC_BASE_URL` | 선택 | 공개 버킷 직접 조회 URL 베이스 | private 버킷이면 **빈 값 유지** |
| `OBJECT_STORAGE_PRESIGN_ENDPOINT` | 선택 | presigned URL 생성 시 사용할 host | dev에서 브라우저 접근 host 분리 시 사용 (예: `http://localhost:9000`) |
| `OBJECT_STORAGE_PRESIGN_EXPIRE_SECONDS` | 선택 | presigned URL 만료(초) | 기본 `300` |

### 설정 가이드

- `private 버킷` 운영 시:
  - `OBJECT_STORAGE_PUBLIC_BASE_URL=` (비움)
  - `/api/files/{id}/view`에서 presigned URL로 리다이렉트
- `public 버킷` 운영 시:
  - `OBJECT_STORAGE_PUBLIC_BASE_URL`를 공개 URL 베이스로 설정 가능
- OCI 키 주의:
  - PEM 형태 API Key 쌍이 아니라 **Customer Secret Key** 값을 사용해야 합니다.

## API 문서(개발)

- `http://localhost:8082/swagger-ui/index.html`
- `http://localhost:8082/v3/api-docs`

## 패키지 구조

```text
com.mocktalkback/
├── global/
│   ├── auth/
│   ├── common/
│   ├── config/
│   └── exception/
├── domain/
│   ├── user/
│   ├── article/
│   ├── board/
│   ├── comment/
│   ├── file/
│   ├── moderation/
│   ├── notification/
│   ├── role/
│   └── search/
└── infra/
    ├── redis/
    └── storage/
```

## 보안 정책 요약

- Access Token은 `Authorization: Bearer <token>`으로 전달합니다.
- Refresh Token은 HttpOnly Cookie로만 전달합니다.
- Refresh/Logout 같은 쿠키 기반 민감 엔드포인트는 Origin allowlist를 검사합니다.
- API는 Stateless를 기본 원칙으로 사용합니다.
