package com.paywith.fds.support;

import com.paywith.fds.domain.DecidedBy;
import com.paywith.fds.domain.RiskLevel;
import com.paywith.fds.dto.FdsDecision;
import com.paywith.fds.dto.TriggeredRule;
import java.util.Collections;
import java.util.List;

/**
 * 판정 경로별 {@link FdsDecision} 표본. 룰 카탈로그를 거치지 않고 결과만 필요할 때 쓴다.
 *
 * <p>등급만으로는 즉시 차단(블랙리스트)과 승인 대기를 구분할 수 없어, 호출부 테스트는
 * 반드시 판정 객체로 분기를 세워야 한다.
 */
public final class FdsDecisions {

    private FdsDecisions() {
    }

    public static FdsDecision safe() {
        return byRule(RiskLevel.SAFE, 0);
    }

    public static FdsDecision caution() {
        return byRule(RiskLevel.CAUTION, FdsTestWiring.CAUTION_THRESHOLD);
    }

    /** 점수 합산으로 DANGER 가 된 건 — 보호자 승인 대기로 간다. */
    public static FdsDecision held() {
        return byRule(RiskLevel.DANGER, FdsTestWiring.DANGER_THRESHOLD);
    }

    /** 블랙리스트 확정 건 — 승인 없이 즉시 차단된다. */
    public static FdsDecision blocked() {
        return new FdsDecision(
            RiskLevel.DANGER,
            DecidedBy.BLACKLIST,
            RiskRules.PREFILTER_SCORE,
            FdsTestWiring.CAUTION_THRESHOLD,
            FdsTestWiring.DANGER_THRESHOLD,
            Collections.singletonList(new TriggeredRule(1L, RiskRules.PREFILTER_SCORE))
        );
    }

    private static FdsDecision byRule(RiskLevel riskLevel, int totalScore) {
        return new FdsDecision(
            riskLevel,
            DecidedBy.RULE,
            totalScore,
            FdsTestWiring.CAUTION_THRESHOLD,
            FdsTestWiring.DANGER_THRESHOLD,
            List.of()
        );
    }
}
