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

    int countRecentTransfers(@Param("walletId") Long walletId, @Param("since") LocalDateTime since);

    int countRecentDistinctRecipients(@Param("walletId") Long walletId, @Param("since") LocalDateTime since);

    RecipientRiskInfo findRecipientRiskInfo(@Param("recipientId") Long recipientId);
}
