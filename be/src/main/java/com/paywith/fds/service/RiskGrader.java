package com.paywith.fds.service;

import com.paywith.fds.domain.RiskLevel;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** 룰 합산 점수를 등급으로 환산한다. 점수 척도(0~100)의 단일 소유자다. */
@Component
@Getter
public class RiskGrader {

    /** 저장·노출되는 총점의 상한. 배점 척도가 0~100 이라는 약속이 여기 한 곳에만 있다. */
    public static final int MAX_SCORE = 100;

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
        // 상한을 넘는 임계값은 클램프 때문에 영원히 도달할 수 없어 등급이 죽는다.
        if (dangerThreshold > MAX_SCORE) {
            throw new IllegalArgumentException(
                "fds.threshold.danger 는 총점 상한 " + MAX_SCORE + " 을 넘을 수 없습니다: "
                    + dangerThreshold);
        }
        this.cautionThreshold = cautionThreshold;
        this.dangerThreshold = dangerThreshold;
    }

    /**
     * 총점을 0~{@value #MAX_SCORE} 로 맞춘다.
     *
     * <p>감점 룰 때문에 합산이 음수가 될 수 있고, 반대로 룰이 여럿 겹치면 상한을 넘는다
     * (현재 배점 기준 최대 151). 어느 쪽도 경계값과 견줄 수 없는 값이라 양끝을 자른다.
     * 상한을 넘는 구간은 이미 전부 DANGER 라 자르더라도 등급은 바뀌지 않는다.
     */
    public int normalizeScore(int rawScore) {
        return Math.min(MAX_SCORE, Math.max(0, rawScore));
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
