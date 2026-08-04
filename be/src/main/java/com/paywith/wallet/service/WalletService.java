package com.paywith.wallet.service;

import com.paywith.wallet.dto.WalletBalanceResponse;

public interface WalletService {

    /**
     * 로그인한 피보호자 본인의 지갑 잔액을 조회한다.
     *
     * @param userId 인증 주체. 경로나 파라미터로 받지 않으므로 남의 지갑은 조회할 수 없다.
     */
    WalletBalanceResponse findMyBalance(Long userId);
}
