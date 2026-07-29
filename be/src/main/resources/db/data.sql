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

-- 2) 송금 위험도 배점 규칙  [v2.7 갱신]
--    배점 원칙: 정상 거래에서도 흔한 신호(신규 수취인·중간 금액)는 낮게,
--              사기에서만 관찰되는 신호(위험 메모·분할 송금)는 높게 배분한다.
--    [변경] 금액·시간대·메모를 구간별 rule_code 로 분리. 같은 계열에서는 한 행만 발동한다.
--    [변경] 등급 경계는 application.properties(fds.threshold.caution=25 / fds.threshold.danger=50)
--          안전 0~24 / 주의 25~49 / 위험 50 이상. 총점은 0 하한.
--    [변경] SAFE_ACCOUNT_CHECK 는 시니어 본인이 등록한 계좌에만 적용(-40 → -20).
--          보호자가 등록한 안전계좌는 감점이 아니라 화이트리스트 단축평가로 처리한다.
INSERT INTO risk_rules (rule_code, description, score, is_active) VALUES
                                                                      ('HIGH_AMOUNT_L3',       '고액 송금 3구간(L3 기준액 초과)',                    35, TRUE),
                                                                      ('DIVISION_TRANSFER',    '단시간 내 서로 다른 여러 계좌로 분할 송금',           28, TRUE),
                                                                      ('SUSPICIOUS_MEMO',      '메모에 위험 키워드 포함(검찰·수사·대출 등)',          25, TRUE),
                                                                      ('HIGH_AMOUNT_L2',       '고액 송금 2구간(L2~L3)',                             18, TRUE),
                                                                      ('PENDING_APPROVAL_EXISTS', '승인 대기 중인 송금이 있는 상태에서 추가 송금',    20, TRUE),
                                                                      ('NEW_RECIPIENT',        '처음 송금하는 신규 수취인(send_count=0)',             15, TRUE),
                                                                      ('REPEATED',             '단기간 내 반복 송금',                                14, TRUE),
                                                                      ('NIGHT_TIME_DEEP',      '심야 송금(자정~새벽)',                               14, TRUE),
                                                                      ('HIGH_AMOUNT_L1',       '고액 송금 1구간(L1~L2)',                             10, TRUE),
                                                                      ('NIGHT_TIME_LATE',      '야간 송금(밤~자정)',                                  6, TRUE),
                                                                      ('SAFE_ACCOUNT_CHECK',   '시니어 본인이 안전계좌로 등록한 수취인 감점',        -20, TRUE)
    ON DUPLICATE KEY UPDATE description = VALUES(description), score = VALUES(score), is_active = VALUES(is_active);

-- 2-1) 단축평가(블랙리스트) 항목  [v2.7 신규]
--      점수 합산에는 참여하지 않으므로 score = 0. 발동 시 DANGER 로 즉시 확정되며,
--      발동 내역은 다른 룰과 동일하게 risk_evaluation_details 에 남는다(판정 근거 조회용).
--      확정적으로 위험한 경우만 둔다. 애매한 신호는 점수 룰로 처리해 조합으로 걸러낸다.
--      화이트리스트는 폐지. 안전계좌는 무조건 통과가 아니라 SAFE_ACCOUNT_CHECK 감점으로만 반영한다.
INSERT INTO risk_rules (rule_code, description, score, is_active) VALUES
                                                                      ('BL_REJECTED_RECIPIENT', '보호자가 거절한 이력이 있는 계좌로 재송금 시도', 0, TRUE),
                                                                      ('BL_FRAUD_ACCOUNT',      '사기계좌로 신고된 계좌에 송금',                  0, TRUE)
    ON DUPLICATE KEY UPDATE description = VALUES(description), score = VALUES(score), is_active = VALUES(is_active);

-- BL_FRAUD_ACCOUNT 는 FraudAccountClient 로 조회한다. 더치트 API 를 개인 개발자가 쓸 수 없어
-- 현재는 MockFraudAccountClient(설정 기반 목 데이터)를 쓰며, 실제 연동 시 구현체만 교체한다.
-- (미구현) BL_OVERSEAS_IP — 해외 IP 송금. transactions 에 IP 컬럼 추가와
--          IP→국가 판정 수단(GeoIP DB 등) 확정이 선행되어야 한다.
-- (폐지)   BL_RAPID_NEW_RECIPIENT — DIVISION_TRANSFER 와 판정 소스가 겹쳐 이중 계산이 되므로 제거.
-- (미도입) UNREGISTERED_ACCOUNT — SAFE_ACCOUNT_CHECK 와 판정 소스 중복

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