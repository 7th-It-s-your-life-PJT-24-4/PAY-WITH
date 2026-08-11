package com.paywith.bank.service;

import com.paywith.bank.dto.BankFilterResponse;

public interface BankFilterService {

    /** 계좌번호 형식(자릿수 범위 및 prefix 조건)과 일치하는 활성 은행 후보 목록을 반환한다. */
    BankFilterResponse filterBanks(Long wardId, String accountNo);
}