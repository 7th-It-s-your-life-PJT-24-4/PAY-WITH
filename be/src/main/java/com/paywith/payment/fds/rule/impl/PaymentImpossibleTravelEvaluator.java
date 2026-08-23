package com.paywith.payment.fds.rule.impl;

import com.paywith.payment.fds.domain.LastCompletedPayment;
import com.paywith.payment.fds.rule.PaymentRuleContext;
import java.math.BigDecimal;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 단축평가 — 직전 결제와의 이동 속도가 임계(기본 400km/h)를 초과하면 즉시 위험.
 * 상용 교통수단으로 도달 불가능한 영역만 잡는다("확정적 신호만 둔다" 원칙) — 국내 최장인
 * 김포-제주(약 450km)를 항공으로 이동하며 탑승 직전·착륙 직후 결제해도 실효 속도는
 * 약 320km/h 라 임계 아래다.
 *
 * <p>{@code PaymentRiskRuleEvaluator}를 구현하지 않는 것은 의도다 — 점수 룰 순회에 섞이면
 * score 0 합산으로 무해화되므로, 판정 서비스가 점수 계산 전에 별도로 호출한다(송금의 블랙리스트
 * 단축평가와 같은 위상). 발동 내역은 risk_rules 의 score 0 행으로 details 에 남는다.
 */
@Component
public class PaymentImpossibleTravelEvaluator {

    public static final String RULE_CODE = "PAY_IMPOSSIBLE_TRAVEL";

    private static final Logger log = LoggerFactory.getLogger(PaymentImpossibleTravelEvaluator.class);

    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final double SECONDS_PER_HOUR = 3600.0;

    private final double impossibleSpeedKmh;

    public PaymentImpossibleTravelEvaluator(
        @Value("${fds.payment.impossible-speed-kmh}") double impossibleSpeedKmh
    ) {
        this.impossibleSpeedKmh = impossibleSpeedKmh;
    }

    /**
     * 스킵 3조건: ① 24h 내 COMPLETED 결제 없음 ② 기준 거래 좌표 없음 ③ 경과 시간 ≤ 0.
     * 현재 가맹점 좌표 부재도 방어적으로 스킵한다 — 실행 경로가 좌표 없는 가맹점 결제를
     * 거부하지만(A1) 이 평가기 단독으로도 안전해야 한다.
     */
    public boolean evaluate(PaymentRuleContext context) {
        LastCompletedPayment last = context.getLastCompletedPayment();
        if (last == null) {
            return false;
        }
        if (last.getLatitude() == null || last.getLongitude() == null) {
            return false;
        }
        if (context.getMerchantLatitude() == null || context.getMerchantLongitude() == null) {
            return false;
        }

        long elapsedSeconds =
            Duration.between(last.getCompletedAt(), context.getRequestedAt()).getSeconds();
        if (elapsedSeconds <= 0) {
            // 기준점이 미래 시각이라는 뜻 — 시계 역전·데이터 오염 신호라 조용히 넘기면 안 된다.
            log.warn("이동 속도 평가 스킵: 경과 시간이 0 이하. lastCompletedAt={}, requestedAt={}",
                last.getCompletedAt(), context.getRequestedAt());
            return false;
        }

        double distanceKm = haversineKm(
            last.getLatitude(), last.getLongitude(),
            context.getMerchantLatitude(), context.getMerchantLongitude());
        double speedKmh = distanceKm / (elapsedSeconds / SECONDS_PER_HOUR);
        return speedKmh > impossibleSpeedKmh;
    }

    private static double haversineKm(
        BigDecimal fromLat, BigDecimal fromLng, BigDecimal toLat, BigDecimal toLng
    ) {
        double lat1 = Math.toRadians(fromLat.doubleValue());
        double lat2 = Math.toRadians(toLat.doubleValue());
        double deltaLat = lat2 - lat1;
        double deltaLng = Math.toRadians(toLng.doubleValue() - fromLng.doubleValue());

        double a = Math.pow(Math.sin(deltaLat / 2), 2)
            + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(deltaLng / 2), 2);
        return 2 * EARTH_RADIUS_KM * Math.asin(Math.sqrt(a));
    }
}
