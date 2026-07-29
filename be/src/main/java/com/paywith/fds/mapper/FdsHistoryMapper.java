package com.paywith.fds.mapper;

import com.paywith.fds.domain.RecipientRiskInfo;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * FDS 룰 평가용 read-only 이력 조회. transactions/recipients 테이블에 대한 쓰기는 하지 않는다.
 */
@Mapper
public interface FdsHistoryMapper {

    /**
     * 속도 룰용 집계. 평가 대상 거래({@code excludedTransactionId})는 모두 제외한다.
     *
     * <p>거래 행을 FDS 평가보다 먼저 생성하므로, 제외하지 않으면 방금 만든 행이 자기 이력에
     * 포함되어 임계값이 1씩 느슨해진다. status 로 거르면 동시에 진행 중인 다른 송금까지
     * 빠지므로 반드시 거래 식별자로 제외해야 한다.
     */
    int countRecentTransfers(
        @Param("walletId") Long walletId,
        @Param("since") LocalDateTime since,
        @Param("excludedTransactionId") Long excludedTransactionId);

    int countRecentDistinctRecipients(
        @Param("walletId") Long walletId,
        @Param("since") LocalDateTime since,
        @Param("excludedTransactionId") Long excludedTransactionId);

    /**
     * 블랙리스트: 해당 수취인 앞으로 보호자가 거절한 송금 이력이 있는지.
     *
     * <p>approval_requests 를 읽지만 승인 도메인의 쓰기는 하지 않는다. FDS 판정용 read-only
     * 조회라 여기에 둔다. 승인요청 생성·갱신은 ApprovalRequestMapper 가 담당한다.
     */
    boolean existsRejectedApproval(@Param("recipientId") Long recipientId);

    /**
     * 이 지갑에 아직 유효한 보호자 승인 대기가 남아 있는지.
     *
     * <p>transactions.status='HELD' 가 아니라 approval_requests 의 PENDING + 미만료로 판정한다.
     * 만료 처리 배치가 없어도 expired_at 이 지나면 자동으로 해제되어 영구 가점이 되지 않는다.
     */
    boolean existsPendingApproval(@Param("walletId") Long walletId);

    RecipientRiskInfo findRecipientRiskInfo(@Param("recipientId") Long recipientId);
}
