-- =====================================================================
-- 결제 파트 개발 전용 시드 (B6) — 공용 data.sql과 분리 관리
-- QR 결제 API 로컬 테스트용 최소 구성 5종:
--   ① WARD(피보호자) 사용자 ② GUARDIAN(보호자) 사용자 ③ 둘의 ACTIVE 페어링
--   ④ WARD의 ACTIVE 지갑 + 초기 잔액 ⑤ DevJwtTokenFactory의 userId(9001/9002)와 일치
-- 적용 방법 (Docker MySQL 기동 상태에서, 저장소 루트 기준):
--   docker compose -f be/docker-compose.yml exec -T mysql \
--     mysql -upaywith -ppaywith pay_with < be/src/test/resources/db/dev-payment-seed.sql
-- 주의: password/pin은 로그인에 쓰지 않는 더미 값(B6는 JWT 직접 발급으로 로그인을 우회).
--       user_id 9001+ 대역을 사용해 팀 공용 시드와의 충돌을 피한다.
-- =====================================================================

SET NAMES utf8mb4;

INSERT INTO users (user_id, role, name, phone, password, status) VALUES
    (9001, 'SENIOR', '결제테스트 피보호자', '010-9001-0001', 'dev-seed-dummy-password', 'ACTIVE'),
    (9002, 'GUARD',  '결제테스트 보호자',   '010-9002-0002', 'dev-seed-dummy-password', 'ACTIVE')
    ON DUPLICATE KEY UPDATE role = VALUES(role), name = VALUES(name), status = VALUES(status);

INSERT INTO guard_senior (relation_id, guard_id, senior_id, status, connected_at) VALUES
    (9001, 9002, 9001, 'ACTIVE', NOW())
    ON DUPLICATE KEY UPDATE status = 'ACTIVE', connected_at = VALUES(connected_at);

INSERT INTO wallets (wallet_id, user_id, balance, status, pin) VALUES
    (9001, 9001, 500000, 'ACTIVE', 'dev-seed-dummy-pin')
    ON DUPLICATE KEY UPDATE balance = VALUES(balance), status = 'ACTIVE';
