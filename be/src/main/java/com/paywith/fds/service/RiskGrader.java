package com.paywith.fds.service;

import com.paywith.fds.domain.RiskLevel;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 룰 합산 점수를 위험 등급으로 환산한다.
 *
 * <p>감점 룰(SAFE_ACCOUNT_CHECK)이 있어 합산 결과가 음수가 될 수 있으므로 0에서 절삭한다.
 * 음수 점수를 그대로 두면 저장된 total_score 가 등급 경계와 견줄 수 없는 값이 된다.
 */
@Component
@Getter
public class RiskGrader {

    private final int cautionThreshold;
    private final int dangerThreshold;

    public RiskGrader(
        @Value("${fds.threshold.caution}") int cautionThreshold,
        @Value("${fds.threshold.danger}") int dangerThreshold
    ) {
        if (cautionThreshold > dangerThreshold) {
            throw new IllegalArgumentException(
                "fds.threshold.caution 은 fds.threshold.danger 보다 클 수 없습니다: "
                    + cautionThreshold + " > " + dangerThreshold);
        }
        this.cautionThreshold = cautionThreshold;
        this.dangerThreshold = dangerThreshold;
    }

    /** 합산 점수의 0 하한을 적용한 값. 저장·판정 모두 이 값을 쓴다. */
    public int normalizeScore(int rawScore) {
        return Math.max(0, rawScore);
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
