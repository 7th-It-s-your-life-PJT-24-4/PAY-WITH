package com.paywith.safeaccount.mapper;

import com.paywith.safeaccount.dto.SafeAccountListItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SafeAccountMapper {

    // 수취인에게 송금 이력이 있는지 확인
    boolean existsCompletedTransfer(@Param("wardId") Long wardId, @Param("recipientId") Long recipientId);

    // 안전 계좌로 등록
    int registerSafeAccount(
            @Param("recipientId") Long recipientId,
            @Param("registeredBy") Long registeredBy,
            @Param("accountAlias") String accountAlias
    );

    // 보호자가 송금 이력 없는 계좌를 안전 계좌로 등록(GUARD 전용)
    Long insertSafeAccountByGuard(
            @Param("wardId") Long wardId,
            @Param("bankCode") String bankCode,
            @Param("accountNo") String accountNo,
            @Param("holderName") String holderName,
            @Param("guardId") Long guardId,
            @Param("accountAlias") String accountAlias
    );

    // 안전 계좌 목록 조회 (Ward/Guard 공용)
    List<SafeAccountListItem> findSafeAccountList(@Param("wardId") Long wardId);
}
