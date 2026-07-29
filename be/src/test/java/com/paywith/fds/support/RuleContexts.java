package com.paywith.fds.support;

import com.paywith.fds.service.rule.RuleContext;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 테스트용 RuleContext 생성 도우미. 관심 없는 필드는 "정상 거래" 기본값으로 채운다.
 */
public final class RuleContexts {

    public static final List<String> MEMO_KEYWORDS =
        List.of("검찰", "경찰", "수사", "대포통장", "납치", "대출", "벌금");

    private RuleContexts() {
    }

    /** 단골 수취인에게 주간에 소액을 보내는, 어떤 룰도 발동하지 않는 기준 컨텍스트. */
    public static RuleContext.RuleContextBuilder normal() {
        return RuleContext.builder()
            .amount(new BigDecimal("30000"))
            .memo(null)
            .requestedAt(at(14))
            .recipientSendCount(5)
            .recipientRegisteredSafe(false)
            .recentTransferCount(0)
            .recentDistinctRecipientCount(0)
            .recipientRejectedBefore(false)
            .pendingApprovalExists(false)
            .memoKeywords(MEMO_KEYWORDS);
    }

    public static LocalDateTime at(int hour) {
        return LocalDateTime.of(2026, 7, 24, hour, 0);
    }

    public static BigDecimal won(String amount) {
        return new BigDecimal(amount);
    }
}
