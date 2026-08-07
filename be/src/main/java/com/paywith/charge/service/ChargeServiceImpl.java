package com.paywith.charge.service;

import com.paywith.account.dto.AccountResponse;
import com.paywith.account.service.AccountService;
import com.paywith.charge.dto.*;
import com.paywith.charge.mapper.UserNameMapper;
import com.paywith.exception.BusinessException;
import com.paywith.exception.ChargeIrrecoverableException;
import com.paywith.external.openbanking.OpenBankingClient;
import com.paywith.guard.service.GuardService;
import com.paywith.transaction.domain.Transaction;
import com.paywith.transaction.domain.TransactionStatus;
import com.paywith.transaction.domain.TransactionType;
import com.paywith.transaction.mapper.TransactionMapper;
import com.paywith.user.domain.User;
import com.paywith.user.mapper.UserMapper;
import com.paywith.wallet.domain.Wallet;
import com.paywith.wallet.mapper.WalletMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;


@Service
@RequiredArgsConstructor
public class ChargeServiceImpl implements ChargeService{

    private static final Logger log = LoggerFactory.getLogger(ChargeServiceImpl.class);

    private static final int COMPLETE_MAX_ATTEMPTS = 3;
    private static final long COMPLETE_RETRY_DELAY_MS = 200;

    private final AccountService accountService;
    private final WalletMapper walletMapper;
    private final TransactionMapper transactionMapper;
    private final OpenBankingClient openBankingClient;
    private final GuardService guardService;
    private final UserNameMapper userNameMapper;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final TransactionTemplate transactionTemplate;


    // 시니어 충전용 메서드
    @Override
    public ChargeResponse charge(Long userId, ChargeRequest request) {
        return doCharge(userId, userId, request, null);
        }


    // 보호자 충전용 메서드
    @Override
    public ChargeResponse chargeByGuard(Long guardId, Long wardId, ChargeRequest request) {
            boolean isGuard = guardService.verifyGuardOfWard(guardId,wardId);
            if (!isGuard){
                throw new BusinessException(HttpStatus.NOT_FOUND, "LINK_001", "연동된 피보호자를 찾을 수 없습니다.");

            }

            // 보호자 본인 계좌 -> 피보호자 지갑으로 나가는 돈이기 때문에 PIN으로 한 번 더 확인
            User guard = userMapper.findById(guardId);
            if(request.getPin() == null || !passwordEncoder.matches(request.getPin(), guard.getPin())){
                throw new BusinessException(HttpStatus.BAD_REQUEST, "CHARGE_003", "충전 비밀번호가 올바르지 않습니다.");
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

        // 3. [TX1] 지갑 잠금 + 거래행 REQUESTED로 insert (충전 전에 거래 행 db 등록)
        PreparedCharge prepared = transactionTemplate.execute(status -> {
            Wallet wallet = walletMapper.findWalletByUserIdForUpdate(walletOwnerId);
            if (wallet == null){
                throw new BusinessException(HttpStatus.NOT_FOUND, "지갑을 찾을 수 없습니다.");
            }

            LocalDateTime createdAt = LocalDateTime.now();

            Transaction transaction = Transaction.builder()
                    .walletId(wallet.getWalletId())
                    .accountId(request.getAccountId())
                    .type(TransactionType.CHARGE)
                    .amount(request.getAmount())
                    .initiatedBy(initiatedBy)
                    .status(TransactionStatus.REQUESTED)
                    .createdAt(createdAt)
                    .build();

            transactionMapper.insertTransaction(transaction);

            return new PreparedCharge(wallet, transaction.getTransactionId(), createdAt);
        });


        // 4. 외부 API -> 출금이체
        // 여기서부터 "돌아올 수 없는 지점" => 실패하면 실패로 확정해야함
        try{
            openBankingClient.withdraw(account.getBankCode(), account.getAccountNo(), request.getAmount());
        } catch (RuntimeException e) {
            markFailed(prepared.transactionId(), "출금 처리 중 오류: "+ e.getMessage());
            throw new ChargeIrrecoverableException(
                    "충전 처리 중 오류가 발생했습니다. 계좌 상태를 확인 후 고객센터로 문의해주세요. transactionId=" + prepared.transactionId()
            );
        }

        // 5.[TX2] 잔액 증가 + 완료 확정. 출금은 이미 성공했으므로 짧게만 재시도 함
        Long balanceAfter = tryCompleteCharge(prepared, walletOwnerId, request.getAmount());
        if (balanceAfter == null){
            markFailed(prepared.transactionId(), "충전 완료 처리 반복 실패(출금은 이미 성공)");
            throw new ChargeIrrecoverableException(
                    "충전 처리 결과 확정에 실패했습니다. 잔액을 확인 후 고객센터로 문의해주세요. transactionId=" + prepared.transactionId());

        }


        // 6. 응답
        return ChargeResponse.builder()
                .transactionId(prepared.transactionId())
                .chargeAmount(request.getAmount())
                .balanceAfter(balanceAfter)
                .bankName(account.getBankName())
                .accountNo(account.getAccountNo())
                .createdAt(prepared.createdAt())
                .build();

    }

    // 잔액 증가 + 완료 처리 (최대 3회 재시도)
        private Long tryCompleteCharge(PreparedCharge prepared, Long walletOwnerId, Long amount){
        LocalDateTime completedAt = LocalDateTime.now();
        for (int attempt = 1; attempt <= COMPLETE_MAX_ATTEMPTS; attempt++){
            try{
                return transactionTemplate.execute(status -> {
                    walletMapper.increaseBalance(prepared.wallet().getWalletId(), amount);
                    // 잔액 재조회 (TX1에서 읽은 값이 변경 됐을 수 있음)
                    Wallet updatedWallet = walletMapper.findWalletByUserId(walletOwnerId);

                    int updatedRows = transactionMapper.completeTransaction(
                            prepared.transactionId(), TransactionStatus.COMPLETED, updatedWallet.getBalance(), completedAt);
                    if (updatedRows != 1){
                        throw new IllegalStateException("거래 완료 처리 실패(영향 행 0)");
                    }
                    return updatedWallet.getBalance();
                });
            } catch (RuntimeException e){
                log.warn("충전 완료 처리 중 예외, 재시도 {}/{}. transaction={}",
                        attempt, COMPLETE_MAX_ATTEMPTS, prepared.transactionId(), e);
            }
            if (attempt < COMPLETE_MAX_ATTEMPTS){
                sleep(COMPLETE_RETRY_DELAY_MS);
            }
        }
        return null;
    }

    private void sleep(long millis){
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void markFailed(Long transactionId, String reason) {
        log.error("충전 확정 실패, FAILED로 표시. transactionId={}, reason={}", transactionId, reason);
        try {
            transactionMapper.markFailedIfRequested(transactionId);
        } catch (RuntimeException e){
            log.error("FAILED 상태 기록 실패. transactionId={}", transactionId, e);
        }
    }

    private record PreparedCharge(Wallet wallet, long transactionId, LocalDateTime createdAt){}
}
