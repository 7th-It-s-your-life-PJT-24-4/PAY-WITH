package com.paywith.fds.dto;

import com.paywith.fds.domain.DecidedBy;
import com.paywith.fds.domain.RiskLevel;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * FDS 판정 결과.
 *
 * <p>단축평가로 확정된 건은 totalScore 가 카탈로그 배점(만점)이다. 등급은 점수와 무관하게
 * DANGER 로 확정된 것이므로, 판정 경로 구분은 점수가 아니라 decidedBy 로 한다.
 */
@Getter
@RequiredArgsConstructor
public class FdsDecision {

    private final RiskLevel riskLevel;
    private final DecidedBy decidedBy;
    private final int totalScore;
    private final int cautionThreshold;
    private final int dangerThreshold;
    private final List<TriggeredRule> triggeredRules;

    public boolean isHeld() {
        return riskLevel == RiskLevel.DANGER;
    }

    public boolean requiresGuardNotification() {
        return riskLevel != RiskLevel.SAFE;
    }
}
