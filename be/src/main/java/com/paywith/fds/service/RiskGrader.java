package com.paywith.fds.service;

import com.paywith.fds.domain.RiskLevel;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** 룰 합산 점수를 등급으로 환산한다. */
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

    /** 감점 룰 때문에 합산이 음수가 될 수 있다. 음수를 그대로 저장하면 경계값과 견줄 수 없다. */
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
