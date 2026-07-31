-- =====================================================================
-- 송금 FDS + 보호자 승인 전 구간 수동 테스트용 시드 — 공용 data.sql과 분리 관리
--
-- 승인요청을 미리 넣어두는 dev-approval-seed.sql 과 달리, 이 시드는 계정과 잔액만
-- 준비한다. 거래·FDS 평가·승인요청은 전부 Swagger 에서 송금 API 를 호출해 만든다.
--
--   보호자 G(9201) 담당: 시니어 5명(9202~9206)
--
-- 시나리오마다 시니어를 나눈 이유: REPEATED / DIVISION_TRANSFER /
-- PENDING_APPROVAL_EXISTS 룰이 전부 wallet_id 기준이라, 한 계정으로 연달아 테스트하면
-- 앞 시나리오의 거래가 뒤 시나리오 점수에 얹힌다. 지갑을 나누면 서로 간섭하지 않는다.
--
-- 적용 (저장소 루트 기준, MySQL 기동 상태에서):
--   docker compose -f be/docker-compose.yml exec -T mysql \
--     mysql --default-character-set=utf8mb4 -upaywith -ppaywith pay_with \
--     < be/src/test/resources/db/dev-transfer-scenario-seed.sql
--
-- 로그인: POST /api/auth/login  {"phone": "010-9201-0001", "password": "123456"}
--         송금 비밀번호(transferPin)도 "123456".
-- Swagger: Authorize 창에 "Bearer eyJ..." 형태로 입력한다(접두어 포함).
--
-- 주의: user_id 9200+ 대역을 사용해 결제(9001/9002)·승인(9101~9105) 시드와 충돌을 피한다.
--       임계값 스냅샷은 application-local.properties 기준(caution=25 / danger=50).
-- =====================================================================

SET NAMES utf8mb4;

-- ① 사용자 (password/pin 은 "123456" 의 BCrypt 해시) ---------------------
SET @dev_hash = '$2y$10$aSquS3bDxHzD/H4i.e7gvON.hZLFRz13/tQe5PgTVE/LFxNbmhpPa';

INSERT INTO users (user_id, role, name, phone, password, birth_date, gender, pin, status) VALUES
    (9201, 'GUARD', '송금테스트 보호자',     '010-9201-0001', @dev_hash, '1972-04-18', '여', @dev_hash, 'ACTIVE'),
    (9202, 'WARD',  '송금테스트 정상',       '010-9202-0002', @dev_hash, '1949-02-14', '남', @dev_hash, 'ACTIVE'),
    (9203, 'WARD',  '송금테스트 주의',       '010-9203-0003', @dev_hash, '1950-06-25', '여', @dev_hash, 'ACTIVE'),
    (9204, 'WARD',  '송금테스트 위험승인',   '010-9204-0004', @dev_hash, '1947-09-30', '남', @dev_hash, 'ACTIVE'),
    (9205, 'WARD',  '송금테스트 잔액부족',   '010-9205-0005', @dev_hash, '1952-12-08', '여', @dev_hash, 'ACTIVE'),
    (9206, 'WARD',  '송금테스트 위험거절',   '010-9206-0006', @dev_hash, '1946-08-03', '남', @dev_hash, 'ACTIVE')
    ON DUPLICATE KEY UPDATE role = VALUES(role), name = VALUES(name), status = VALUES(status),
        password = VALUES(password), pin = VALUES(pin);

-- ② 페어링 (ACTIVE 만 담당으로 인정 — 승인 API 의 담당 조인 조건) --------
INSERT INTO guard_senior (relation_id, guard_id, senior_id, status, connected_at) VALUES
    (9201, 9201, 9202, 'ACTIVE', NOW()),
    (9202, 9201, 9203, 'ACTIVE', NOW()),
    (9203, 9201, 9204, 'ACTIVE', NOW()),
    (9204, 9201, 9205, 'ACTIVE', NOW()),
    (9205, 9201, 9206, 'ACTIVE', NOW())
    ON DUPLICATE KEY UPDATE status = 'ACTIVE', connected_at = VALUES(connected_at);

-- ③ 지갑 (WARD 전용, 1인 1지갑) -----------------------------------------
--    9205 만 잔액을 송금액(3,500,000)보다 적게 둬서 승인 후 잔액 부족을 재현한다.
--    보류 시점에는 잔액을 보지 않으므로(차감은 승인 후 executeCompletion 에서만 일어난다)
--    잔액이 모자라도 HELD 까지는 정상 진행된다.
INSERT INTO wallets (wallet_id, user_id, balance, status) VALUES
    (9202, 9202,  1000000, 'ACTIVE'),
    (9203, 9203,  5000000, 'ACTIVE'),
    (9204, 9204, 10000000, 'ACTIVE'),
    (9205, 9205,   500000, 'ACTIVE'),
    (9206, 9206, 10000000, 'ACTIVE')
    ON DUPLICATE KEY UPDATE balance = VALUES(balance), status = 'ACTIVE';

-- ④ 이전 실행 흔적 정리 --------------------------------------------------
--    같은 시드로 반복 테스트할 때 앞선 거래가 REPEATED/DIVISION/PENDING_APPROVAL
--    점수에 얹히는 것을 막는다. FK 참조 순서대로 지운다.
DELETE red FROM risk_evaluation_details red
    JOIN risk_evaluations re ON re.evaluation_id = red.evaluation_id
    JOIN transactions t      ON t.transaction_id = re.transaction_id
    WHERE t.wallet_id BETWEEN 9202 AND 9206;

DELETE ar FROM approval_requests ar
    JOIN transactions t ON t.transaction_id = ar.transaction_id
    WHERE t.wallet_id BETWEEN 9202 AND 9206;

DELETE re FROM risk_evaluations re
    JOIN transactions t ON t.transaction_id = re.transaction_id
    WHERE t.wallet_id BETWEEN 9202 AND 9206;

-- 현재 삽입 코드는 없지만 transactions 를 FK 로 참조하므로 함께 정리한다.
DELETE lrr FROM llm_risk_reviews lrr
    JOIN transactions t ON t.transaction_id = lrr.transaction_id
    WHERE t.wallet_id BETWEEN 9202 AND 9206;

DELETE FROM transactions WHERE wallet_id BETWEEN 9202 AND 9206;
DELETE FROM recipients   WHERE senior_id BETWEEN 9202 AND 9206;
