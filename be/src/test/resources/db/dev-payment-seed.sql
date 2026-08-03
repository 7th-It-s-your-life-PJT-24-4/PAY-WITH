-- =====================================================================
-- 결제 파트 개발 전용 시드 (B6) — 공용 data.sql과 분리 관리
-- QR 결제 API 로컬 테스트용 최소 구성:
--   ① WARD(피보호자) 사용자 ② GUARDIAN(보호자) 사용자 ③ 둘의 ACTIVE 페어링
--   ④ WARD의 ACTIVE 지갑 + 초기 잔액 ⑤ DevJwtTokenFactory의 userId(9001/9002)와 일치
--   ⑥ 좌표 미등록 가맹점 1곳 (merchants 목록 제외·결제 거부 검증용)
-- (2026-07-31 스키마 정합: users.birth_date·gender NOT NULL 추가 반영)
-- 적용 방법 (Docker MySQL 기동 상태에서, 저장소 루트 기준):
--   docker compose -f be/docker-compose.yml exec -T mysql \
--     mysql -upaywith -ppaywith pay_with < be/src/test/resources/db/dev-payment-seed.sql
-- 주의: users.password는 로그인에 쓰지 않는 더미 값(B6는 JWT 직접 발급으로 로그인을 우회).
--       users.pin은 결제 비밀번호 검증(A7)에 실제 사용 — 개발용 PIN은 "123456" (BCrypt 해시 저장).
--       (2026-07-30 스키마 이전: pin 저장 위치 wallets.pin → users.pin, role SENIOR → WARD)
--       user_id 9001+ 대역을 사용해 팀 공용 시드와의 충돌을 피한다.
-- =====================================================================

SET NAMES utf8mb4;

INSERT INTO users (user_id, role, name, phone, password, birth_date, gender, pin, status) VALUES
    (9001, 'WARD',  '결제테스트 피보호자', '010-9001-0001', 'dev-seed-dummy-password', '1948-05-10', '여', '$2y$10$aSquS3bDxHzD/H4i.e7gvON.hZLFRz13/tQe5PgTVE/LFxNbmhpPa', 'ACTIVE'),
    (9002, 'GUARD', '결제테스트 보호자',   '010-9002-0002', 'dev-seed-dummy-password', '1975-11-02', '남', '$2y$10$aSquS3bDxHzD/H4i.e7gvON.hZLFRz13/tQe5PgTVE/LFxNbmhpPa', 'ACTIVE')
    ON DUPLICATE KEY UPDATE role = VALUES(role), name = VALUES(name), pin = VALUES(pin), status = VALUES(status);

INSERT INTO guard_senior (relation_id, guard_id, senior_id, status, connected_at) VALUES
    (9001, 9002, 9001, 'ACTIVE', NOW())
    ON DUPLICATE KEY UPDATE status = 'ACTIVE', connected_at = VALUES(connected_at);

INSERT INTO wallets (wallet_id, user_id, balance, status) VALUES
    (9001, 9001, 500000, 'ACTIVE')
    ON DUPLICATE KEY UPDATE balance = VALUES(balance), status = 'ACTIVE';

-- 좌표 미등록 가맹점: GET /merchants 목록 제외·결제 실행 거부(A1) 검증용.
-- 공용 data.sql의 가맹점 5곳은 전부 좌표가 있어 이 케이스를 실 DB에서 확인할 수 없다.
INSERT INTO merchants (merchant_id, name, category_code, region, latitude, longitude) VALUES
    (9001, '결제테스트 좌표미등록상점', 'MART', '서울 종로구', NULL, NULL)
    ON DUPLICATE KEY UPDATE name = VALUES(name), latitude = NULL, longitude = NULL;
