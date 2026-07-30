package com.paywith.transfer.service;

import com.paywith.exception.BusinessException;
import com.paywith.exception.TransferIrrecoverableException;
import com.paywith.external.openbanking.OpenBankingClient;
import com.paywith.fds.domain.RiskLevel;
import com.paywith.transaction.mapper.TransactionMapper;
import com.paywith.transfer.dto.PreparedTransfer;
import com.paywith.transfer.dto.TransferRequest;
import com.paywith.transfer.dto.TransferResponse;
import com.paywith.wallet.domain.Wallet;
import com.paywith.wallet.mapper.WalletMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TransferFinalizationServiceImpl implements TransferFinalizationService {

    private static final Logger log = LoggerFactory.getLogger(TransferFinalizationServiceImpl.class);

    // 11번(거래 완료 처리)은 입금이 이미 성공한 뒤라 짧게만 재시도하고, 그래도 안되면 FAILED로 확정한다
    private static final int COMPLETE_MAX_ATTEMPTS = 3;
    private static final long COMPLETE_RETRY_DELAY_MS = 200;

    private final WalletMapper walletMapper;
    private final TransactionMapper transactionMapper;
    private final OpenBankingClient openBankingClient;
    private final TransactionTemplate transactionTemplate;

    @Override
    public TransferResponse finalize(PreparedTransfer prepared, RiskLevel riskLevel, TransferRequest request) {

        Long transactionId = prepared.getTransaction().getTransactionId();


        // 6. 만약 riskLevel 이 위험이라면 다음 거래를 진행하지 않음
        // -> transaction 테이블의 status 컬럼을 Held로 변경
        // update는 거래 실패 시 0 으로 결과값이 나오기 때문에 그것도 확인 해주는 것이 필요함
        if (riskLevel == RiskLevel.DANGER) {
            int updated = transactionMapper.updateStatus(transactionId, "HELD");
            if (updated != 1) {
                throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "거래 상태를 HELD로 변경하지 못했습니다. transactionId=" + transactionId);
            }
            return TransferResponse.builder()
                    .transactionId(transactionId)
                    .status("HELD")
                    .build();
        }

        // 7~9는 아직 외부에 아무 영향이 없는 구간(내부 DB만) -> 하나의 트랜잭션으로 묶어서
        // 실패하면 통째로 롤백되게 한다. TransactionTemplate을 쓰는 이유는, 같은 클래스 안에서
        // deposit() 호출 전까지만 트랜잭션을 걸고 싶은데 @Transactional은 self-invocation으로
        // 나눌 수 없기 때문이다 (여기서는 finalize()가 한 메서드라 프록시 경계를 못 만든다).
        Long balanceAfter = transactionTemplate.execute(status -> {
            // 7. 아니라면 status는 PROCESSING으로 업데이트
            // update 거래 실패 시 확인
            int processingUpdated = transactionMapper.updateStatus(transactionId, "PROCESSING");
            if (processingUpdated != 1) {
                throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "거래 상태를 PROCESSING으로 변경하지 못했습니다. transactionId=" + transactionId);
            }

            // 8. 잔액 조건부 차감
            int affectedRows = walletMapper.decreaseBalanceIfSufficient(
                    prepared.getWallet().getWalletId(), request.getAmount());
            if (affectedRows == 0) {
                throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "송금 가능한 잔액이 부족합니다.");
            }

            // 9. 최신 거래 반영 (잔액 재조회)
            Wallet updatedWallet = walletMapper.findWalletByUserId(prepared.getWallet().getUserId());
            return updatedWallet.getBalance();
        });

        // 10. 입금 이체
        // 여기서부터 "돌아올 수 없는 지점" (point of no return) -> 실패해도 위 트랜잭션은 이미 커밋된 뒤라
        // 잔액 차감을 되돌릴 수 없고, 외부로 나가는 호출이라 우리 쪽에서 취소도 불가능하다.
        // 그래서 이 아래는 실패 시 일반 RuntimeException이 아니라 TransferIrrecoverableException을 던져서
        // TransferServiceImpl이 "재시도 허용" 대신 "실패로 확정"하도록 신호를 준다.
        try {
            openBankingClient.deposit(request.getBankCode(), request.getAccountNo(), request.getAmount());
        } catch (RuntimeException e) {
            markFailed(transactionId, "입금 처리 중 오류: " + e.getMessage());
            throw new TransferIrrecoverableException(
                    "송금 처리 중 오류가 발생했습니다. 잔액을 확인 후 고객센터로 문의해주세요. transactionId=" + transactionId);
        }

        // 11. 거래 기록 변경 (잔액 업데이트, 완료 시각, 상태)
        // update 거래 실패 시 확인
        // 입금은 이미 성공했으므로 여기서 실패해도 처음부터 재시도하면 안 되고, 짧게만 재시도한다.
        LocalDateTime completedAt = LocalDateTime.now();
        if (!tryCompleteTransaction(transactionId, balanceAfter, completedAt)) {
            // TODO: 오픈뱅킹 입금 결과 조회 A
            //  PI 연동되면, 여기서 실제 입금 성사 여부를 재조회해서
            //  COMPLETED로 되돌릴 수 있는 배치/운영툴을 붙여야 한다. 지금은 FAILED로 고정하고 수동 정산.
            markFailed(transactionId, "거래 완료 처리 반복 실패 (입금은 이미 성공)");
            throw new TransferIrrecoverableException(
                    "송금 처리 결과 확정에 실패했습니다. 잔액을 확인 후 고객센터로 문의해주세요. transactionId=" + transactionId);
        }

        // 12. 응답
        return TransferResponse.builder()
                .transactionId(transactionId)
                .status("COMPLETED")
                .holderName(prepared.getInquiryResponse().getAccountHolderName())
                .bankCode(request.getBankCode())
                .bankName(prepared.getInquiryResponse().getBankName())
                .accountNo(request.getAccountNo())
                .amount(request.getAmount())
                .memo(request.getMemo())
                .completedAt(completedAt)
                .balanceAfter(balanceAfter)
                .build();
    }

    private boolean tryCompleteTransaction(Long transactionId, Long balanceAfter, LocalDateTime completedAt) {
        for (int attempt = 1; attempt <= COMPLETE_MAX_ATTEMPTS; attempt++) {
            try {
                int updated = transactionMapper.completeTransaction(transactionId, "COMPLETED", balanceAfter, completedAt);
                if (updated == 1) {
                    return true;
                }
                log.warn("거래 완료 처리 실패(영향 행 0), 재시도 {}/{}. transactionId={}", attempt, COMPLETE_MAX_ATTEMPTS, transactionId);
            } catch (RuntimeException e) {
                log.warn("거래 완료 처리 중 예외, 재시도 {}/{}. transactionId={}", attempt, COMPLETE_MAX_ATTEMPTS, transactionId, e);
            }
            if (attempt < COMPLETE_MAX_ATTEMPTS) {
                sleep(COMPLETE_RETRY_DELAY_MS);
            }
        }
        return false;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void markFailed(Long transactionId, String reason) {
        log.error("송금 확정 실패, FAILED로 표시. transactionId={}, reason={}", transactionId, reason);
        try {
            transactionMapper.updateStatus(transactionId, "FAILED");
        } catch (RuntimeException e) {
            log.error("FAILED 상태 기록마저 실패. transactionId={}", transactionId, e);
        }
    }
}
