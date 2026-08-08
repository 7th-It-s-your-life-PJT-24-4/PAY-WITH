-- =====================================================================
-- PAY-WITH 시드 데이터 (ERD v2.6)
-- banks / risk_rules / payment_anomaly_rules / merchants
--
-- Repeatable 마이그레이션(R__)이라 버전이 없고, 모든 V 파일이 끝난 뒤에 실행된다.
-- 이 파일의 내용이 바뀌면 다음 기동 때 자동으로 다시 실행된다 — FDS 룰 배점처럼
-- 자주 조정되는 값을 새 V 파일 없이 반영하기 위한 구조다.
-- 모든 INSERT 가 ON DUPLICATE KEY UPDATE 라 몇 번 실행돼도 결과가 같다.
--
-- ⚠ 이 파일은 추가·수정만 반영한다. 여기서 행을 지워도 DB 에서는 사라지지 않는다.
--   UPSERT 만 하고 DELETE 를 하지 않기 때문이다. risk_rules 는 특히 주의해야 하는데,
--   RiskRuleCache 가 is_active 인 행을 전부 읽어 평가기와 매칭하므로 폐기했다고 생각한
--   룰이 계속 발동한다.
--   → 룰 폐기·rule_code 변경은 이 파일이 아니라 versioned migration 에서 명시적으로
--     UPDATE risk_rules SET is_active = FALSE WHERE rule_code = '...' 로 처리한다.
--
-- 문법: MySQL 8.0.20 부터 ON DUPLICATE KEY UPDATE 의 VALUES() 가 deprecated 라
--       행 별칭(AS new)을 쓴다. 로컬(docker mysql:8.4)과 RDS(8.4) 모두 지원한다.
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
    AS new
    ON DUPLICATE KEY UPDATE bank_name = new.bank_name, is_active = new.is_active;

-- 2) 송금 위험도 배점 규칙  [v2.7 갱신]
--    배점 원칙: 정상 거래에서도 흔한 신호(신규 수취인·중간 금액)는 낮게,
--              사기에서만 관찰되는 신호(위험 메모·분할 송금)는 높게 배분한다.
--    [변경] 금액·시간대·메모를 구간별 rule_code 로 분리. 같은 계열에서는 한 행만 발동한다.
--    [변경] 등급 경계는 application.properties(fds.threshold.caution=25 / fds.threshold.danger=50)
--          안전 0~24 / 주의 25~49 / 위험 50 이상.
--          총점은 0~100 으로 자른다(RiskGrader.MAX_SCORE). 배점 합은 상한을 넘을 수 있으나
--          (여러 룰 동시 발동 시 최대 151) 그 구간은 이미 전부 위험이라 등급은 영향받지 않는다.
--          배점 합을 100 에 맞추려 재배점하지 않는다. 룰을 추가할 때마다 전 항목을 다시 튜닝해야 한다.
--          결제 룰(2-2)은 임계값과 배점을 따로 쓴다 — 여기 값을 바꿔도 결제 판정은 움직이지 않는다.
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
    AS new
    ON DUPLICATE KEY UPDATE description = new.description, score = new.score, is_active = new.is_active;

-- 2-1) 단축평가(블랙리스트) 항목  [v2.7 신규]
--      룰 합산에는 참여하지 않는다(평가기 빈이 없어 순회에서 건너뛴다). 발동 시 DANGER 로 즉시
--      확정되며, 이때 이 배점이 그대로 총점이 된다.
--      [변경] score 0 → 100. 확정 위험이므로 만점을 준다. 0 이면 조회 화면에 '위험한데 0점' 으로
--            보이고, risk_score 로 정렬·비교할 때 안전 거래와 구분되지 않는다.
--      [변경] 발동 시 보호자 승인을 받지 않고 거래를 BLOCKED 로 즉시 차단한다. 점수 룰로 DANGER 가
--            된 건만 HELD(승인 대기)로 간다. 승인 요청이 없으므로 보호자가 나중에 풀어줄 수도 없다.
--      발동 내역은 다른 룰과 동일하게 risk_evaluation_details 에 남는다(판정 근거 조회용).
--      확정적으로 위험한 경우만 둔다. 애매한 신호는 점수 룰로 처리해 조합으로 걸러낸다.
--      화이트리스트는 폐지. 안전계좌는 무조건 통과가 아니라 SAFE_ACCOUNT_CHECK 감점으로만 반영한다.
INSERT INTO risk_rules (rule_code, description, score, is_active) VALUES
                                                                      ('BL_REJECTED_RECIPIENT', '보호자가 거절한 이력이 있는 계좌로 재송금 시도', 100, TRUE),
                                                                      ('BL_FRAUD_ACCOUNT',      '사기계좌로 신고된 계좌에 송금',                  100, TRUE)
    AS new
    ON DUPLICATE KEY UPDATE description = new.description, score = new.score, is_active = new.is_active;

-- BL_FRAUD_ACCOUNT 는 FraudAccountClient 로 조회한다. 더치트 API 를 개인 개발자가 쓸 수 없어
-- 현재는 MockFraudAccountClient(설정 기반 목 데이터)를 쓰며, 실제 연동 시 구현체만 교체한다.
-- (미구현) BL_OVERSEAS_IP — 해외 IP 송금. transactions 에 IP 컬럼 추가와
--          IP→국가 판정 수단(GeoIP DB 등) 확정이 선행되어야 한다.
-- (폐지)   BL_RAPID_NEW_RECIPIENT — DIVISION_TRANSFER 와 판정 소스가 겹쳐 이중 계산이 되므로 제거.
-- (미도입) UNREGISTERED_ACCOUNT — SAFE_ACCOUNT_CHECK 와 판정 소스 중복

-- 2-2) 결제 FDS 룰 (PR#4) — 임계값·목록은 properties(fds.payment.*)로 외부화. 송금 판정 서비스는
--      평가기 빈이 없는 rule_code 를 건너뛰므로 PAY_* 행 추가는 송금 판정에 영향이 없다
--      (결제 서비스도 대칭으로 무해).
--      [변경] 배점 일괄 2배 + 결제 전용 임계값(주의 50 / 위험 100)으로 분리. 차단된 결제가 100점
--            만점에 50점으로 보여 "절반만 위험한 거래"로 읽힌다는 QA 지적을 반영한 것이며, 전 항목을
--            같은 비율로 올려 설계서 §4-2 의 조합 관계는 그대로 유지된다. 송금은 배점이 그대로라
--            임계값을 공유할 수 없어 fds.payment.threshold.* 를 따로 뒀다(PaymentRiskGrader).
--      SPLIT 80 은 분할(80+L1=100 차단)이 일괄 구매(L3+GIFT=90 알림)보다 불리하도록 정한 값,
--      RISKY 50 은 단독 발동이 정확히 주의 문턱이 되도록 정한 값 — 임의 조정 금지(설계서 §4-2).
INSERT INTO risk_rules (rule_code, description, score, is_active) VALUES
                                                                      ('PAY_SPLIT_PAYMENT',    '상품권 의심 결제의 단기간 반복(고액 FDS 회피 분할 의심)',      80, TRUE),
                                                                      ('PAY_HIGH_AMOUNT_L3',   '고액 결제 3구간(L3 기준액 이상)',                              70, TRUE),
                                                                      ('PAY_RISKY_CATEGORY',   '위험 업종 결제(귀금속·전자제품 등 현금 교환 용이 물품)',       50, TRUE),
                                                                      ('PAY_PENDING_APPROVAL', '승인 대기 송금이 있는 상태의 결제',                            40, TRUE),
                                                                      ('PAY_HIGH_AMOUNT_L2',   '고액 결제 2구간(L2~L3)',                                       36, TRUE),
                                                                      ('PAY_NIGHT_DEEP',       '심야 결제(자정~새벽)',                                         28, TRUE),
                                                                      ('PAY_HIGH_AMOUNT_L1',   '고액 결제 1구간(L1~L2)',                                       20, TRUE),
                                                                      ('PAY_GIFT_CARD_AMOUNT', '상품권 취급 업종에서 단위 배수 금액 결제(상품권 의심)',        20, TRUE),
                                                                      ('PAY_NIGHT_LATE',       '야간 결제(밤~자정)',                                           12, TRUE)
    AS new
    ON DUPLICATE KEY UPDATE description = new.description, score = new.score, is_active = new.is_active;

-- 2-2-1) 결제 단축평가 — 송금 BL_* 와 같은 위상(발동 즉시 DANGER, details 에 근거 기록).
--        결제 DANGER 는 승인 보류가 아니라 즉시 거절(BLOCKED)로 처리된다.
--        [변경] score 0 → 100. 송금 BL_* 와 같은 이유다 — 차단해 놓고 조회 화면에는 0점으로 뜨고,
--              risk_score 로 정렬·비교하면 안전 거래와 구분되지 않았다(QA 지적).
--              PaymentFdsEvaluationServiceImpl.evaluateShortcut 도 이 배점을 그대로 총점으로 쓴다
--              (하드코딩 0 제거). 결제 위험 임계값이 100 이라 만점이 곧 차단이다.
INSERT INTO risk_rules (rule_code, description, score, is_active) VALUES
                                                                      ('PAY_IMPOSSIBLE_TRAVEL', '직전 결제 대비 물리적으로 불가능한 이동 속도', 100, TRUE)
    AS new
    ON DUPLICATE KEY UPDATE description = new.description, score = new.score, is_active = new.is_active;

-- 3) 결제 이상 규칙 카탈로그 (IMPOSSIBLE_TRAVEL만 즉시 차단)  [변경 없음]
INSERT INTO payment_anomaly_rules (rule_code, description, default_action, is_active) VALUES
                                                                                          ('HIGH_AMOUNT_PAYMENT',  '평소 대비 고액 결제',       'NOTIFY', TRUE),
                                                                                          ('OUT_OF_ZONE',          '생활 반경 밖 결제',         'NOTIFY', TRUE),
                                                                                          ('IMPOSSIBLE_TRAVEL',    '물리적으로 불가능한 이동',  'BLOCK',  TRUE),
                                                                                          ('MISSED_ROUTINE_VISIT', '루틴 방문 이탈',            'NOTIFY', TRUE)
    AS new
    ON DUPLICATE KEY UPDATE description = new.description, default_action = new.default_action, is_active = new.is_active;

-- 4) 시연용 더미 가맹점 (좌표는 서울 기준 예시)  [변경 없음]
INSERT INTO merchants (merchant_id, name, category_code, region, latitude, longitude) VALUES
                                                                                          (1, '행복마트 종로점',   'MART',       '서울 종로구',  37.5729000, 126.9793000),
                                                                                          (2, '정든약국',          'PHARMACY',   '서울 종로구',  37.5710000, 126.9820000),
                                                                                          (3, '한마음경로식당',    'RESTAURANT', '서울 종로구',  37.5735000, 126.9768000),
                                                                                          (4, '우리동네편의점',    'CVS',        '서울 중구',    37.5636000, 126.9976000),
                                                                                          (5, '서울대병원 원무과', 'HOSPITAL',   '서울 종로구',  37.5799000, 126.9987000)
    AS new
    ON DUPLICATE KEY UPDATE name = new.name, category_code = new.category_code, region = new.region,
    latitude = new.latitude, longitude = new.longitude;