package com.paywith.wallet.mapper;

import com.paywith.wallet.domain.Wallet;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
@Mapper
public interface WalletMapper {
    //지갑 조회, 행 잠금
    Wallet findWalletByUserIdForUpdate(Long userId);

    // 잔액 업데이트
    void increaseBalance(@Param("walletId") Long walletId, @Param("amount") Long amount);
}
