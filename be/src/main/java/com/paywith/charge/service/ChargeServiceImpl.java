package com.paywith.charge.service;

import com.paywith.account.dto.AccountResponse;
import com.paywith.account.service.AccountService;
import com.paywith.charge.dto.*;
import com.paywith.charge.mapper.UserNameMapper;
import com.paywith.exception.BusinessException;
import com.paywith.external.openbanking.OpenBankingClient;
import com.paywith.guard.service.GuardService;
import com.paywith.transaction.domain.Transaction;
import com.paywith.transaction.mapper.TransactionMapper;
import com.paywith.wallet.domain.Wallet;
import com.paywith.wallet.mapper.WalletMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChargeServiceImpl implements ChargeService{

    private final AccountService accountService;
    private final WalletMapper walletMapper;
    private final TransactionMapper transactionMapper;
    private final OpenBankingClient openBankingClient;
    private final GuardService guardService;
    private final UserNameMapper userNameMapper;


    // 시니어 충전용 메서드
    @Override
    @Transactional
    public ChargeResponse charge(Long userId, ChargeRequest request) {
        return doCharge(userId, userId, request, null);
        }


    // 보호자 충전용 메서드
    @Override
    @Transactional
    public ChargeResponse chargeByGuard(Long guardId, Long wardId, ChargeRequest request) {
            boolean isGuard = guardService.verifyGuardOfWard(guardId,wardId);
            if (!isGuard){
                throw new BusinessException(HttpStatus.NOT_FOUND, "LINK_001", "연동된 피보호자를 찾을 수 없습니다.");

            }
            ChargeResponse response = doCharge(guardId, wardId, request, guardId);

            response.setWardId(wardId);
            response.setWardName(userNameMapper.findUserName(wardId));

            return response;
    }

    // 보호자 충전 조회 메서드
    @Override
    public ChargeHistoryListResponse getChargeHistories(Long guardId) {
        List<ChargeHistoryItem> items = transactionMapper.findChargeHistoriesByGuardId(guardId);
        return ChargeHistoryListResponse.builder()
                .charges(items)
                .build();
    }

    // 보호자 충전 상세 조회 메서드
    @Override
    public ChargeDetailResponse getChargeDetail(Long guardId, Long transactionId) {
        ChargeDetailResponse response = transactionMapper.findChargeDetailByGuardId(transactionId,guardId);
        if(response == null){
            throw new BusinessException(HttpStatus.NOT_FOUND, "CHARGE_002", "충전 내역을 찾을 수 없습니다.");
        }
        return response;
    }

    private ChargeResponse doCharge(Long accountOwnerId, Long walletOwnerId,
                                    ChargeRequest request, Long initiatedBy){
        // 1. 계좌 소유 확인
        boolean isOwner = accountService.verifyOwnership(accountOwnerId, request.getAccountId());
        if (!isOwner) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "ACCOUNT_004", "등록된 계좌를 찾을 수 없습니다.");
        }
        // 2. 계좌 정보 조회
        AccountResponse account = accountService.getAccountDetail(request.getAccountId());

        // 3. Mock 출금이체
        openBankingClient.withdraw(account.getBankCode(), account.getAccountNo(), request.getAmount());

        // 4. 지갑 조회+잠금
        Wallet wallet = walletMapper.findWalletByUserIdForUpdate(walletOwnerId);
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
                .amount(request.getAmount())
                .balanceAfter(balanceAfter)
                .initiatedBy(initiatedBy)
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
                .accountNo(account.getAccountNo())
                .createdAt(transaction.getCreatedAt())
                .build();

    }
}
