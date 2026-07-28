package com.paywith.fds.service.rule;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class RuleContext {

    private final BigDecimal amount;
    private final String memo;
    private final LocalDateTime requestedAt;
    private final int recipientSendCount;
    private final boolean recipientRegisteredSafe;
    private final int recentTransferCount;
    private final int recentDistinctRecipientCount;
    private final List<String> memoKeywords;
}
