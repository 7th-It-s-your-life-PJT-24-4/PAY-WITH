# pay-with-api

Spring MVC 레거시 기반 백엔드 예제 프로젝트다.

## 로컬 실행

```bash
cp .env.example .env
docker compose up --build
```

### DB 스키마

스키마는 앱이 기동하면서 Flyway가 `src/main/resources/db/migration`을 적용해 만든다.

- **`mysql`만 띄우면 DB는 비어 있다.** 테이블을 보려면 앱을 한 번 기동해야 한다.
- 스키마가 바뀌어도 `docker compose down -v`로 볼륨을 지울 필요가 없다. 새 마이그레이션만 추가로 적용되므로 로컬 테스트 데이터가 보존된다.
- 스키마를 바꿀 때는 `db/migration/`에 새 파일을 만든다. 파일명은 `V<YYYYMMDD>_<HHmm>__<요약>.sql` (예: `V20260806_1430__approval_add_canceled.sql`). 병렬 브랜치 간 버전 충돌을 피하려고 타임스탬프를 쓴다.
- **이미 적용된 마이그레이션은 수정하지 않는다.** `V1__baseline.sql`을 포함해서다. 고치면 Flyway가 checksum 불일치로 기동을 막는다.
- 시드 값(FDS 룰 배점, 은행 목록 등)은 새 파일 대신 `R__seed.sql`을 직접 고친다. 내용이 바뀌면 다음 기동 때 자동으로 다시 실행된다.

## 기본 계정

```text
email: admin@example.com
password: password123
```

## 주요 엔드포인트

- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `GET /api/users`
- `GET /api/users/{id}`
- `POST /api/users`
- `PUT /api/users/{id}`
- `DELETE /api/users/{id}`
- `GET /swagger-ui.html`
