# pay-with-api

Spring MVC 레거시 기반 백엔드 예제 프로젝트다.

## 로컬 실행

```bash
docker compose up --build
```

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
