package com.paywith.fds.dto;

import com.paywith.fds.domain.DecidedBy;
import com.paywith.fds.domain.RiskLevel;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * FDS 판정 결과.
 *
 * <p>단축평가로 확정된 건은 totalScore 가 0이다. 이때 점수는 판정 근거가 아니므로
 * 등급 해석에 쓰면 안 된다.
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
