-- =====================================================================
-- 보호자 승인 파트 개발 전용 시드 — 공용 data.sql과 분리 관리
--
-- 승인 API 4종(목록/상세/승인/거절)의 분기를 전부 밟을 수 있게 구성한다.
--   보호자 A(9101) 담당: 시니어 A1(9102), A2(9103)
--   보호자 B(9104) 담당: 시니어 B1(9105)   ← 권한 격리 확인용
--
-- 모든 행이 실제 규칙(expired_at = requested_at + fds.approval.expire-minutes(3시간))을 따른다.
--
-- 승인요청 5건:
--   9102 대기(요청 8분 전   → 만료 172분 후) A2  → 목록 1순위(만료 임박 순 정렬 확인)
--   9101 대기(요청 3분 전   → 만료 177분 후) A1  → 목록 2순위, 상세 200, 승인/거절 성공
--   9103 대기(요청 195분 전 → 만료 15분 지남) A1  → 목록 제외, 상세 404, 승인 409
--                                                (만료 스캔이 곧 EXPIRED 로 바꾼다. 아래 주의 참고)
--   9104 APPROVED (요청 2시간 전)             A1  → 목록 제외, 상세 404, 승인 409
--   9105 대기(요청 6분 전   → 만료 174분 후) B1  → 보호자 A 에게는 404 (담당 아님)
--
-- 주의: 만료 스캔(fds.approval.expire-scan-ms, 기본 60초)이 돌면 9103 은 status 가 PENDING →
--       EXPIRED 로, 대상 거래는 HELD → CANCELED 로 바뀐다. 걸러지는 결과는 같지만 근거가
--       expired_at 조건에서 status 조건으로 옮겨간다. PENDING 인 상태로 확인하려면 시드 적용
--       직후에 조회하거나 스캔 주기를 늘린다.
--
-- 적용 (저장소 루트 기준, MySQL 기동 상태에서):
--   docker compose -f be/docker-compose.yml exec -T mysql \
--     mysql --default-character-set=utf8mb4 -upaywith -ppaywith paywith \
--     < be/src/test/resources/db/dev-approval-seed.sql
--
-- 로그인: POST /api/auth/login 에 phone + password 로 요청한다. 개발용 비밀번호는 모두 "123456"
--         (BCrypt 해시 저장 — dev-payment-seed.sql 이 쓰는 것과 같은 해시).
--         예) {"phone": "010-9101-0001", "password": "123456"}  → 보호자A 토큰 발급
-- 주의: user_id 9100+ 대역을 사용해 결제 파트 시드(9001/9002)와 충돌을 피한다.
--       임계값 스냅샷은 application-local.properties 기준(caution=25, danger=50).
-- =====================================================================

SET NAMES utf8mb4;

-- ① 사용자 (password/pin 은 "123456" 의 BCrypt 해시) ---------------------
SET @dev_hash = '$2y$10$aSquS3bDxHzD/H4i.e7gvON.hZLFRz13/tQe5PgTVE/LFxNbmhpPa';

INSERT INTO users (user_id, role, name, phone, password, birth_date, gender, pin, status) VALUES
    (9101, 'GUARD', '승인테스트 보호자A', '010-9101-0001', @dev_hash, '1970-03-11', '여', @dev_hash, 'ACTIVE'),
    (9102, 'WARD',  '승인테스트 피보호자A1', '010-9102-0002', @dev_hash, '1948-07-02', '남', @dev_hash, 'ACTIVE'),
    (9103, 'WARD',  '승인테스트 피보호자A2', '010-9103-0003', @dev_hash, '1951-11-23', '여', @dev_hash, 'ACTIVE'),
    (9104, 'GUARD', '승인테스트 보호자B', '010-9104-0004', @dev_hash, '1975-05-30', '남', @dev_hash, 'ACTIVE'),
    (9105, 'WARD',  '승인테스트 피보호자B1', '010-9105-0005', @dev_hash, '1946-01-19', '남', @dev_hash, 'ACTIVE')
    ON DUPLICATE KEY UPDATE role = VALUES(role), name = VALUES(name), status = VALUES(status),
        password = VALUES(password), pin = VALUES(pin);

-- ② 페어링 (ACTIVE 만 담당으로 인정) ------------------------------------
INSERT INTO guard_senior (relation_id, guard_id, senior_id, status, connected_at) VALUES
    (9101, 9101, 9102, 'ACTIVE', NOW()),
    (9102, 9101, 9103, 'ACTIVE', NOW()),
    (9103, 9104, 9105, 'ACTIVE', NOW())
    ON DUPLICATE KEY UPDATE status = 'ACTIVE', connected_at = VALUES(connected_at);

-- ③ 지갑 (WARD 전용, 1인 1지갑) -----------------------------------------
INSERT INTO wallets (wallet_id, user_id, balance, status) VALUES
    (9102, 9102, 3000000, 'ACTIVE'),
    (9103, 9103, 1500000, 'ACTIVE'),
    (9105, 9105, 2000000, 'ACTIVE')
    ON DUPLICATE KEY UPDATE balance = VALUES(balance), status = 'ACTIVE';

-- ④ 수취인 (신규 수취인 = send_count 0) ---------------------------------
INSERT INTO recipients (recipient_id, senior_id, bank_code, account_no, holder_name, send_count) VALUES
    (9101, 9102, '088', '110234567890', '박수취', 0),
    (9102, 9103, '004', '9876543210123', '최수취', 0),
    (9103, 9102, '011', '3020199887766', '정수취', 2),
    (9104, 9105, '020', '1002345678901', '한수취', 0)
    ON DUPLICATE KEY UPDATE holder_name = VALUES(holder_name), send_count = VALUES(send_count);

-- ⑤ 보류된 송금 거래 (HELD = 승인 대기, risk_score 는 비정규화 복사본) --
INSERT INTO transactions
    (transaction_id, wallet_id, recipient_id, type, amount, memo, status, risk_score, created_at) VALUES
    (9101, 9102, 9101, 'TRANSFER_OUT', 2000000, '검찰 수사 협조 요청',  'HELD',     58, NOW() - INTERVAL 3 MINUTE),
    (9102, 9103, 9102, 'TRANSFER_OUT',  850000, '급하게 보내달래',      'HELD',     53, NOW() - INTERVAL 8 MINUTE),
    (9103, 9102, 9103, 'TRANSFER_OUT', 1200000, NULL,                   'HELD',     51, NOW() - INTERVAL 45 MINUTE),
    (9104, 9102, 9101, 'TRANSFER_OUT',  600000, '손자 학원비',          'APPROVED', 50, NOW() - INTERVAL 2 HOUR),
    (9105, 9105, 9104, 'TRANSFER_OUT', 1800000, '대출 상환',            'HELD',     58, NOW() - INTERVAL 6 MINUTE)
    ON DUPLICATE KEY UPDATE status = VALUES(status), risk_score = VALUES(risk_score), memo = VALUES(memo);

-- ⑥ 위험도 평가 (거래당 1행, 임계값은 판정 시점 스냅샷) ----------------
INSERT INTO risk_evaluations
    (evaluation_id, transaction_id, total_score, caution_threshold, danger_threshold, risk_level, decided_by) VALUES
    (9101, 9101, 58, 25, 50, 'DANGER', 'RULE'),
    (9102, 9102, 53, 25, 50, 'DANGER', 'RULE'),
    (9103, 9103, 51, 25, 50, 'DANGER', 'RULE'),
    (9104, 9104, 50, 25, 50, 'DANGER', 'RULE'),
    (9105, 9105, 58, 25, 50, 'DANGER', 'RULE')
    ON DUPLICATE KEY UPDATE total_score = VALUES(total_score), risk_level = VALUES(risk_level);

-- ⑦ 발동한 룰 내역 (상세 응답의 ruleHits, 점수 큰 순 정렬 확인용) ------
--    rule_id 는 data.sql 시드 기준: 3=SUSPICIOUS_MEMO(25) 4=HIGH_AMOUNT_L2(18)
--    6=NEW_RECIPIENT(15) 2=DIVISION_TRANSFER(28) 7=REPEATED(14) 8=NIGHT_TIME_DEEP(14)
INSERT INTO risk_evaluation_details (detail_id, evaluation_id, rule_id, score) VALUES
    -- 9101: 메모 25 + 고액L2 18 + 신규수취인 15 = 58
    (9101, 9101, 3, 25), (9102, 9101, 4, 18), (9103, 9101, 6, 15),
    -- 9102: 분할송금 28 + 반복 14 + ... = 53 (신규수취인 15 - 심야 미발동)
    (9104, 9102, 2, 28), (9105, 9102, 6, 15), (9106, 9102, 7, 14),
    -- 9103: 고액L2 18 + 심야 14 + 신규수취인 15 + 반복 14 = 51 (감점 -20 포함 가정 없음)
    (9107, 9103, 4, 18), (9108, 9103, 8, 14), (9109, 9103, 6, 15), (9110, 9103, 7, 14),
    -- 9104: 이미 승인된 건
    (9111, 9104, 4, 18), (9112, 9104, 6, 15), (9113, 9104, 7, 14),
    -- 9105: 다른 보호자 담당 (권한 격리 확인용)
    (9114, 9105, 3, 25), (9115, 9105, 4, 18), (9116, 9105, 6, 15)
    ON DUPLICATE KEY UPDATE score = VALUES(score);

-- ⑧ 승인요청 (보류 송금당 1행) -----------------------------------------
--    expired_at 은 모두 requested_at + 3시간(fds.approval.expire-minutes)으로 맞춘다.
INSERT INTO approval_requests
    (approval_id, transaction_id, status, responded_by, requested_at, responded_at, expired_at) VALUES
    -- 대기 중 (목록 2순위) — 만료 177분 후
    (9101, 9101, 'PENDING',  NULL, NOW() - INTERVAL 3 MINUTE,  NULL, NOW() - INTERVAL 3 MINUTE  + INTERVAL 180 MINUTE),
    -- 대기 중, 만료가 더 임박 (목록 1순위) — 만료 172분 후
    (9102, 9102, 'PENDING',  NULL, NOW() - INTERVAL 8 MINUTE,  NULL, NOW() - INTERVAL 8 MINUTE  + INTERVAL 180 MINUTE),
    -- 이미 만료됨(15분 지남). 시드 직후에는 expired_at 으로 걸러지고, 만료 스캔이 돌면
    -- status = EXPIRED 로 바뀌어 걸러진다. 어느 쪽이든 목록·상세에서 제외돼야 한다
    (9103, 9103, 'PENDING',  NULL, NOW() - INTERVAL 195 MINUTE, NULL, NOW() - INTERVAL 195 MINUTE + INTERVAL 180 MINUTE),
    -- 이미 승인 처리됨 (요청 2시간 전, 10분 뒤 응답)
    (9104, 9104, 'APPROVED', 9101, NOW() - INTERVAL 2 HOUR,    NOW() - INTERVAL 110 MINUTE,
                                                                     NOW() - INTERVAL 2 HOUR   + INTERVAL 180 MINUTE),
    -- 보호자 B 담당 시니어의 건 — 만료 174분 후
    (9105, 9105, 'PENDING',  NULL, NOW() - INTERVAL 6 MINUTE,  NULL, NOW() - INTERVAL 6 MINUTE  + INTERVAL 180 MINUTE)
    ON DUPLICATE KEY UPDATE status = VALUES(status), responded_by = VALUES(responded_by),
        requested_at = VALUES(requested_at), responded_at = VALUES(responded_at),
        expired_at = VALUES(expired_at);
