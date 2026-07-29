package com.paywith.fds.service.prefilter.impl;

import com.paywith.fds.domain.DecidedBy;
import com.paywith.fds.domain.RiskLevel;
import com.paywith.fds.service.prefilter.FdsPreFilter;
import com.paywith.fds.service.rule.RuleContext;
import java.util.Optional;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 블랙리스트: 보호자가 이미 거절한 계좌로 재송금을 시도하는 경우.
 *
 * <p>보호자의 명시적 거절은 가장 강한 판단 근거이므로 점수 합산을 거치지 않고 바로 차단한다.
 */
@Component
@Order(10)
public class RejectedRecipientPreFilter implements FdsPreFilter {

    @Override
    public String getRuleCode() {
        return "BL_REJECTED_RECIPIENT";
    }

    @Override
    public DecidedBy getDecidedBy() {
        return DecidedBy.BLACKLIST;
    }

    @Override
    public Optional<RiskLevel> apply(RuleContext context) {
        return context.isRecipientRejectedBefore()
            ? Optional.of(RiskLevel.DANGER)
            : Optional.empty();
    }
}
