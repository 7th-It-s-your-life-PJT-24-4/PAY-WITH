package com.paywith.fds.service.rule;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * 룰·단축평가가 참조하는 판정 입력 묶음.
 *
 * <p>0단계(선집계)에서 한 번에 만들어지며 이후 단계는 DB를 다시 조회하지 않는다.
 * 룰이 늘어도 조회 횟수가 늘지 않게 하는 것이 목적이다.
 */
@Getter
@Builder
public class RuleContext {

    private final BigDecimal amount;
    private final String memo;
    private final LocalDateTime requestedAt;

    /** 해당 수취인에게 보낸 누적 횟수. 0이면 신규 수취인. */
    private final int recipientSendCount;

    /** 안전계좌로 등록된 수취인(감점 대상). 등록 주체(본인/보호자)는 구분하지 않는다. */
    private final boolean recipientRegisteredSafe;

    /** 반복 송금 시간창 내 송금 횟수. */
    private final int recentTransferCount;

    /** 분할 송금 시간창 내 서로 다른 수취인 수. */
    private final int recentDistinctRecipientCount;

    /** 이 수취인에게 보호자가 송금을 거절한 이력이 있는지. */
    private final boolean recipientRejectedBefore;

    /** 이 지갑에 아직 유효한 보호자 승인 대기가 남아 있는지. */
    private final boolean pendingApprovalExists;

    private final List<String> memoKeywords;
}
