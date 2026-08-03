package com.paywith.account.service;

import com.paywith.account.domain.Account;
import com.paywith.account.dto.AccountCreateRequest;
import com.paywith.account.dto.AccountResponse;
import com.paywith.account.mapper.AccountMapper;
import com.paywith.exception.BusinessException;
import com.paywith.external.openbanking.OpenBankingClient;
import com.paywith.user.domain.User;
import com.paywith.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import com.paywith.external.openbanking.dto.RealNameInquiryResponse;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService{

    private final AccountMapper accountMapper;
    private final OpenBankingClient openBankingClient;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public AccountResponse registerAccount(Long userId, AccountCreateRequest request) {
        if (accountMapper.existsByUserIdAndAccount(userId, request.getBankCode(), request.getAccountNo())) {
            throw new BusinessException(HttpStatus.CONFLICT, "ACCOUNT_003","이미 등록된 계좌입니다.");
        }

        User user = userMapper.findById(userId);

        RealNameInquiryResponse inquiryResponse = openBankingClient.inquireRealName(
                request.getBankCode(),
                request.getAccountNo(),
                user.getBirthDate().toString()
        );

        if (!inquiryResponse.isSuccess()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "ACCOUNT_002", "계좌 실명조회에 실패했습니다. " + inquiryResponse.getRspMessage());
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

    @Override
    public boolean verifyOwnership(Long userId, Long accountId) {
        return accountMapper.existsByUserIdAndAccountId(userId, accountId);
    }

    @Override
    public AccountResponse getAccountDetail(Long accountId) {
        return accountMapper.findAccountById(accountId);
    }
}
