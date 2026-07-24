package com.paywith.charge.service;

import com.paywith.account.dto.AccountResponse;
import com.paywith.account.service.AccountService;
import com.paywith.charge.dto.ChargeRequest;
import com.paywith.charge.dto.ChargeResponse;
import com.paywith.exception.BusinessException;
import com.paywith.external.openbanking.OpenBankingClient;
import com.paywith.transaction.domain.Transaction;
import com.paywith.transaction.mapper.TransactionMapper;
import com.paywith.wallet.domain.Wallet;
import com.paywith.wallet.mapper.WalletMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ChargeServiceImpl implements ChargeService{

    private final AccountService accountService;
    private final WalletMapper walletMapper;
    private final TransactionMapper transactionMapper;
    private final OpenBankingClient openBankingClient;


    @Override
    @Transactional
    public ChargeResponse charge(Long userId, ChargeRequest request) {
        // 1. 소유 확인
        boolean isOwner = accountService.verifyOwnership(userId, request.getAccountId());
        if (!isOwner){
            throw new BusinessException(HttpStatus.FORBIDDEN, "본인 소유의 계좌만 사용할 수 있습니다.");
        }
        // 2. 계좌 정보 조회
        AccountResponse account = accountService.getAccountDetail(request.getAccountId());

        // 3. Mock 출금이체
        openBankingClient.withdraw(account.getBankCode(), account.getAccountNo(), request.getAmount());

        // 4. 지갑 조회+잠금
        Wallet wallet = walletMapper.findWalletByUserIdForUpdate(userId);
        if(wallet == null){
            throw new BusinessException(HttpStatus.NOT_FOUND, "지갑을 찾을 수 없습니다.");
        }

        // 5. 잔액 증가
        walletMapper.increaseBalance(wallet.getWalletId(), request.getAmount());

        // 6. 거래 기록
        Long balanceAfter = wallet.getBalance() + request.getAmount();

        Transaction transaction = Transaction.builder()
                .walletId(wallet.getWalletId())
                .accountId(request.getAccountId())
                .type("CHARGE")
                .initiatedBy(null)
                .amount(request.getAmount())
                .balanceAfter(balanceAfter)
                .status("COMPLETED")
                .createdAt(LocalDateTime.now())
                .build();

        transactionMapper.insertTransaction(transaction);

        // 7. 응답
        return ChargeResponse.builder()
                .transactionId(transaction.getTransactionId())
                .chargeAmount(transaction.getAmount())
                .balanceAfter(balanceAfter)
                .bankName(account.getBankName())
                .maskedAccountNo(maskAccountNo(account.getAccountNo()))
                .createdAt(transaction.getCreatedAt())
                .build();

    }

    private String maskAccountNo(String accountNo){
        if (accountNo == null || accountNo.length() < 4){
            return accountNo;
        }
        return accountNo.substring(accountNo.length()-4);
    }

}
