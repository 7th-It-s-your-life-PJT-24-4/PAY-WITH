package com.paywith.account.service;

import com.paywith.account.domain.Account;
import com.paywith.account.dto.AccountCreateRequest;
import com.paywith.account.dto.AccountResponse;
import com.paywith.account.mapper.AccountMapper;
import com.paywith.exception.BusinessException;
import com.paywith.external.openbanking.OpenBankingClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import com.paywith.external.openbanking.dto.RealNameInquiryResponse;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService{

    private final AccountMapper accountMapper;
    private final OpenBankingClient openBankingClient;

    @Override
    public AccountResponse registerAccount(Long userId, AccountCreateRequest request) {
        if (accountMapper.existsByUserIdAndAccount(userId, request.getBankCode(), request.getAccountNo())) {
            throw new BusinessException(HttpStatus.CONFLICT, "이미 등록된 계좌입니다.");
        }

        RealNameInquiryResponse inquiryResponse = openBankingClient.inquireRealName(
                request.getBankCode(),
                request.getAccountNo(),
                request.getBirthDate()
        );

        if (!inquiryResponse.isSuccess()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "계좌 실명조회에 실패했습니다: " + inquiryResponse.getRspMessage());
        }

        Account account = Account.builder()
                .userId(userId)
                .bankCode(request.getBankCode())
                .accountNo(request.getAccountNo())
                .holderName(inquiryResponse.getAccountHolderName())
                .isVerified(true)
                .build();

        accountMapper.insertAccount(account);

        AccountResponse response = AccountResponse.builder()
                .accountId(account.getAccountId())
                .bankCode(account.getBankCode())
                .bankName(inquiryResponse.getBankName())
                .accountNo(account.getAccountNo())
                .build();

        return response;
    }

    @Override
    public List<AccountResponse> getAccounts(Long userId) {
        return accountMapper.findAccountsByUserId(userId);
    }
}
