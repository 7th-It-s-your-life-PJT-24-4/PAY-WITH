-- =====================================================================
-- 결제 이상감지 v2.5 잔재 정리
--
-- payment_anomaly_rules / payment_anomaly_logs 는 결제 FDS 를 별도 카탈로그로
-- 굴리려던 설계의 산물이다. 실제 구현은 risk_rules 에 PAY_* 룰을 넣는 쪽으로
-- 갔고(PaymentFdsEvaluationServiceImpl 이 RiskRuleCache 를 탄다), 두 테이블을
-- 읽거나 쓰는 코드는 매퍼·도메인 포함해 하나도 없다. payment_anomaly_rules 의
-- IMPOSSIBLE_TRAVEL 은 risk_rules.PAY_IMPOSSIBLE_TRAVEL 과 같은 개념의 사본이라
-- 남겨두면 어느 쪽이 진짜인지 헷갈린다.
--
-- transactions.pg_payment_key 는 PG 연동을 전제로 잡아둔 자리다. INSERT 문에
-- 자리는 있지만 값을 채우는 코드가 없어 전 행이 NULL 이고, 결제는 지갑 잔액
-- 차감으로만 끝나 PG 를 태우지 않는다. 응답 DTO 에도 실리지 않는다.
--
-- FK 때문에 logs -> rules 순으로 지운다.
-- =====================================================================

DROP TABLE IF EXISTS payment_anomaly_logs;
DROP TABLE IF EXISTS payment_anomaly_rules;

ALTER TABLE transactions DROP COLUMN pg_payment_key;
