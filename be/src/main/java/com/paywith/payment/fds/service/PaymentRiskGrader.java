package com.paywith.payment.fds.service;

import com.paywith.fds.domain.RiskLevel;
import com.paywith.fds.service.RiskGrader;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 결제 전용 등급 환산기. 점수 척도(0~{@link RiskGrader#MAX_SCORE})는 송금과 같고 임계값만 결제 값을 쓴다.
 *
 * <p>송금과 임계값을 공유하던 것을 분리한 이유는 화면 표시 때문이다. 차단된 결제가 주의 25 / 위험 50
 * 기준에서는 100점 만점에 50점으로 보여, 사용자에게 "절반만 위험한 거래"로 읽혔다(QA 지적). 결제 임계값을
 * 50 / 100 으로 올리고 PAY_* 배점을 일괄 2배로 맞춰, 차단이 곧 만점이 되도록 했다.
 *
 * <p>공용 {@link RiskGrader} 를 그대로 올리지 않은 것은 송금 배점이 그대로이기 때문이다. 송금 최고 배점은
 * HIGH_AMOUNT_L3 35 라, 위험 임계값이 100 이 되면 단축평가(BL_* 100점) 말고는 DANGER 가 사실상 나올 수
 * 없다. 척도의 소유권은 {@link RiskGrader} 에 두고 클램프도 그쪽에 위임한다 — 상한이 두 곳에서 갈리면
 * 저장값과 화면이 어긋난다.
 */
@Component
public class PaymentRiskGrader {

    private final RiskGrader riskGrader;

    @Getter
    private final int cautionThreshold;

    @Getter
    private final int dangerThreshold;

    public PaymentRiskGrader(
        RiskGrader riskGrader,
        @Value("${fds.payment.threshold.caution}") int cautionThreshold,
        @Value("${fds.payment.threshold.danger}") int dangerThreshold
    ) {
        if (cautionThreshold > dangerThreshold) {
            throw new IllegalArgumentException(
                "fds.payment.threshold.caution 은 fds.payment.threshold.danger 보다 클 수 없습니다: "
                    + cautionThreshold + " > " + dangerThreshold);
        }
        // 상한을 넘는 임계값은 클램프 때문에 영원히 도달할 수 없어 등급이 죽는다.
        if (dangerThreshold > RiskGrader.MAX_SCORE) {
            throw new IllegalArgumentException(
                "fds.payment.threshold.danger 는 총점 상한 " + RiskGrader.MAX_SCORE
                    + " 을 넘을 수 없습니다: " + dangerThreshold);
        }
        this.riskGrader = riskGrader;
        this.cautionThreshold = cautionThreshold;
        this.dangerThreshold = dangerThreshold;
    }

    /**
     * 총점을 0~{@link RiskGrader#MAX_SCORE} 로 맞춘다. 척도가 하나여야 하므로 공용 환산기에 위임한다.
     *
     * <p>배점을 2배로 올린 뒤로는 룰이 두엇만 겹쳐도 상한에 닿는다(예: 위험업종 50 + 고액 L3 70 = 120 → 100).
     * 잘리는 구간은 전부 위험 임계값 이상이라 등급은 바뀌지 않는다.
     */
    public int normalizeScore(int rawScore) {
        return riskGrader.normalizeScore(rawScore);
    }

    public RiskLevel grade(int normalizedScore) {
        if (normalizedScore >= dangerThreshold) {
            return RiskLevel.DANGER;
        }
        if (normalizedScore >= cautionThreshold) {
            return RiskLevel.CAUTION;
        }
        return RiskLevel.SAFE;
    }
}
