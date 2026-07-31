package com.paywith.approval.service;

import com.paywith.approval.dto.ApprovalDecisionResponse;
import com.paywith.approval.dto.TransferResultResponse;
import com.paywith.approval.mapper.TransactionApprovalMapper;
import com.paywith.exception.BusinessException;
import com.paywith.exception.TransferIrrecoverableException;
import com.paywith.transfer.dto.TransferResponse;
import com.paywith.transfer.service.TransferFinalizationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 보호자 승인과 그 뒤에 이어지는 송금 실행을 순서대로 엮는다.
 *
 * <p><b>이 클래스에는 절대 {@code @Transactional} 을 붙이지 않는다.</b> 승인 기록은 되돌릴 수
 * 있지만(DB) 입금은 되돌릴 수 없어서(외부), 둘을 한 트랜잭션에 묶으면 입금이 나간 뒤 롤백될 때
 * 돈만 빠져나가고 기록은 사라진다. {@link ApprovalRequestService#approve} 가 프록시를 빠져나오며
 * 커밋을 끝낸 다음에 송금을 시작해야 하고, 그 커밋 경계를 만들려고 별도 빈으로 분리했다.
 * 같은 클래스 안에서 호출하면 프록시를 거치지 않아 이 경계가 생기지 않는다.
 *
 * <p>{@link TransferFinalizationService} 안의 {@code TransactionTemplate} 도 전파 방식이
 * REQUIRED 라, 여기에 트랜잭션이 걸려 있으면 자기 트랜잭션을 열지 못하고 합류해버린다.
 */
@Service
@RequiredArgsConstructor
public class ApprovalTransferFacade {

    private static final Logger log = LoggerFactory.getLogger(ApprovalTransferFacade.class);

    private final ApprovalRequestService approvalRequestService;
    private final TransferFinalizationService transferFinalizationService;
    private final TransactionApprovalMapper transactionApprovalMapper;

    /**
     * 보류된 송금을 승인하고, 이어서 실제 송금을 실행한다.
     *
     * <p>승인 단계의 실패(담당 아님 404, 이미 처리됨 409)는 그대로 던져서 승인이 성립하지
     * 않았음을 알린다. 반면 승인이 확정된 뒤의 송금 실패는 예외로 던지지 않고 응답에 담는다.
     * 이 시점의 승인은 이미 커밋돼 되돌릴 수 없고, 여기서 500 을 내보내면 보호자가 승인이
     * 실패한 줄 알고 다시 눌러 409 를 받게 되기 때문이다.
     */
    public ApprovalDecisionResponse approveAndTransfer(Long approvalId, Long guardId) {
        ApprovalDecisionResponse decision = approvalRequestService.approve(approvalId, guardId);
        return decision.withTransfer(executeTransfer(decision.getTransactionId()));
    }

    /** 송금 실행의 성패를 예외 대신 결과 객체로 바꾼다. */
    private TransferResultResponse executeTransfer(Long transactionId) {
        try {
            TransferResponse transfer =
                transferFinalizationService.finalizeApprovedTransfer(transactionId);
            return TransferResultResponse.completed(
                transfer.getCompletedAt(), transfer.getBalanceAfter());

        } catch (TransferIrrecoverableException e) {
            // 입금 호출을 통과한 뒤의 실패라 실제로 돈이 나갔을 수 있다. 자동 재시도 대상이
            // 아니며 사람이 확인해야 하므로 error 로 남긴다.
            log.error("승인 후 송금 실패(복구 불가). transactionId={}", transactionId, e);
            return TransferResultResponse.failed(e.getMessage());

        } catch (BusinessException e) {
            // 잔액 부족 등 입금 이전 단계의 실패. 외부에 영향이 없고 원인도 분명하므로 여기서
            // 거래를 종결한다. 자동 재시도는 하지 않는다 — 승인 시점과 출금 시점이 벌어지면
            // 시니어가 예상하지 못한 때에 돈이 나가기 때문이다. 다시 보내려면 새 송금을 만든다.
            log.warn("승인 후 송금 실패. transactionId={}, reason={}", transactionId, e.getMessage());
            markFailed(transactionId);
            return TransferResultResponse.failed(e.getMessage());

        } catch (RuntimeException e) {
            // 원인을 모르는 실패. 거래를 APPROVED 로 남겨 보정 대상이 되게 한다. 내부 메시지는
            // 그대로 내보내지 않는다.
            log.error("승인 후 송금 중 예기치 못한 오류. transactionId={}", transactionId, e);
            return TransferResultResponse.failed("송금 처리 중 오류가 발생했습니다. 잠시 후 다시 확인해주세요.");
        }
    }

    /** 종결 기록이 실패해도 승인·송금 결과 응답까지 뒤집지는 않는다. */
    private void markFailed(Long transactionId) {
        try {
            if (transactionApprovalMapper.markFailedIfApproved(transactionId) == 0) {
                log.warn("거래가 이미 다른 상태로 진행돼 FAILED 로 종결하지 않았다. transactionId={}",
                    transactionId);
            }
        } catch (RuntimeException e) {
            log.error("거래를 FAILED 로 종결하지 못했다. transactionId={}", transactionId, e);
        }
    }
}
