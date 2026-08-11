package com.paywith.approval.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.paywith.approval.dto.ApprovalDecisionResponse;
import com.paywith.approval.mapper.TransactionApprovalMapper;
import com.paywith.notification.service.TransferNotifier;
import com.paywith.exception.BusinessException;
import com.paywith.exception.TransferIrrecoverableException;
import com.paywith.transaction.domain.TransactionStatus;
import com.paywith.transfer.dto.TransferResponse;
import com.paywith.transfer.service.TransferFinalizationService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
@DisplayName("승인 후 송금 실행")
class ApprovalTransferFacadeTest {

    private static final Long GUARD_ID = 10L;
    private static final Long APPROVAL_ID = 100L;
    private static final Long TRANSACTION_ID = 500L;
    private static final LocalDateTime RESPONDED_AT = LocalDateTime.of(2026, 7, 31, 14, 2, 11);

    @Mock
    private ApprovalRequestService approvalRequestService;

    @Mock
    private TransferFinalizationService transferFinalizationService;

    @Mock
    private TransactionApprovalMapper transactionApprovalMapper;

    @Mock
    private TransferNotifier transferNotifier;

    private ApprovalTransferFacade facade;

    @BeforeEach
    void setUp() {
        facade = new ApprovalTransferFacade(
            approvalRequestService, transferFinalizationService, transactionApprovalMapper,
            transferNotifier);
    }

    private void givenApprovalSucceeds() {
        given(approvalRequestService.approve(APPROVAL_ID, GUARD_ID)).willReturn(
            new ApprovalDecisionResponse(APPROVAL_ID, TRANSACTION_ID, "APPROVED", RESPONDED_AT));
    }

    @Test
    void approveAndTransfer_returnsCompletedTransferWhenFinalizationSucceeds() {
        givenApprovalSucceeds();
        LocalDateTime completedAt = RESPONDED_AT.plusSeconds(2);
        given(transferFinalizationService.finalizeApprovedTransfer(TRANSACTION_ID)).willReturn(
            TransferResponse.builder()
                .transactionId(TRANSACTION_ID)
                .status(TransactionStatus.COMPLETED)
                .completedAt(completedAt)
                .balanceAfter(120_000L)
                .build());

        ApprovalDecisionResponse result = facade.approveAndTransfer(APPROVAL_ID, GUARD_ID);

        assertThat(result.getStatus()).isEqualTo("APPROVED");
        assertThat(result.getTransactionId()).isEqualTo(TRANSACTION_ID);
        assertThat(result.getTransfer().getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getTransfer().getCompletedAt()).isEqualTo(completedAt);
        assertThat(result.getTransfer().getBalanceAfter()).isEqualTo(120_000L);
        assertThat(result.getTransfer().getFailureReason()).isNull();
    }

    @Test
    void approveAndTransfer_notifiesCompletionOnlyWhenTransferSucceeds() {
        givenApprovalSucceeds();
        given(transferFinalizationService.finalizeApprovedTransfer(TRANSACTION_ID))
            .willReturn(TransferResponse.builder()
                .transactionId(TRANSACTION_ID)
                .status(TransactionStatus.COMPLETED)
                .completedAt(RESPONDED_AT.plusSeconds(2))
                .balanceAfter(120_000L)
                .build());

        facade.approveAndTransfer(APPROVAL_ID, GUARD_ID);

        then(transferNotifier).should().notifyTransferCompleted(TRANSACTION_ID);
        then(transferNotifier).should(never()).notifyTransferFailed(anyLong(), anyString());
        then(transferNotifier).should(never()).notifyTransferUnresolved(anyLong());
    }

    @Test
    void approveAndTransfer_keepsApprovalSuccessfulWhenBalanceIsInsufficient() {
        givenApprovalSucceeds();
        given(transferFinalizationService.finalizeApprovedTransfer(TRANSACTION_ID)).willThrow(
            new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "송금 가능한 잔액이 부족합니다."));

        ApprovalDecisionResponse result = facade.approveAndTransfer(APPROVAL_ID, GUARD_ID);

        // 승인은 이미 커밋돼 되돌릴 수 없으므로 예외로 뒤집지 않고 결과를 나눠서 전달한다
        assertThat(result.getStatus()).isEqualTo("APPROVED");
        assertThat(result.getRespondedAt()).isEqualTo(RESPONDED_AT);
        assertThat(result.getTransfer().getStatus()).isEqualTo("FAILED");
        assertThat(result.getTransfer().getFailureReason()).isEqualTo("송금 가능한 잔액이 부족합니다.");
        assertThat(result.getTransfer().getCompletedAt()).isNull();
        assertThat(result.getTransfer().getBalanceAfter()).isNull();
        // 입금 이전 실패라 원인이 분명하다 — APPROVED 에 방치하지 않고 거래를 종결한다
        then(transactionApprovalMapper).should().markFailedIfApproved(TRANSACTION_ID);
        // 돈이 나가기 전이라 "실패"로 알려도 되고, 다시 시도해도 안전하다
        then(transferNotifier).should()
            .notifyTransferFailed(TRANSACTION_ID, "송금 가능한 잔액이 부족합니다.");
        then(transferNotifier).should(never()).notifyTransferUnresolved(anyLong());
    }

    @Test
    void approveAndTransfer_keepsRespondingWhenClosingFailedTransactionThrows() {
        givenApprovalSucceeds();
        given(transferFinalizationService.finalizeApprovedTransfer(TRANSACTION_ID)).willThrow(
            new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "송금 가능한 잔액이 부족합니다."));
        given(transactionApprovalMapper.markFailedIfApproved(TRANSACTION_ID))
            .willThrow(new IllegalStateException("db down"));

        ApprovalDecisionResponse result = facade.approveAndTransfer(APPROVAL_ID, GUARD_ID);

        // 종결 기록이 실패해도 이미 확정된 승인 결과까지 뒤집지는 않는다
        assertThat(result.getStatus()).isEqualTo("APPROVED");
        assertThat(result.getTransfer().getStatus()).isEqualTo("FAILED");
    }

    @Test
    void approveAndTransfer_keepsApprovalSuccessfulWhenDepositIsIrrecoverable() {
        givenApprovalSucceeds();
        given(transferFinalizationService.finalizeApprovedTransfer(TRANSACTION_ID)).willThrow(
            new TransferIrrecoverableException("송금 처리 중 오류가 발생했습니다. 고객센터로 문의해주세요."));

        ApprovalDecisionResponse result = facade.approveAndTransfer(APPROVAL_ID, GUARD_ID);

        assertThat(result.getStatus()).isEqualTo("APPROVED");
        assertThat(result.getTransfer().getStatus()).isEqualTo("FAILED");
        assertThat(result.getTransfer().getFailureReason())
            .isEqualTo("송금 처리 중 오류가 발생했습니다. 고객센터로 문의해주세요.");
        // 입금 이후 실패는 송금 쪽이 이미 FAILED 로 기록했다 — 여기서 또 건드리면 안 된다
        then(transactionApprovalMapper).should(never()).markFailedIfApproved(anyLong());
        // 잔액은 이미 차감됐고 수취인 도달 여부를 모른다. "실패"로 알리면 다시 보내 이중 송금이 된다
        then(transferNotifier).should().notifyTransferUnresolved(TRANSACTION_ID);
        then(transferNotifier).should(never()).notifyTransferFailed(anyLong(), anyString());
    }

    @Test
    void approveAndTransfer_masksMessageOfUnexpectedFailure() {
        givenApprovalSucceeds();
        given(transferFinalizationService.finalizeApprovedTransfer(TRANSACTION_ID))
            .willThrow(new IllegalStateException("connection pool exhausted"));

        ApprovalDecisionResponse result = facade.approveAndTransfer(APPROVAL_ID, GUARD_ID);

        assertThat(result.getStatus()).isEqualTo("APPROVED");
        assertThat(result.getTransfer().getStatus()).isEqualTo("FAILED");
        // 내부 오류 메시지는 보호자에게 그대로 내보내지 않는다
        assertThat(result.getTransfer().getFailureReason()).doesNotContain("connection pool");
        // 원인을 모르므로 APPROVED 로 남겨 보정 대상이 되게 한다
        then(transactionApprovalMapper).should(never()).markFailedIfApproved(anyLong());
        // 어느 단계에서 터졌는지 모르므로 실패로 단정하지 않는다
        then(transferNotifier).should().notifyTransferUnresolved(TRANSACTION_ID);
        then(transferNotifier).should(never()).notifyTransferFailed(anyLong(), anyString());
    }

    @Test
    void approveAndTransfer_propagatesApprovalFailureWithoutRunningTransfer() {
        given(approvalRequestService.approve(APPROVAL_ID, GUARD_ID)).willThrow(
            new BusinessException(HttpStatus.CONFLICT, "이미 처리되었거나 만료된 승인요청입니다."));

        assertThatThrownBy(() -> facade.approveAndTransfer(APPROVAL_ID, GUARD_ID))
            .isInstanceOf(BusinessException.class)
            .hasMessage("이미 처리되었거나 만료된 승인요청입니다.");

        // 승인이 성립하지 않았으면 송금은 시작조차 하면 안 된다
        then(transferFinalizationService).should(never()).finalizeApprovedTransfer(anyLong());
    }
}
