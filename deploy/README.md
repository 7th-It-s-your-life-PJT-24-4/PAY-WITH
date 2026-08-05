# EC2 임시 배포

Vue 정적 파일은 Nginx가 제공하고, `/api` 요청은 같은 Docker 네트워크의 Spring 앱으로 프록시한다. 따라서 브라우저에는 API 별도 도메인이나 CORS 설정이 필요하지 않다.

DB는 RDS(MySQL)를 쓴다. Redis는 EC2 안의 컨테이너로 띄운다. 인증 코드와 송금 idempotency 키만 담고 둘 다 TTL이 짧아, 유실 부담보다 운영 비용을 줄이는 쪽을 택했다.

## RDS 준비

앱을 올리기 전에 한 번만 하면 된다.

**1. 파라미터 그룹에서 `time_zone = Asia/Seoul` 로 설정한다.**

RDS 기본값은 UTC다. 앱은 `LocalDateTime.now()`(TZ=Asia/Seoul)로 `expired_at`을 만드는데 조회는 MySQL `NOW()`와 비교하므로, 서버 시간대가 UTC면 9시간 어긋나 승인요청이 제때 만료되지 않는다. JDBC URL의 `serverTimezone`은 드라이버 쪽 해석만 바꿀 뿐 서버의 `NOW()`는 바꾸지 않는다.

**2. 스키마와 시드를 넣는다.**

컨테이너 MySQL과 달리 RDS에는 `docker-entrypoint-initdb.d` 같은 자동 초기화가 없다.

```bash
mysql -h <RDS_ENDPOINT> -u <MYSQL_USER> -p <MYSQL_DATABASE> < ../be/src/main/resources/db/schema.sql
mysql -h <RDS_ENDPOINT> -u <MYSQL_USER> -p <MYSQL_DATABASE> < ../be/src/main/resources/db/data.sql
```

`data.sql`은 선택이 아니다. `risk_rules`(FDS 룰 카탈로그), `banks`, `merchants`가 들어 있어 없으면 송금 위험도 평가가 동작하지 않는다.

**3. RDS 보안 그룹 인바운드 3306을 EC2의 보안 그룹에서만 허용한다.**

## 실행

EC2에서 저장소를 받은 뒤 다음을 실행한다.

```bash
cd deploy
cp .env.example .env
# .env의 RDS_ENDPOINT, MYSQL_PASSWORD, JWT_SECRET, PAIRING_INVITE_BASE_URL을 실제 값으로 변경
docker compose -f docker-compose.ec2.yml up --build -d
```

접속 확인은 `https://paywith.site/healthz`로 한다. 프런트 SPA 라우트는 Nginx의 `try_files` fallback으로 `/index.html`을 반환하며, `/api/*`는 내부 `app:8080` 컨테이너로 전달된다.

## HTTPS

Nginx가 TLS를 끊고 뒤로는 평문 HTTP로 넘긴다. 80은 443으로 308 리다이렉트하되, 갱신 검증이 평문 HTTP로 들어오므로 `/.well-known/acme-challenge/`만 리다이렉트에서 제외한다.

정식 주소는 apex인 `paywith.site` 하나다. `www.paywith.site`는 DNS에서 apex로 CNAME이 걸려 있고 Nginx가 308로 넘긴다. 리다이렉트만 하는 서버 블록에도 인증서 설정이 필요하다 — TLS 핸드셰이크가 리다이렉트보다 먼저이기 때문이며, 같은 `fullchain.pem`이 두 이름을 모두 담는다.

**인증서 발급** — Nginx가 챌린지 경로를 서빙하는 상태에서 한 번만 하면 된다.

```bash
sudo mkdir -p /var/www/certbot/.well-known/acme-challenge
sudo apt-get install -y certbot
sudo certbot certonly --webroot -w /var/www/certbot \
  -d paywith.site -d www.paywith.site \
  --email <메일> --agree-tos --no-eff-email
```

발급 전에 443 블록이 들어간 설정을 올리면 `ssl_certificate` 파일이 없어 Nginx가 아예 뜨지 않는다. 순서를 지켜야 한다. 실패가 잦으면 `--dry-run`으로 먼저 확인한다(Let's Encrypt는 검증 실패가 시간당 5회를 넘으면 도메인을 차단한다).

**갱신 훅** — certbot의 systemd 타이머가 인증서를 갱신해도 컨테이너 Nginx는 그 사실을 모른다. 훅을 걸지 않으면 90일 뒤 만료된 인증서를 계속 물고 있게 된다.

```bash
sudo tee /etc/letsencrypt/renewal-hooks/deploy/reload-nginx.sh > /dev/null <<'EOF'
#!/bin/sh
docker compose --env-file /home/ubuntu/paywith/deploy/.env \
  -f /home/ubuntu/paywith/deploy/docker-compose.ec2.yml exec -T web nginx -s reload
EOF
sudo chmod +x /etc/letsencrypt/renewal-hooks/deploy/reload-nginx.sh
sudo certbot renew --dry-run   # 훅까지 도는지 확인
```

앱 쪽은 `WebAppInitializer`의 `ForwardedHeaderFilter`가 `X-Forwarded-Proto`를 읽어 `https`로 인식한다. 이 필터가 없으면 Swagger "Try it out"이 mixed content로 차단된다.

후속으로 HSTS(`Strict-Transport-Security`)를 붙일 수 있지만, 브라우저가 정책을 캐시해 되돌리기 어려우므로 HTTPS가 안정적으로 도는 것을 확인한 뒤에 적용한다.

## Swagger UI

`/swagger-ui`, `/v2/api-docs`, `/swagger-resources`, `/webjars`는 `/api/` 밖이라 기본 프록시로는 닿지 않는다. Nginx에 정규식 `location`을 따로 두어 앱으로 넘긴다.

`https://paywith.site/swagger-ui/index.html`로 연다. springfox 3이라 2.x의 `/swagger-ui.html`은 없다. Authorize 창에는 토큰만이 아니라 `Bearer eyJ...` 형태로 접두어까지 넣어야 한다(`SwaggerConfig` 참고).

앱 컨테이너의 8080은 `127.0.0.1`에만 바인딩되어 있으므로, Nginx를 거치지 않고 직접 보려면 SSH 터널을 쓴다.

```bash
ssh -i <key>.pem -N -L 8080:localhost:8080 ubuntu@<EC2 공인 IP>
# → http://localhost:8080/swagger-ui/index.html
```

### 닫아야 할 시점

지금은 실사용자 데이터가 없고 SMS·오픈뱅킹·사기계좌가 모두 목이라 공개해 두었다. 다음 중 하나라도 생기면 접근을 제한한다.

- 실제 SMS 프로바이더 연동 — `/api/auth/phone/code`가 `permitAll`이라 발송 비용이 샌다
- 실사용자 데이터 유입

제한은 `fe/apps/web/nginx/default.conf`의 swagger `location`에 Basic Auth를 붙이는 방식이 간단하다.

```nginx
auth_basic "PayWith API Docs";
auth_basic_user_file /etc/nginx/.htpasswd;
```

계정 파일은 EC2에서 `htpasswd -c deploy/htpasswd <아이디>`로 만들고, compose의 `web`에 `./htpasswd:/etc/nginx/.htpasswd:ro`로 마운트한다(자격증명이라 이미지에 굽지 않는다). 호스트에 파일이 없는 상태로 띄우면 Docker가 같은 이름의 디렉터리를 만들어 인증이 깨지므로 순서를 지켜야 한다.

## EC2 보안 그룹

- 인바운드: TCP 443과 80을 허용한다. 80은 443 리다이렉트와 인증서 갱신 검증에 계속 필요하다.
- Redis(6379)는 호스트에 노출하지 않는다. Spring 앱(8080)은 `127.0.0.1`에만 바인딩하므로 외부에서 직접 닿지 않는다.
- SSH(22)는 본인 IP로만 제한한다.
- RDS는 퍼블릭 액세스를 끄고, 인바운드 3306을 EC2 보안 그룹에서만 허용한다.

## 후속 작업

- RDS 자동 백업 보존 기간 설정
- DB 연결을 `sslMode=VERIFY_IDENTITY`로 상향(현재 `REQUIRED`는 암호화만 하고 서버 인증서를 검증하지 않는다). RDS CA 번들을 트러스트스토어에 추가해야 한다
- 시크릿 관리 도입(`.env` 파일 대신 SSM Parameter Store 등)
- Redis를 ElastiCache로 전환(현재는 EC2 컨테이너)
