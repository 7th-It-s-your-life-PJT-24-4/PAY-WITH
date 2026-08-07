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

    /**
     * 블랙리스트 확정 건은 보호자 승인을 받지 않고 즉시 차단한다. 승인 요청을 만들지 않으므로
     * 보호자가 나중에 풀어줄 경로도 없다 — 확정적으로 위험한 항목만 블랙리스트에 두는 이유다.
     */
    public boolean isBlocked() {
        return decidedBy == DecidedBy.BLACKLIST;
    }

    /** 점수 합산으로 DANGER 가 된 건만 승인 대기로 보낸다. 블랙리스트는 차단이라 여기서 빠진다. */
    public boolean isHeld() {
        return riskLevel == RiskLevel.DANGER && !isBlocked();
    }

    public boolean requiresGuardNotification() {
        return riskLevel != RiskLevel.SAFE;
    }
}
