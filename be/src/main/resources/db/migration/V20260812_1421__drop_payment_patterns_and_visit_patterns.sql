-- =====================================================================
-- 결제 이상감지 v2.5 잔재 정리 2차 — #184 후속
--
-- payment_patterns 는 시니어별 결제 기준선(평균·최대 금액, 활동 시간대,
-- 활동 중심 좌표·반경)을, merchant_visit_patterns 는 시니어×가맹점 방문
-- 패턴(방문 횟수·평균 간격)을 미리 집계해 두고 이상을 판정하려던 v2.5
-- 설계의 산물이다. 실제 구현은 패턴 테이블을 따로 쌓지 않고 판정 시점에
-- 원장(transactions·payment_requests)을 직접 집계하는 쪽으로 갔고
-- (PaymentRuleContextCollectorImpl), 두 테이블을 읽거나 쓰는 코드는 매퍼·
-- 도메인 포함해 하나도 없다. 채우는 코드가 없으니 모든 환경에서 0행이다.
--
-- #184(V20260808_0936)가 같은 잔재 4개 중 payment_anomaly_rules/logs 를
-- 정리했고, 남은 이 2개를 마저 지운다. R__seed 에 시드가 없고 두 테이블을
-- 참조하는 FK 도 없어 이 파일 하나로 끝난다(DROP 순서 무관).
-- =====================================================================

DROP TABLE IF EXISTS payment_patterns;
DROP TABLE IF EXISTS merchant_visit_patterns;
