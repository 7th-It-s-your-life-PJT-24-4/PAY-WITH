-- =====================================================================
-- 보호자 홈 이상 거래 건수 확인용 개발 시드
--
-- 한 보호자에게 정상 피보호자와 승인 대기 이상 거래 2건이 있는 피보호자를 연결한다.
-- 기본 선택된 정상 피보호자에서는 다른 아바타의 빨간 테두리를 확인할 수 있고,
-- 이상 거래 피보호자를 선택하면 선택 테두리와 "위험 거래 2건 발생" 문구를 확인할 수 있다.
--
-- 적용 (저장소 루트 기준, MySQL 기동 상태에서):
--   docker compose -f be/docker-compose.yml exec -T mysql \
--     sh -c 'mysql --default-character-set=utf8mb4 -u"$MYSQL_USER" \
--       -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE"' \
--     < be/src/test/resources/db/dev-guard-home-seed.sql
--
-- 로그인: 전화번호 010-9401-0001 / 비밀번호 123456
-- 간편비밀번호(PIN): 123456
--
-- user_id 9400+ 대역을 사용하며, 재실행하면 같은 행을 최신 시각 기준으로 갱신한다.
-- =====================================================================

SET NAMES utf8mb4;

-- password와 pin은 "123456"의 BCrypt 해시다.
SET @dev_hash = '$2y$10$aSquS3bDxHzD/H4i.e7gvON.hZLFRz13/tQe5PgTVE/LFxNbmhpPa';

INSERT INTO users
    (user_id, role, name, phone, password, birth_date, gender, avatar_id, pin, status) VALUES
    (9401, 'GUARD', '홈테스트 보호자', '01094010001', @dev_hash, '1975-04-12', '여', 2, @dev_hash, 'ACTIVE'),
    (9402, 'WARD',  '김두건',          '01094020002', @dev_hash, '1948-09-21', '남', 4, @dev_hash, 'ACTIVE'),
    (9403, 'WARD',  '한평온',          '01094030003', @dev_hash, '1950-02-15', '여', 3, @dev_hash, 'ACTIVE')
    AS new
    ON DUPLICATE KEY UPDATE role = new.role, name = new.name, phone = new.phone,
        password = new.password, birth_date = new.birth_date, gender = new.gender,
        avatar_id = new.avatar_id, pin = new.pin, status = new.status;

INSERT INTO guard_senior
    (relation_id, guard_id, senior_id, status, connected_at) VALUES
    (9401, 9401, 9402, 'ACTIVE', NOW()),
    (9402, 9401, 9403, 'ACTIVE', NOW() - INTERVAL 1 DAY)
    AS new
    ON DUPLICATE KEY UPDATE status = new.status, connected_at = new.connected_at,
        revoked_at = NULL;

INSERT INTO wallets (wallet_id, user_id, balance, status) VALUES
    (9402, 9402, 3000000, 'ACTIVE'),
    (9403, 9403, 1200000, 'ACTIVE')
    AS new
    ON DUPLICATE KEY UPDATE balance = new.balance, status = new.status;

INSERT INTO recipients
    (recipient_id, senior_id, bank_code, account_no, holder_name, send_count) VALUES
    (9401, 9402, '088', '110940100001', '박긴급', 0),
    (9402, 9402, '004', '123456940202', '이의심', 0)
    AS new
    ON DUPLICATE KEY UPDATE holder_name = new.holder_name, send_count = new.send_count;

INSERT INTO transactions
    (transaction_id, wallet_id, recipient_id, type, amount, memo, status, risk_score, created_at) VALUES
    (9401, 9402, 9401, 'TRANSFER_OUT', 2000000, '검찰 수사 협조 요청', 'HELD', 58, NOW() - INTERVAL 3 MINUTE),
    (9402, 9402, 9402, 'TRANSFER_OUT',  850000, '급하게 나눠서 송금', 'HELD', 57, NOW() - INTERVAL 8 MINUTE)
    AS new
    ON DUPLICATE KEY UPDATE wallet_id = new.wallet_id, recipient_id = new.recipient_id,
        type = new.type, amount = new.amount, memo = new.memo, status = new.status,
        risk_score = new.risk_score, created_at = new.created_at;

INSERT INTO risk_evaluations
    (evaluation_id, transaction_id, total_score, caution_threshold, danger_threshold, risk_level, decided_by, evaluated_at) VALUES
    (9401, 9401, 58, 25, 50, 'DANGER', 'RULE', NOW() - INTERVAL 3 MINUTE),
    (9402, 9402, 57, 25, 50, 'DANGER', 'RULE', NOW() - INTERVAL 8 MINUTE)
    AS new
    ON DUPLICATE KEY UPDATE total_score = new.total_score,
        caution_threshold = new.caution_threshold, danger_threshold = new.danger_threshold,
        risk_level = new.risk_level, decided_by = new.decided_by,
        evaluated_at = new.evaluated_at;

SET @rule_suspicious_memo = (
    SELECT rule_id FROM risk_rules WHERE rule_code = 'SUSPICIOUS_MEMO'
);
SET @rule_high_amount_l2 = (
    SELECT rule_id FROM risk_rules WHERE rule_code = 'HIGH_AMOUNT_L2'
);
SET @rule_new_recipient = (
    SELECT rule_id FROM risk_rules WHERE rule_code = 'NEW_RECIPIENT'
);
SET @rule_division_transfer = (
    SELECT rule_id FROM risk_rules WHERE rule_code = 'DIVISION_TRANSFER'
);
SET @rule_repeated = (
    SELECT rule_id FROM risk_rules WHERE rule_code = 'REPEATED'
);

INSERT INTO risk_evaluation_details
    (detail_id, evaluation_id, rule_id, score) VALUES
    (9401, 9401, @rule_suspicious_memo, 25),
    (9402, 9401, @rule_high_amount_l2, 18),
    (9403, 9401, @rule_new_recipient, 15),
    (9404, 9402, @rule_division_transfer, 28),
    (9405, 9402, @rule_new_recipient, 15),
    (9406, 9402, @rule_repeated, 14)
    AS new
    ON DUPLICATE KEY UPDATE evaluation_id = new.evaluation_id,
        rule_id = new.rule_id, score = new.score;

-- 화면 확인 도중 자동 만료되지 않도록 개발 시드의 유효기간은 7일로 둔다.
INSERT INTO approval_requests
    (approval_id, transaction_id, status, responded_by, requested_at, responded_at, expired_at) VALUES
    (9401, 9401, 'PENDING', NULL, NOW() - INTERVAL 3 MINUTE, NULL,
        NOW() + INTERVAL 7 DAY),
    (9402, 9402, 'PENDING', NULL, NOW() - INTERVAL 8 MINUTE, NULL,
        NOW() + INTERVAL 7 DAY)
    AS new
    ON DUPLICATE KEY UPDATE status = new.status, responded_by = new.responded_by,
        requested_at = new.requested_at, responded_at = new.responded_at,
        expired_at = new.expired_at;
