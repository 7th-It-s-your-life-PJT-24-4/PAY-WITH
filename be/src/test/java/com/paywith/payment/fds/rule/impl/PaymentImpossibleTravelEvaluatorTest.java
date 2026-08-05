package com.paywith.payment.fds.rule.impl;

import static com.paywith.payment.fds.support.PaymentRuleContexts.BUSAN_LAT;
import static com.paywith.payment.fds.support.PaymentRuleContexts.BUSAN_LNG;
import static com.paywith.payment.fds.support.PaymentRuleContexts.SEOUL_LAT;
import static com.paywith.payment.fds.support.PaymentRuleContexts.SEOUL_LNG;
import static com.paywith.payment.fds.support.PaymentRuleContexts.at;
import static com.paywith.payment.fds.support.PaymentRuleContexts.lastPayment;
import static com.paywith.payment.fds.support.PaymentRuleContexts.normal;
import static org.assertj.core.api.Assertions.assertThat;

import com.paywith.payment.fds.rule.PaymentRuleContext;
import com.paywith.payment.fds.support.PaymentFdsTestWiring;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 이동 속도 단축평가. 기준 컨텍스트(normal)는 서울 가맹점 결제이고, 기준점을 부산(약 325km)으로
 * 두면 경과 시간에 따라 속도가 정해진다 — 5분이면 수천 km/h, 3시간이면 약 110km/h.
 */
@DisplayName("결제 FDS 이동 속도 단축평가")
class PaymentImpossibleTravelEvaluatorTest {

    private final PaymentImpossibleTravelEvaluator evaluator = PaymentFdsTestWiring.travelEvaluator();

    @Test
    void doesNotTriggerWithoutRecentCompletedPayment() {
        assertThat(evaluator.evaluate(normal().lastCompletedPayment(null).build())).isFalse();
    }

    /** 기준 거래 좌표가 없으면 스킵 — 더 오래된 좌표 보유 행으로 대체하지 않는다는 전제의 짝. */
    @Test
    void doesNotTriggerWhenLastPaymentHasNoCoordinates() {
        PaymentRuleContext context = normal()
            .lastCompletedPayment(lastPayment(null, null, at(13)))
            .build();
        assertThat(evaluator.evaluate(context)).isFalse();
    }

    /** 실행 경로가 좌표 없는 가맹점을 거부하지만(A1) 평가기 단독으로도 안전해야 한다. */
    @Test
    void doesNotTriggerWhenCurrentMerchantHasNoCoordinates() {
        PaymentRuleContext context = normal()
            .merchantLatitude(null).merchantLongitude(null)
            .lastCompletedPayment(lastPayment(BUSAN_LAT, BUSAN_LNG, at(13)))
            .build();
        assertThat(evaluator.evaluate(context)).isFalse();
    }

    /** 기준점이 현재와 같거나 미래 시각이면 속도 계산 없이 스킵한다(시계 역전·데이터 오염 방어). */
    @Test
    void doesNotTriggerWhenElapsedTimeIsZeroOrNegative() {
        PaymentRuleContext sameInstant = normal()
            .lastCompletedPayment(lastPayment(BUSAN_LAT, BUSAN_LNG, at(14)))
            .build();
        PaymentRuleContext future = normal()
            .lastCompletedPayment(lastPayment(BUSAN_LAT, BUSAN_LNG, at(15)))
            .build();
        assertThat(evaluator.evaluate(sameInstant)).isFalse();
        assertThat(evaluator.evaluate(future)).isFalse();
    }

    /** 서울 결제 5분 전 부산 결제 — 약 325km 를 5분에 이동한 속도라 확정 위험. */
    @Test
    void triggersForImpossiblyFastTravel() {
        PaymentRuleContext context = normal()
            .lastCompletedPayment(
                lastPayment(BUSAN_LAT, BUSAN_LNG, at(14).minusMinutes(5)))
            .build();
        assertThat(evaluator.evaluate(context)).isTrue();
    }

    /** 3시간 전 부산 결제 — 약 110km/h 는 임계(300) 아래라 정상 이동으로 본다. */
    @Test
    void doesNotTriggerForPlausibleTravel() {
        PaymentRuleContext context = normal()
            .lastCompletedPayment(lastPayment(BUSAN_LAT, BUSAN_LNG, at(11)))
            .build();
        assertThat(evaluator.evaluate(context)).isFalse();
    }

    /** 같은 가맹점 연속 결제 — 거리 0이라 경과 시간이 짧아도 발동하지 않는다. */
    @Test
    void doesNotTriggerForSameLocationRepeat() {
        PaymentRuleContext context = normal()
            .lastCompletedPayment(
                lastPayment(SEOUL_LAT, SEOUL_LNG, at(14).minusSeconds(1)))
            .build();
        assertThat(evaluator.evaluate(context)).isFalse();
    }

    /**
     * 엄격 부등호 검증 — 좌표 기하로 속도를 임계와 정확히 같게 만들 수 없어, 임계 0으로
     * 등식 케이스(속도 0 = 임계 0)를 고정한다. 등식은 미발동, 초과는 발동이어야 한다.
     */
    @Test
    void doesNotTriggerWhenSpeedEqualsThreshold() {
        PaymentImpossibleTravelEvaluator zeroThreshold = new PaymentImpossibleTravelEvaluator(0.0);

        PaymentRuleContext zeroSpeed = normal()
            .lastCompletedPayment(
                lastPayment(SEOUL_LAT, SEOUL_LNG, at(14).minusMinutes(10)))
            .build();
        PaymentRuleContext anySpeed = normal()
            .lastCompletedPayment(lastPayment(BUSAN_LAT, BUSAN_LNG, at(11)))
            .build();

        assertThat(zeroThreshold.evaluate(zeroSpeed)).isFalse();
        assertThat(zeroThreshold.evaluate(anySpeed)).isTrue();
    }
}
