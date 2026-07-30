CREATE DATABASE IF NOT EXISTS pay_with;
USE pay_with;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS payment_requests;
DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS payment_anomaly_logs;
DROP TABLE IF EXISTS merchant_visit_patterns;
DROP TABLE IF EXISTS payment_anomaly_rules;
DROP TABLE IF EXISTS payment_patterns;
DROP TABLE IF EXISTS llm_risk_reviews;
DROP TABLE IF EXISTS approval_requests;
DROP TABLE IF EXISTS risk_evaluation_details;
DROP TABLE IF EXISTS risk_evaluations;
DROP TABLE IF EXISTS risk_rules;
DROP TABLE IF EXISTS transactions;
DROP TABLE IF EXISTS recipients;
DROP TABLE IF EXISTS linked_accounts;
DROP TABLE IF EXISTS merchants;
DROP TABLE IF EXISTS banks;
DROP TABLE IF EXISTS wallets;
DROP TABLE IF EXISTS guard_senior;
DROP TABLE IF EXISTS users;

SET FOREIGN_KEY_CHECKS = 1;

-- =====================================================================
-- 1. 회원 · 페어링
-- =====================================================================

-- 1. users — 회원 (시니어/보호자 공용, 전화번호 로그인)
CREATE TABLE users (
                       user_id     BIGINT       NOT NULL AUTO_INCREMENT,
                       role        ENUM('WARD','GUARD') NOT NULL,
                       name        VARCHAR(50)  NOT NULL,
                       phone       VARCHAR(20)  NOT NULL,
                       password    VARCHAR(255) NOT NULL,               -- BCrypt 해시
                       birth_date  DATE         NOT NULL,
                       gender      CHAR(1)      NOT NULL COMMENT '남 또는 여',
                       pin         VARCHAR(255) NOT NULL COMMENT '결제/충전 확인용 6자리 PIN(BCrypt 해시). WARD는 결제, GUARD는 충전 대행 시 사용. password(로그인)와 별개',
                       fcm_token   VARCHAR(255) NULL,
                       status      ENUM('PENDING_PAIRING','ACTIVE','WITHDRAWN') NOT NULL DEFAULT 'PENDING_PAIRING',
                       created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                       PRIMARY KEY (user_id),
                       UNIQUE KEY uk_users_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. guard_senior — 시니어-보호자 연동 관계 (N:M 중간 테이블)
CREATE TABLE guard_senior (
                                 relation_id  BIGINT   NOT NULL AUTO_INCREMENT,
                                 guard_id     BIGINT   NOT NULL,
                                 senior_id    BIGINT   NOT NULL,
                                 status       ENUM('PENDING','ACTIVE','REJECTED','REVOKED') NOT NULL DEFAULT 'PENDING'
               COMMENT 'PENDING=연동요청 / ACTIVE=승인 / REJECTED=연동거절 / REVOKED=해제. 여기 REJECTED는 페어링 거절이며 transactions.REJECTED와 무관',
                                 requested_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 connected_at DATETIME NULL,
                                 revoked_at   DATETIME NULL,
                                 PRIMARY KEY (relation_id),
                                 UNIQUE KEY uk_guard_senior (guard_id, senior_id),
                                 KEY idx_gs_senior (senior_id),
                                 CONSTRAINT fk_gs_guard   FOREIGN KEY (guard_id)   REFERENCES users (user_id),
                                 CONSTRAINT fk_gs_senior   FOREIGN KEY (senior_id)   REFERENCES users (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================================
-- 2. 지갑 · 거래
-- =====================================================================

-- 3. wallets — 전자지갑 (WARD 전용, 1인 1지갑) [v2.6]
--    지갑은 role='WARD' 회원에게만 생성한다. 보호자는 지갑을 갖지 않으며,
--    보호자의 시니어 지갑 충전 대행은 transactions.initiated_by 로만 표현.
--    (MySQL은 users.role 교차 CHECK 불가 → 회원가입/지갑생성 서비스에서 강제)
CREATE TABLE wallets (
                         wallet_id  BIGINT        NOT NULL AUTO_INCREMENT,
                         user_id    BIGINT        NOT NULL,                -- WARD 회원만 (앱 로직 강제)
                         balance    DECIMAL(15,0) NOT NULL DEFAULT 0,      -- 원화, 소수점 없음
                         status     ENUM('ACTIVE','LOCKED') NOT NULL DEFAULT 'ACTIVE',
                         created_at DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         updated_at DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                         PRIMARY KEY (wallet_id),
                         UNIQUE KEY uk_wallets_user (user_id),             -- 1인 1지갑 강제
                         CONSTRAINT fk_wallets_user FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. banks — 은행 마스터 (금결원 표준 코드)
CREATE TABLE banks (
                       bank_code VARCHAR(10) NOT NULL,
                       bank_name VARCHAR(50) NOT NULL,
                       is_active BOOLEAN     NOT NULL DEFAULT TRUE,
                       PRIMARY KEY (bank_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. linked_accounts — 연동 계좌 (충전 출금원, 1인 N계좌)
CREATE TABLE linked_accounts (
                                 account_id  BIGINT      NOT NULL AUTO_INCREMENT,
                                 user_id     BIGINT      NOT NULL,
                                 bank_code   VARCHAR(10) NOT NULL,
                                 account_no  VARCHAR(30) NOT NULL,
                                 holder_name VARCHAR(50) NOT NULL,
                                 is_verified BOOLEAN     NOT NULL DEFAULT FALSE,
                                 created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 PRIMARY KEY (account_id),
                                 KEY idx_la_user (user_id),
                                 CONSTRAINT uk_linked_accounts_account UNIQUE (user_id, bank_code, account_no),
                                 CONSTRAINT fk_la_user FOREIGN KEY (user_id)   REFERENCES users (user_id),
                                 CONSTRAINT fk_la_bank FOREIGN KEY (bank_code) REFERENCES banks (bank_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. recipients — 송금 수취인 원장 (안전/안심 계좌 통합, v2.5)
CREATE TABLE recipients (
                            recipient_id       BIGINT      NOT NULL AUTO_INCREMENT,
                            senior_id          BIGINT      NOT NULL,
                            bank_code          VARCHAR(10) NOT NULL,
                            account_no         VARCHAR(30) NOT NULL,
                            holder_name        VARCHAR(50) NOT NULL,
                            send_count         INT         NOT NULL DEFAULT 0,
                            first_sent_at      DATETIME    NULL,
                            last_sent_at       DATETIME    NULL,
                            is_registered_safe BOOLEAN     NOT NULL DEFAULT FALSE,   -- SAFE_ACCOUNT_CHECK 감점 판정에 사용
                            safe_registered_at DATETIME    NULL,
                            safe_registered_by BIGINT      NULL,               -- NULL=시니어 본인 지정, 값=해당 보호자
                            account_alias      VARCHAR(50) NULL,
                            PRIMARY KEY (recipient_id),
                            UNIQUE KEY uk_recipients_account (senior_id, bank_code, account_no),
                            CONSTRAINT fk_recipients_senior   FOREIGN KEY (senior_id)          REFERENCES users (user_id),
                            CONSTRAINT fk_recipients_regby    FOREIGN KEY (safe_registered_by) REFERENCES users (user_id),
                            CONSTRAINT fk_recipients_bank     FOREIGN KEY (bank_code)          REFERENCES banks (bank_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. merchants — 가맹점 마스터 (결제 상대 원장 + 위치 FDS 기준점)
CREATE TABLE merchants (
                           merchant_id   BIGINT        NOT NULL AUTO_INCREMENT,
                           name          VARCHAR(100)  NOT NULL,
                           category_code VARCHAR(30)   NULL,
                           region        VARCHAR(100)  NULL,
                           latitude      DECIMAL(10,7) NULL,
                           longitude     DECIMAL(10,7) NULL,
                           created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           PRIMARY KEY (merchant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. transactions — 통합 거래 원장 (충전/결제/송금, v2.5 / v2.6 인덱스·주석)
CREATE TABLE transactions (
                              transaction_id BIGINT        NOT NULL AUTO_INCREMENT,
                              wallet_id      BIGINT        NOT NULL,
                              recipient_id   BIGINT        NULL,                 -- TRANSFER_OUT 시
                              merchant_id    BIGINT        NULL,                 -- PAYMENT 시
                              account_id     BIGINT        NULL,                 -- CHARGE(계좌) 시
                              type           ENUM('CHARGE','PAYMENT','TRANSFER_OUT') NOT NULL,
                              initiated_by   BIGINT        NULL,                 -- 보호자 충전 시 보호자, NULL=본인
                              amount         DECIMAL(15,0) NOT NULL,
                              memo           VARCHAR(200)  NULL,
                              balance_after  DECIMAL(15,0) NULL,
                              status         ENUM('REQUESTED','HELD','APPROVED','PROCESSING','REJECTED','COMPLETED','CANCELED','BLOCKED','FAILED') NOT NULL
                 COMMENT 'REQUESTED=요청(거래 행 선생성, FDS 판정 전) / HELD=송금FDS 위험(DANGER) 보류(승인대기) / APPROVED=보호자 승인 / PROCESSING=외부 이체 실행 중(FDS·승인 통과 후, 최종 확정 전) / REJECTED=보호자 명시 거절 / COMPLETED=완료 / CANCELED=취소(승인만료 후속 등) / BLOCKED=위치FDS 시스템 즉시차단(결제 전용) / FAILED=외부 이체(deposit) 호출 이후 확정 처리 실패, 잔액은 이미 차감됨 → 재시도 금지, 수동 정산·재조회 필요. 송금은 블랙리스트 확정 건도 HELD 로 보내 보호자 판단을 받는다',
                              -- [v2.7] 위험도(risk_evaluations.risk_level)와 처리 단계(status)는 별개 축이다.
                              --        SAFE/CAUTION 은 동일하게 PROCESSING → COMPLETED 로 진행하며, 주의 판정 여부는 risk_level 로만 구분한다.
                              -- [v2.7] APPROVED/REJECTED 는 approval_requests.status 의 비정규화 복사본이다(risk_score 와 동일한 single writer 규칙).
                              --        승인 처리 서비스가 approval_requests 와 이 컬럼을 같은 트랜잭션에서 함께 갱신한다. 다른 경로에서 단독 갱신 금지.
                              latitude       DECIMAL(10,7) NULL,
                              longitude      DECIMAL(10,7) NULL,
                              pg_payment_key VARCHAR(100)  NULL,
                              risk_score     INT           NULL                  -- [v2.6] 비정규화 복사본. TransferService가 FDS 평가 직후 risk_evaluations insert와 동일 트랜잭션에서 단독 갱신(single writer). 코어 거래 로직은 미갱신.
                 COMMENT 'risk_evaluations.total_score 비정규화 복사본(FDS 서비스 단독 갱신)',
                              created_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              completed_at   DATETIME      NULL,
                              PRIMARY KEY (transaction_id),
                              KEY idx_tx_wallet_created (wallet_id, created_at),               -- 거래내역 조회용
                              KEY idx_tx_wallet_type_created (wallet_id, type, created_at),    -- [v2.6] 송금 속도 룰(REPEATED/DIVISION) 전용
                              KEY idx_tx_recipient (recipient_id),
                              KEY idx_tx_merchant (merchant_id),
                              CONSTRAINT fk_tx_wallet    FOREIGN KEY (wallet_id)    REFERENCES wallets (wallet_id),
                              CONSTRAINT fk_tx_recipient FOREIGN KEY (recipient_id) REFERENCES recipients (recipient_id),
                              CONSTRAINT fk_tx_merchant  FOREIGN KEY (merchant_id)  REFERENCES merchants (merchant_id),
                              CONSTRAINT fk_tx_account   FOREIGN KEY (account_id)   REFERENCES linked_accounts (account_id),
                              CONSTRAINT fk_tx_initiator FOREIGN KEY (initiated_by) REFERENCES users (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8-1. payment_requests — QR 결제 요청 수명주기 [v2.7 신설 · 결제 파트 소유]
--      QR 생성은 실제 거래가 아닌 임시 결제 요청 → 생성/만료/취소/성공/실패 상태의 최종 기준은 이 테이블.
--      transactions에는 실제 완료된 결제와 FDS 차단(BLOCKED) 거래만 원장으로 기록.
--      Redis qr:{token}(TTL 60초)은 토큰 유효성 보조 저장소 — 중복 결제 최종 판단은
--      이 테이블의 행 잠금(SELECT ... FOR UPDATE) + 상태 검사로 수행 (2026-07-24 A2 승인).
CREATE TABLE payment_requests (
                                  payment_id     BIGINT        NOT NULL AUTO_INCREMENT,
                                  wallet_id      BIGINT        NOT NULL,
                                  senior_id      BIGINT        NOT NULL,
                                  qr_token       VARCHAR(64)   NOT NULL,               -- 일회용 QR 토큰. 토큰 생성 후 INSERT(NOT NULL)
                                  status         ENUM('PENDING','PROCESSING','COMPLETED','FAILED','EXPIRED','CANCELED') NOT NULL DEFAULT 'PENDING'
                 COMMENT 'PENDING=생성·미스캔 / PROCESSING=스캔됨 / COMPLETED=차감 완료 / FAILED=잔액 부족·FDS 차단 / EXPIRED=60초 경과 / CANCELED=피보호자 취소',
                                  transaction_id BIGINT        NULL,                   -- 결제 성공 시 원장 거래 연결. UNIQUE — 거래 1건은 결제 요청 1건에만
                                  merchant_id    BIGINT        NULL,                   -- 스캔 시 확정 (생성 시점 NULL)
                                  amount         DECIMAL(15,0) NULL,                   -- 스캔 시 확정 (생성 시점 NULL)
                                  failure_code   VARCHAR(30)   NULL,                   -- INSUFFICIENT_BALANCE / FDS_BLOCKED(transactions.BLOCKED 행 동반) 등
                                  expires_at     DATETIME      NOT NULL,               -- 생성 +60초. 경과 시 조회에서 EXPIRED로 lazy 전이
                                  created_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  updated_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                  PRIMARY KEY (payment_id),
                                  UNIQUE KEY uk_pr_token (qr_token),
                                  UNIQUE KEY uk_pr_transaction (transaction_id),
                                  KEY idx_pr_senior_created (senior_id, created_at),
                                  CONSTRAINT fk_pr_wallet      FOREIGN KEY (wallet_id)      REFERENCES wallets (wallet_id),
                                  CONSTRAINT fk_pr_senior      FOREIGN KEY (senior_id)      REFERENCES users (user_id),
                                  CONSTRAINT fk_pr_transaction FOREIGN KEY (transaction_id) REFERENCES transactions (transaction_id),
                                  CONSTRAINT fk_pr_merchant    FOREIGN KEY (merchant_id)    REFERENCES merchants (merchant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================================
-- 3. 송금 FDS · 보호자 승인
-- =====================================================================

-- 9. risk_rules — 위험도 배점 규칙 [v2.6: THRESHOLD 행 폐지, 순수 배점만]
--    임계값은 application.properties(fds.threshold.caution / fds.threshold.danger)로 관리.
--    판정 시점 임계값은 risk_evaluations.caution_threshold / danger_threshold 에 스냅샷 보존.
--    [v2.7] 단축평가(블랙/화이트리스트) 항목도 rule_code 행으로 등록한다.
--          점수 합산에는 참여하지 않지만 발동 내역은 risk_evaluation_details 에 동일하게 남긴다.
--    [v2.7] 금액·시간대·메모는 구간별로 rule_code 를 분리한다(예: HIGH_AMOUNT_L1/L2/L3).
--          같은 계열에서는 한 행만 발동한다.
CREATE TABLE risk_rules (
                            rule_id     BIGINT       NOT NULL AUTO_INCREMENT,
                            rule_code   VARCHAR(30)  NOT NULL,
                            description VARCHAR(200) NOT NULL,
                            score       INT          NOT NULL,                -- 배점. 음수 허용(감점: SAFE_ACCOUNT_CHECK)
                            is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
                            PRIMARY KEY (rule_id),
                            UNIQUE KEY uk_risk_rules_code (rule_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 10. risk_evaluations — 위험도 평가 결과 (거래당 1행)
CREATE TABLE risk_evaluations (
                                  evaluation_id     BIGINT   NOT NULL AUTO_INCREMENT,
                                  transaction_id    BIGINT   NOT NULL,
                                  total_score       INT      NOT NULL,               -- 룰 합산 점수. 0 하한(감점으로 음수가 되면 0으로 절삭)
                                  caution_threshold INT      NOT NULL,               -- 판정 시점 임계값 스냅샷(properties 값 복사)
                                  danger_threshold  INT      NOT NULL,
                                  risk_level        ENUM('SAFE','CAUTION','DANGER') NOT NULL
                 COMMENT 'SAFE=정상 송금 / CAUTION=보호자 알림 후 송금 진행 / DANGER=보호자 승인 전까지 보류(transactions.HELD)',
                                  decided_by        ENUM('BLACKLIST','WHITELIST','RULE') NOT NULL
                 COMMENT '판정 경로. BLACKLIST/WHITELIST=단축평가로 확정(total_score와 무관) / RULE=룰 점수 합산 결과',
                                  evaluated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  PRIMARY KEY (evaluation_id),
                                  UNIQUE KEY uk_re_transaction (transaction_id),
                                  CONSTRAINT fk_re_transaction FOREIGN KEY (transaction_id) REFERENCES transactions (transaction_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 11. risk_evaluation_details — 규칙별 적용 내역 스냅샷
CREATE TABLE risk_evaluation_details (
                                         detail_id     BIGINT NOT NULL AUTO_INCREMENT,
                                         evaluation_id BIGINT NOT NULL,
                                         rule_id       BIGINT NOT NULL,
                                         score         INT    NOT NULL,                    -- 적용 시점 점수 스냅샷
                                         PRIMARY KEY (detail_id),
                                         KEY idx_red_evaluation (evaluation_id),
                                         CONSTRAINT fk_red_evaluation FOREIGN KEY (evaluation_id) REFERENCES risk_evaluations (evaluation_id),
                                         CONSTRAINT fk_red_rule       FOREIGN KEY (rule_id)       REFERENCES risk_rules (rule_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 12. approval_requests — 보호자 승인 요청 (보류 송금당 1행)
CREATE TABLE approval_requests (
                                   approval_id    BIGINT   NOT NULL AUTO_INCREMENT,
                                   transaction_id BIGINT   NOT NULL,
                                   status         ENUM('PENDING','APPROVED','REJECTED','EXPIRED') NOT NULL DEFAULT 'PENDING'
                 COMMENT 'PENDING=승인대기 / APPROVED=승인 / REJECTED=거절 / EXPIRED=승인시간 초과(대상 거래는 CANCELED)',
                                   responded_by   BIGINT   NULL,
                                   requested_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   responded_at   DATETIME NULL,
                                   expired_at     DATETIME NOT NULL,
                                   PRIMARY KEY (approval_id),
                                   UNIQUE KEY uk_ar_transaction (transaction_id),
                                   KEY idx_ar_status_expired (status, expired_at),
                                   CONSTRAINT fk_ar_transaction FOREIGN KEY (transaction_id) REFERENCES transactions (transaction_id),
                                   CONSTRAINT fk_ar_responder   FOREIGN KEY (responded_by)   REFERENCES users (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 13. llm_risk_reviews — LLM 사후 재검토 (v2.5 신규)
CREATE TABLE llm_risk_reviews (
                                  review_id      BIGINT        NOT NULL AUTO_INCREMENT,
                                  transaction_id BIGINT        NOT NULL,
                                  evaluation_id  BIGINT        NULL,
                                  trigger_score  INT           NOT NULL,
                                  verdict        ENUM('SUSPICIOUS','NORMAL') NOT NULL,
                                  reason         VARCHAR(1000) NULL,
                                  input_summary  VARCHAR(2000) NULL,
                                  model_name     VARCHAR(50)   NULL,
                                  is_notified    BOOLEAN       NOT NULL DEFAULT FALSE,
                                  reviewed_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  PRIMARY KEY (review_id),
                                  UNIQUE KEY uk_lrr_transaction (transaction_id),
                                  CONSTRAINT fk_lrr_transaction FOREIGN KEY (transaction_id) REFERENCES transactions (transaction_id),
                                  CONSTRAINT fk_lrr_evaluation  FOREIGN KEY (evaluation_id)  REFERENCES risk_evaluations (evaluation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================================
-- 4. 결제 패턴 감지 · 알림
-- =====================================================================

-- 14. payment_patterns — 개인별 결제 기준선 (시니어 1인 1행)
CREATE TABLE payment_patterns (
                                  pattern_id        BIGINT        NOT NULL AUTO_INCREMENT,
                                  senior_id         BIGINT        NOT NULL,
                                  avg_amount        DECIMAL(15,0) NULL,
                                  max_amount        DECIMAL(15,0) NULL,
                                  active_hour_start TINYINT       NULL,              -- 0-23
                                  active_hour_end   TINYINT       NULL,              -- 0-23
                                  center_lat        DECIMAL(10,7) NULL,
                                  center_lng        DECIMAL(10,7) NULL,
                                  radius_km         DECIMAL(6,2)  NULL,
                                  updated_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                  PRIMARY KEY (pattern_id),
                                  UNIQUE KEY uk_pp_senior (senior_id),
                                  CONSTRAINT fk_pp_senior FOREIGN KEY (senior_id) REFERENCES users (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 15. payment_anomaly_rules — 결제 이상 규칙 카탈로그 (v2.5 신규)
CREATE TABLE payment_anomaly_rules (
                                       rule_id        BIGINT       NOT NULL AUTO_INCREMENT,
                                       rule_code      VARCHAR(30)  NOT NULL,
                                       description    VARCHAR(200) NOT NULL,
                                       default_action ENUM('NOTIFY','BLOCK') NOT NULL DEFAULT 'NOTIFY',
                                       is_active      BOOLEAN      NOT NULL DEFAULT TRUE,
                                       PRIMARY KEY (rule_id),
                                       UNIQUE KEY uk_par_code (rule_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 16. merchant_visit_patterns — 가맹점별 방문 패턴 (v2.5 신규, 루틴 이탈)
CREATE TABLE merchant_visit_patterns (
                                         pattern_id        BIGINT       NOT NULL AUTO_INCREMENT,
                                         senior_id         BIGINT       NOT NULL,
                                         merchant_id       BIGINT       NOT NULL,
                                         visit_count       INT          NOT NULL DEFAULT 0,
                                         avg_interval_days DECIMAL(6,2) NULL,
                                         last_visited_at   DATETIME     NULL,
                                         updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                         PRIMARY KEY (pattern_id),
                                         UNIQUE KEY uk_mvp_senior_merchant (senior_id, merchant_id),
                                         CONSTRAINT fk_mvp_senior   FOREIGN KEY (senior_id)   REFERENCES users (user_id),
                                         CONSTRAINT fk_mvp_merchant FOREIGN KEY (merchant_id) REFERENCES merchants (merchant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 17. payment_anomaly_logs — 결제(+위치) 이상 감지 기록 [v2.6: anomaly_logs 리네임]
--     송금 이상은 risk_evaluations 계열에 기록되며 이 테이블에는 들어오지 않는다.
CREATE TABLE payment_anomaly_logs (
                                      anomaly_id     BIGINT       NOT NULL AUTO_INCREMENT,
                                      transaction_id BIGINT       NOT NULL,
                                      senior_id      BIGINT       NOT NULL,
                                      rule_id        BIGINT       NOT NULL,
                                      action         ENUM('NOTIFY','BLOCK') NOT NULL DEFAULT 'NOTIFY',
                                      description    VARCHAR(300)  NULL,
                                      is_reviewed    BOOLEAN       NOT NULL DEFAULT FALSE,
                                      resolved_at    DATETIME      NULL,
                                      resolved_by    BIGINT        NULL,
                                      detected_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                      PRIMARY KEY (anomaly_id),
                                      KEY idx_pal_senior_detected (senior_id, detected_at),
                                      CONSTRAINT fk_pal_transaction FOREIGN KEY (transaction_id) REFERENCES transactions (transaction_id),
                                      CONSTRAINT fk_pal_senior      FOREIGN KEY (senior_id)      REFERENCES users (user_id),
                                      CONSTRAINT fk_pal_rule        FOREIGN KEY (rule_id)        REFERENCES payment_anomaly_rules (rule_id),
                                      CONSTRAINT fk_pal_resolver    FOREIGN KEY (resolved_by)    REFERENCES users (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 18. notifications — 알림함
CREATE TABLE notifications (
                               notification_id BIGINT       NOT NULL AUTO_INCREMENT,
                               user_id         BIGINT       NOT NULL,
                               type            ENUM('APPROVAL_REQUEST','APPROVAL_RESULT','ANOMALY','PAIRING','TRANSACTION') NOT NULL,
                               title           VARCHAR(100) NOT NULL,
                               body            VARCHAR(500) NOT NULL,
                               ref_type        VARCHAR(30)  NULL,
                               ref_id          BIGINT       NULL,
                               is_read         BOOLEAN      NOT NULL DEFAULT FALSE,
                               created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               PRIMARY KEY (notification_id),
                               KEY idx_noti_user_read_created (user_id, is_read, created_at),
                               CONSTRAINT fk_noti_user FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
