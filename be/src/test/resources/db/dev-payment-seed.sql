-- =====================================================================
-- 결제 파트 개발 전용 시드 (B6) — 공용 R__seed.sql과 분리 관리
-- QR 결제 API 로컬 테스트용 최소 구성:
--   ① WARD(피보호자) 사용자 ② GUARDIAN(보호자) 사용자 ③ 둘의 ACTIVE 페어링
--   ④ WARD의 ACTIVE 지갑 + 초기 잔액 ⑤ DevJwtTokenFactory의 userId(9001/9002)와 일치
--   ⑥ 좌표 미등록 가맹점 1곳 (merchants 목록 제외·결제 거부 검증용)
-- (2026-07-31 스키마 정합: users.birth_date·gender NOT NULL 추가 반영)
-- 적용 방법 (Docker MySQL 기동 상태에서, 저장소 루트 기준):
--   docker compose -f be/docker-compose.yml exec -T mysql \
--     mysql -upaywith -ppaywith paywith < be/src/test/resources/db/dev-payment-seed.sql
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
-- 공용 R__seed.sql 의 가맹점 5곳은 전부 좌표가 있어 이 케이스를 실 DB에서 확인할 수 없다.
INSERT INTO merchants (merchant_id, name, category_code, region, latitude, longitude) VALUES
    (9001, '결제테스트 좌표미등록상점', 'MART', '서울 종로구', NULL, NULL)
    ON DUPLICATE KEY UPDATE name = VALUES(name), latitude = NULL, longitude = NULL;

-- FDS 카테고리·이동 속도 룰 검증용 가맹점 (PR#4a, 룰별 최소 1케이스 — 설계서 §7-1-b):
--   9002 금은방(JEWELRY)      PAY_RISKY_CATEGORY 단독(9만→CAUTION)·조합(50만→25+35=60 DANGER)
--   9003 전자상가(ELECTRONICS) risky 목록 복수 코드 파싱 확인
--   9004 부산식당(RESTAURANT)  서울 결제 후 수분 내 결제 시 속도>300km/h → PAY_IMPOSSIBLE_TRAVEL.
--                              중립 카테고리(어느 목록에도 없음)라 속도 룰만 격리 검증
--   9005 무분류(NULL, 좌표 O)  category_code NULL 이면 카테고리 룰 3종 스킵 확인
-- 상품권 취급 업종(PAY_GIFT_CARD_AMOUNT/SPLIT) 검증은 공용 시드의 4(CVS)·1(MART)을 그대로 사용.
-- 카테고리 코드 문자열은 properties(fds.payment.*-categories) 목록과 정확히 일치해야 한다
-- (카탈로그 부재로 오타 시 조용히 미발동) — 시드·properties 를 같은 커밋에서 관리.
INSERT INTO merchants (merchant_id, name, category_code, region, latitude, longitude) VALUES
    (9002, '결제테스트 금은방',     'JEWELRY',     '서울 종로구',   37.5700000, 126.9850000),
    (9003, '결제테스트 전자상가',   'ELECTRONICS', '서울 중구',     37.5670000, 126.9930000),
    (9004, '결제테스트 부산식당',   'RESTAURANT',  '부산 해운대구', 35.1587000, 129.1604000),
    (9005, '결제테스트 무분류상점', NULL,          '서울 종로구',   37.5750000, 126.9800000)
    ON DUPLICATE KEY UPDATE name = VALUES(name), category_code = VALUES(category_code),
    region = VALUES(region), latitude = VALUES(latitude), longitude = VALUES(longitude);
