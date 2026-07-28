-- =====================================================================
-- PAY-WITH 시드 데이터 (ERD v2.6)
-- schema.sql 실행 후 적용. banks / risk_rules / payment_anomaly_rules / merchants
-- =====================================================================

SET NAMES utf8mb4;

-- 1) 은행 마스터 (금융결제원 표준 코드)  [변경 없음]
INSERT INTO banks (bank_code, bank_name, is_active) VALUES
                                                        ('002', '한국산업은행',   TRUE),
                                                        ('003', 'IBK기업은행',    TRUE),
                                                        ('004', 'KB국민은행',     TRUE),
                                                        ('007', '수협은행',       TRUE),
                                                        ('011', 'NH농협은행',     TRUE),
                                                        ('020', '우리은행',       TRUE),
                                                        ('023', 'SC제일은행',     TRUE),
                                                        ('027', '한국씨티은행',   TRUE),
                                                        ('031', 'DGB대구은행',    TRUE),
                                                        ('032', 'BNK부산은행',    TRUE),
                                                        ('034', '광주은행',       TRUE),
                                                        ('035', '제주은행',       TRUE),
                                                        ('037', '전북은행',       TRUE),
                                                        ('039', 'BNK경남은행',    TRUE),
                                                        ('045', '새마을금고',     TRUE),
                                                        ('048', '신협',           TRUE),
                                                        ('071', '우체국예금보험', TRUE),
                                                        ('081', '하나은행',       TRUE),
                                                        ('088', '신한은행',       TRUE),
                                                        ('089', '케이뱅크',       TRUE),
                                                        ('090', '카카오뱅크',     TRUE),
                                                        ('092', '토스뱅크',       TRUE)
    ON DUPLICATE KEY UPDATE bank_name = VALUES(bank_name), is_active = VALUES(is_active);

-- 2) 송금 위험도 배점 규칙  [v2.6 갱신]
--    가점 6종 합계 = 100 · 감점 SAFE_ACCOUNT_CHECK = -40
--    [변경] THRESHOLD 행 폐지 → 임계값은 application.properties(fds.threshold=40)
--    [변경] UNREGISTERED_ACCOUNT 미도입(SAFE_ACCOUNT_CHECK와 판정 소스 중복)
INSERT INTO risk_rules (rule_code, description, score, is_active) VALUES
                                                                      ('NEW_RECIPIENT',      '처음 송금하는 신규 수취인(send_count=0)',        24, TRUE),
                                                                      ('HIGH_AMOUNT',        '고액 송금(기준액 이상)',                        20, TRUE),
                                                                      ('DIVISION_TRANSFER',  '단시간 내 서로 다른 여러 계좌로 분할 송금',     20, TRUE),
                                                                      ('REPEATED',           '단기간 내 반복 송금',                           16, TRUE),
                                                                      ('NIGHT_TIME',         '심야 시간대 송금',                              12, TRUE),
                                                                      ('SUSPICIOUS_MEMO',    '메모에 위험 키워드 포함(가점 전용)',             8, TRUE),
                                                                      ('SAFE_ACCOUNT_CHECK', '안전/안심 등록 계좌 감점(사용자가 신뢰 지정)', -40, TRUE)
    ON DUPLICATE KEY UPDATE description = VALUES(description), score = VALUES(score), is_active = VALUES(is_active);

-- (미도입) UNREGISTERED_ACCOUNT — 필요 시 아래 주석 해제
-- INSERT INTO risk_rules (rule_code, description, score, is_active) VALUES
--   ('UNREGISTERED_ACCOUNT', '안전/안심 미등록 계좌로 송금', 10, FALSE)
-- ON DUPLICATE KEY UPDATE score = VALUES(score), is_active = VALUES(is_active);

-- 3) 결제 이상 규칙 카탈로그 (IMPOSSIBLE_TRAVEL만 즉시 차단)  [변경 없음]
INSERT INTO payment_anomaly_rules (rule_code, description, default_action, is_active) VALUES
                                                                                          ('HIGH_AMOUNT_PAYMENT',  '평소 대비 고액 결제',       'NOTIFY', TRUE),
                                                                                          ('OUT_OF_ZONE',          '생활 반경 밖 결제',         'NOTIFY', TRUE),
                                                                                          ('IMPOSSIBLE_TRAVEL',    '물리적으로 불가능한 이동',  'BLOCK',  TRUE),
                                                                                          ('MISSED_ROUTINE_VISIT', '루틴 방문 이탈',            'NOTIFY', TRUE)
    ON DUPLICATE KEY UPDATE description = VALUES(description), default_action = VALUES(default_action), is_active = VALUES(is_active);

-- 4) 시연용 더미 가맹점 (좌표는 서울 기준 예시)  [변경 없음]
INSERT INTO merchants (merchant_id, name, category_code, region, latitude, longitude) VALUES
                                                                                          (1, '행복마트 종로점',   'MART',       '서울 종로구',  37.5729000, 126.9793000),
                                                                                          (2, '정든약국',          'PHARMACY',   '서울 종로구',  37.5710000, 126.9820000),
                                                                                          (3, '한마음경로식당',    'RESTAURANT', '서울 종로구',  37.5735000, 126.9768000),
                                                                                          (4, '우리동네편의점',    'CVS',        '서울 중구',    37.5636000, 126.9976000),
                                                                                          (5, '서울대병원 원무과', 'HOSPITAL',   '서울 종로구',  37.5799000, 126.9987000)
    ON DUPLICATE KEY UPDATE name = VALUES(name), category_code = VALUES(category_code), region = VALUES(region),
    latitude = VALUES(latitude), longitude = VALUES(longitude);