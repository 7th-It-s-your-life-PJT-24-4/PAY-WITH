package com.paywith.fds.mapper;

import com.paywith.fds.domain.RecipientRiskInfo;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** FDS 판정용 read-only 조회. 다른 도메인 테이블을 읽지만 쓰지는 않는다. */
@Mapper
public interface FdsHistoryMapper {

    /**
     * 거래 행을 FDS 평가보다 먼저 만들기 때문에 평가 대상 거래를 빼야 한다. 빼지 않으면 방금 만든
     * 행이 자기 이력에 잡혀 임계값이 1씩 느슨해진다. status 로 거르면 동시 진행 중인 다른 송금까지
     * 빠지므로 거래 식별자로 제외한다.
     */
    int countRecentTransfers(
        @Param("walletId") Long walletId,
        @Param("since") LocalDateTime since,
        @Param("excludedTransactionId") Long excludedTransactionId);

    int countRecentDistinctRecipients(
        @Param("walletId") Long walletId,
        @Param("since") LocalDateTime since,
        @Param("excludedTransactionId") Long excludedTransactionId);

    boolean existsRejectedApproval(@Param("recipientId") Long recipientId);

    /**
     * 유효한 승인 대기가 남아 있는지. transactions.status='HELD' 대신 expired_at 을 보므로
     * 만료 배치가 없어도 시간이 지나면 자동으로 풀린다.
     */
    boolean existsPendingApproval(@Param("walletId") Long walletId);

    RecipientRiskInfo findRecipientRiskInfo(@Param("recipientId") Long recipientId);
}
