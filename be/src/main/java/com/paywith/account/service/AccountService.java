package com.paywith.account.service;

import com.paywith.account.domain.Account;
import com.paywith.account.dto.AccountCreateRequest;
import com.paywith.account.dto.AccountResponse;

import java.util.List;

public interface AccountService {
    //계좌 등록
    AccountResponse registerAccount(Long userId, AccountCreateRequest request);

    //계좌 조회
    List<AccountResponse> getAccounts(Long userId);

    //이 계좌의 소유자가 계정주가 맞는지 확인
    boolean verifyOwnership(Long userId, Long accountId);

    //계좌 조회 1건 -> ChargeResponse에서 사용
    AccountResponse getAccountDetail(Long accountId);


}
