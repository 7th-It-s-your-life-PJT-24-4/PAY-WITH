package com.paywith.wallet.mapper;

import com.paywith.wallet.domain.Wallet;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
@Mapper
public interface WalletMapper {
    //지갑 조회, 행 잠금
    Wallet findWalletByUserIdForUpdate(Long userId);

    //지갑 단순 조회
    Wallet findWalletByUserId(Long userId);

    // 잔액 업데이트
    void increaseBalance(@Param("walletId") Long walletId, @Param("amount") Long amount);

    // 잔액 출금
    int decreaseBalanceIfSufficient(@Param("walletId") Long walletId, @Param("amount") Long amount);
}
