-- =====================================================================
-- PAY-WITH 시드 데이터 (ERD v2.6)
-- banks / risk_rules / merchants
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
                                                                      ('HIGH_AMOUNT_L3',       '매우 큰 금액을 송금했어요.',                         35, TRUE),
                                                                      ('DIVISION_TRANSFER',    '짧은 시간 동안 여러 계좌로 나누어 송금했어요.',      28, TRUE),
                                                                      ('SUSPICIOUS_MEMO',      '송금 메모에 검찰·수사·대출 등 주의가 필요한 표현이 있어요.', 25, TRUE),
                                                                      ('HIGH_AMOUNT_L2',       '큰 금액을 송금했어요.',                              18, TRUE),
                                                                      ('PENDING_APPROVAL_EXISTS', '이미 승인을 기다리는 송금이 있는데 추가로 송금했어요.', 20, TRUE),
                                                                      ('NEW_RECIPIENT',        '처음 송금하는 계좌예요.',                            15, TRUE),
                                                                      ('REPEATED',             '짧은 시간에 송금을 여러 번 했어요.',                 14, TRUE),
                                                                      ('NIGHT_TIME_DEEP',      '자정 이후 새벽 시간에 송금했어요.',                  14, TRUE),
                                                                      ('HIGH_AMOUNT_L1',       '비교적 큰 금액을 송금했어요.',                       10, TRUE),
                                                                      ('NIGHT_TIME_LATE',      '늦은 밤 시간에 송금했어요.',                          6, TRUE),
                                                                      ('SAFE_ACCOUNT_CHECK',   '본인이 안전계좌로 등록한 계좌에 송금했어요.',       -20, TRUE)
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
                                                                      ('BL_REJECTED_RECIPIENT', '보호자가 이전에 거절한 계좌로 다시 송금하려고 했어요.', 100, TRUE),
                                                                      ('BL_FRAUD_ACCOUNT',      '사기 계좌로 신고된 계좌에 송금하려고 했어요.',          100, TRUE)
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
--      REPEATED 40 은 위험 업종 반복 3건째가 40만대(RISKY+L2+REPEATED=126→상한 100)부터 차단으로
--      넘어가되 소액 반복(RISKY+REPEATED=90)은 주의에 머물도록 정한 값 — SPLIT 이 위험 업종을
--      배제(isGiftCardSuspect)해 생기던 금액 분할 회피 구멍을 막는다. 임의 조정 금지.
INSERT INTO risk_rules (rule_code, description, score, is_active) VALUES
                                                                      ('PAY_SPLIT_PAYMENT',    '짧은 시간에 상품권으로 의심되는 결제를 여러 번 했어요.', 80, TRUE),
                                                                      ('PAY_HIGH_AMOUNT_L3',   '매우 큰 금액을 결제했어요.',                             70, TRUE),
                                                                      ('PAY_RISKY_CATEGORY',   '현금화하기 쉬운 귀금속·전자제품 등을 결제했어요.',       50, TRUE),
                                                                      ('PAY_RISKY_REPEATED',   '현금화하기 쉬운 업종에서 짧은 시간에 결제를 여러 번 했어요.', 40, TRUE),
                                                                      ('PAY_PENDING_APPROVAL', '승인을 기다리는 송금이 있는데 추가로 결제했어요.',       40, TRUE),
                                                                      ('PAY_HIGH_AMOUNT_L2',   '큰 금액을 결제했어요.',                                  36, TRUE),
                                                                      ('PAY_NIGHT_DEEP',       '자정 이후 새벽 시간에 결제했어요.',                      28, TRUE),
                                                                      ('PAY_HIGH_AMOUNT_L1',   '비교적 큰 금액을 결제했어요.',                           20, TRUE),
                                                                      ('PAY_GIFT_CARD_AMOUNT', '상품권 구매로 의심되는 결제예요.',                       20, TRUE),
                                                                      ('PAY_NIGHT_LATE',       '늦은 밤 시간에 결제했어요.',                             12, TRUE)
    AS new
    ON DUPLICATE KEY UPDATE description = new.description, score = new.score, is_active = new.is_active;

-- 2-2-1) 결제 단축평가 — 송금 BL_* 와 같은 위상(발동 즉시 DANGER, details 에 근거 기록).
--        결제 DANGER 는 승인 보류가 아니라 즉시 거절(BLOCKED)로 처리된다.
--        [변경] score 0 → 100. 송금 BL_* 와 같은 이유다 — 차단해 놓고 조회 화면에는 0점으로 뜨고,
--              risk_score 로 정렬·비교하면 안전 거래와 구분되지 않았다(QA 지적).
--              PaymentFdsEvaluationServiceImpl.evaluateShortcut 도 이 배점을 그대로 총점으로 쓴다
--              (하드코딩 0 제거). 결제 위험 임계값이 100 이라 만점이 곧 차단이다.
INSERT INTO risk_rules (rule_code, description, score, is_active) VALUES
                                                                      ('PAY_IMPOSSIBLE_TRAVEL', '직전 결제 위치에서 이동하기 어려운 장소에서 결제했어요.', 100, TRUE)
    AS new
    ON DUPLICATE KEY UPDATE description = new.description, score = new.score, is_active = new.is_active;

-- 3) 시연용 더미 가맹점
--    1~5 는 서울권(종로·중구) — 평상시 생활 반경 결제를 만드는 용도.
--    9001~ 은 제주권. 서울 결제 직후 여기서 결제하면 PAY_IMPOSSIBLE_TRAVEL 이 발동한다.
--    종로-제주시 대권거리가 약 454km 이고 임계가 300km/h(fds.payment.impossible-speed-kmh)라,
--    직전 COMPLETED 결제와 90분 이내면 걸린다. 기준 결제는 24시간 안에 있어야 한다.
--    번호대를 9001 부터 띄운 것은 서울 더미(1~5)가 뒤에 늘어나도 겹치지 않게 하기 위함이다.
--    created_at 은 컬럼 DEFAULT(CURRENT_TIMESTAMP)에 맡긴다 — UPDATE 절에 없으므로
--    이미 들어가 있는 행의 값은 재실행해도 그대로 남는다.
INSERT INTO merchants (merchant_id, name, category_code, region, latitude, longitude) VALUES
                                                                                          (1, '행복마트 종로점',   'MART',       '서울 종로구',  37.5729000, 126.9793000),
                                                                                          (2, '정든약국',          'PHARMACY',   '서울 종로구',  37.5710000, 126.9820000),
                                                                                          (3, '한마음경로식당',    'RESTAURANT', '서울 종로구',  37.5735000, 126.9768000),
                                                                                          (4, '우리동네편의점',    'CVS',        '서울 중구',    37.5636000, 126.9976000),
                                                                                          (5, '서울대병원 원무과', 'HOSPITAL',   '서울 종로구',  37.5799000, 126.9987000),
                                                                                          (9001, '한라식당',          'RESTAURANT', '제주 제주시',   33.4996000, 126.5312000),
                                                                                          (9002, '탐라마트 서귀포점', 'MART',       '제주 서귀포시', 33.2541000, 126.5601000),
                                                                                          (9003, '성산일출봉편의점',  'CVS',        '제주 서귀포시', 33.4581000, 126.9425000)
    AS new
    ON DUPLICATE KEY UPDATE name = new.name, category_code = new.category_code, region = new.region,
    latitude = new.latitude, longitude = new.longitude;

-- 3-1) 위험 업종 시연 가맹점 (9301~)
--     공용 가맹점이 전부 생활 업종(MART·PHARMACY·RESTAURANT·CVS·HOSPITAL)이라 위험 업종 룰
--     PAY_RISKY_CATEGORY(50점, 2위 배점)가 발동할 가맹점이 하나도 없었다 — 로컬 dev 시드에만
--     있어서 팀원·운영 환경에서는 룰이 죽어 있던 것을 여기서 살린다.
--     발동 산수(결제 임계 주의 50 / 차단 100):
--       금은방  9만 원 = RISKY 50            → CAUTION (결제는 완료, 보호자 알림)
--       금은방 50만 원 = RISKY 50 + L3 70 = 120 → 100 클램프, DANGER (403 차단)
--     좌표는 서울 더미(1~5)와 도보권(종로)이라 어떤 순서로 시연해도 PAY_IMPOSSIBLE_TRAVEL 이
--     끼어들지 않는다. 번호대 9301~ 은 9001~(제주)·9901~(결제 로컬 시드)과, 그리고 타 파트
--     dev 시드의 user_id 대역(9101~ 승인·9200+ 송금·9401~ 보호자 홈)과도 겹치지 않는 빈 대역이다.
--     카테고리 문자열은 properties(fds.payment.risky-categories) 목록과 정확히 일치해야 한다
--     (카탈로그가 없어 오타 시 조용히 미발동).
INSERT INTO merchants (merchant_id, name, category_code, region, latitude, longitude) VALUES
                                                                                          (9301, '종로귀금속',   'JEWELRY',     '서울 종로구', 37.5710000, 126.9880000),
                                                                                          (9302, '세운전자상가', 'ELECTRONICS', '서울 종로구', 37.5680000, 126.9940000)
    AS new
    ON DUPLICATE KEY UPDATE name = new.name, category_code = new.category_code, region = new.region,
    latitude = new.latitude, longitude = new.longitude;
