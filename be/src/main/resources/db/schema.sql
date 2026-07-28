SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

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
                       role        ENUM('SENIOR','GUARD') NOT NULL,
                       name        VARCHAR(50)  NOT NULL,
                       phone       VARCHAR(20)  NOT NULL,
                       password    VARCHAR(255) NOT NULL,               -- BCrypt 해시
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

-- 3. wallets — 전자지갑 (SENIOR 전용, 1인 1지갑) [v2.6]
--    지갑은 role='SENIOR' 회원에게만 생성한다. 보호자는 지갑을 갖지 않으며,
--    보호자의 시니어 지갑 충전 대행은 transactions.initiated_by 로만 표현.
--    (MySQL은 users.role 교차 CHECK 불가 → 회원가입/지갑생성 서비스에서 강제)
CREATE TABLE wallets (
                         wallet_id  BIGINT        NOT NULL AUTO_INCREMENT,
                         user_id    BIGINT        NOT NULL,                -- SENIOR 회원만 (앱 로직 강제)
                         balance    DECIMAL(15,0) NOT NULL DEFAULT 0,      -- 원화, 소수점 없음
                         status     ENUM('ACTIVE','LOCKED') NOT NULL DEFAULT 'ACTIVE',
                         pin        VARCHAR(255)  NOT NULL                 -- 송금/결제 확인용 6자리 PIN(BCrypt 해시)
               COMMENT '송금/결제 확인용 6자리 PIN(BCrypt 해시). users.password(로그인)와 별개',
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
                              status         ENUM('REQUESTED','HELD','APPROVED','PROCESSING','REJECTED','COMPLETED','CANCELED','BLOCKED') NOT NULL
                 COMMENT 'REQUESTED=요청 / HELD=송금FDS 보류(승인대기) / APPROVED=보호자 승인 / PROCESSING=외부 이체 실행 중(FDS·승인 통과 후, 최종 확정 전) / REJECTED=보호자 명시 거절 / COMPLETED=완료 / CANCELED=취소(승인만료 후속 등) / BLOCKED=위치FDS 시스템 즉시차단(결제)',
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

-- =====================================================================
-- 3. 송금 FDS · 보호자 승인
-- =====================================================================

-- 9. risk_rules — 위험도 배점 규칙 [v2.6: THRESHOLD 행 폐지, 순수 배점만]
--    임계값은 application.properties(fds.threshold=40)로 관리.
--    판정 시점 임계값은 risk_evaluations.threshold 에 스냅샷 보존.
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
                                  evaluation_id  BIGINT   NOT NULL AUTO_INCREMENT,
                                  transaction_id BIGINT   NOT NULL,
                                  total_score    INT      NOT NULL,
                                  threshold      INT      NOT NULL,                 -- 판정 시점 임계값 스냅샷(properties 값 복사)
                                  is_held        BOOLEAN  NOT NULL,
                                  evaluated_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
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
