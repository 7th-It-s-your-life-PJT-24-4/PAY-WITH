package com.paywith.transfer.service;

import com.paywith.exception.BusinessException;
import com.paywith.external.openbanking.OpenBankingClient;
import com.paywith.fds.domain.RiskLevel;
import com.paywith.transaction.mapper.TransactionMapper;
import com.paywith.transfer.dto.PreparedTransfer;
import com.paywith.transfer.dto.TransferRequest;
import com.paywith.transfer.dto.TransferResponse;
import com.paywith.wallet.domain.Wallet;
import com.paywith.wallet.mapper.WalletMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TransferFinalizationServiceImpl implements TransferFinalizationService {

    private final WalletMapper walletMapper;
    private final TransactionMapper transactionMapper;
    private final OpenBankingClient openBankingClient;

    @Override
    @Transactional
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
        Long balanceAfter = updatedWallet.getBalance();

        // 10. 입금 이체
        openBankingClient.deposit(request.getBankCode(), request.getAccountNo(), request.getAmount());

        // 11. 거래 기록 변경 (잔액 업데이트, 완료 시각, 상태)
        // update 거래 실패 시 확인
        LocalDateTime completedAt = LocalDateTime.now();
        int completedUpdated = transactionMapper.completeTransaction(transactionId, "COMPLETED", balanceAfter, completedAt);
        if (completedUpdated != 1) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "거래 완료 처리에 실패했습니다. transactionId=" + transactionId);
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
}