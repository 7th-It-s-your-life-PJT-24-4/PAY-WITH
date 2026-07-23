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
}
