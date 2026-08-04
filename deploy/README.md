# EC2 임시 배포

Vue 정적 파일은 Nginx가 제공하고, `/api` 요청은 같은 Docker 네트워크의 Spring 앱으로 프록시한다. 따라서 브라우저에는 API 별도 도메인이나 CORS 설정이 필요하지 않다.

## 실행

EC2에서 저장소를 받은 뒤 다음을 실행한다.

```bash
cd deploy
cp .env.example .env
# .env의 MYSQL_PASSWORD, MYSQL_ROOT_PASSWORD, JWT_SECRET, PAIRING_INVITE_BASE_URL을 실제 값으로 변경
docker compose -f docker-compose.ec2.yml up --build -d
```

접속 확인은 `http://<EC2 공인 IP>/healthz`로 한다. 프런트 SPA 라우트는 Nginx의 `try_files` fallback으로 `/index.html`을 반환하며, `/api/*`는 내부 `app:8080` 컨테이너로 전달된다.

## EC2 보안 그룹

- 인바운드: TCP 80만 임시로 허용
- MySQL(3306), Redis(6379), Spring 앱(8080)은 호스트에 노출하지 않는다.
- SSH(22)는 본인 IP로만 제한한다.

## 후속 작업

- 실제 도메인 연결 후 `PAIRING_INVITE_BASE_URL`을 `https://<도메인>/ward/pairing`으로 변경
- Nginx TLS 인증서와 443 리스너 추가
- MySQL 백업, 외부 RDS/ElastiCache 전환, 시크릿 관리 도입
