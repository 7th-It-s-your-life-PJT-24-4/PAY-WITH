package com.paywith.fds.dto;

import com.paywith.fds.domain.DecidedBy;
import com.paywith.fds.domain.RiskLevel;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * FDS 판정 결과. 송금 API는 {@link #riskLevel} 만 보고 분기하면 되고 룰 내부 기준은 몰라도 된다.
 *
 * <p>단축평가로 확정된 건은 {@link #totalScore} 가 0이고 {@link #decidedBy} 가
 * BLACKLIST/WHITELIST 다. 이때 점수는 판정 근거가 아니므로 등급 해석에 쓰면 안 된다.
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

    /** 보호자 승인 전까지 송금을 막아야 하는지 여부. */
    public boolean isHeld() {
        return riskLevel == RiskLevel.DANGER;
    }

    /** 보호자에게 알림을 보내야 하는지 여부(주의 이상). */
    public boolean requiresGuardNotification() {
        return riskLevel != RiskLevel.SAFE;
    }
}
