package com.paywith.approval.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.paywith.approval.dto.ApprovalDecisionResponse;
import com.paywith.approval.mapper.ApprovalRequestMapper;
import com.paywith.approval.mapper.TransactionApprovalMapper;
import com.paywith.notification.service.TransferNotifier;
import com.paywith.exception.BusinessException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
@DisplayName("보호자 승인/거절 처리")
class ApprovalRequestDecisionTest {

    private static final Long GUARD_ID = 10L;
    private static final Long APPROVAL_ID = 100L;
    private static final Long TRANSACTION_ID = 500L;

    @Mock
    private ApprovalRequestMapper approvalRequestMapper;

    @Mock
    private TransactionApprovalMapper transactionApprovalMapper;

    @Mock
    private TransferNotifier transferNotifier;

    private ApprovalRequestService service;

    @BeforeEach
    void setUp() {
        service = new ApprovalRequestServiceImpl(
            approvalRequestMapper, transactionApprovalMapper, transferNotifier, 30);
    }

    private void givenPendingRequestFound() {
        given(approvalRequestMapper.findTransactionIdByIdAndGuardId(APPROVAL_ID, GUARD_ID))
            .willReturn(TRANSACTION_ID);
        given(approvalRequestMapper.updateDecision(
            eq(APPROVAL_ID), eq(GUARD_ID), anyString(), any(LocalDateTime.class))).willReturn(1);
    }

    @Test
    void approve_marksApprovalAndTransactionApproved() {
        givenPendingRequestFound();

        ApprovalDecisionResponse result = service.approve(APPROVAL_ID, GUARD_ID);

        assertThat(result.getApprovalId()).isEqualTo(APPROVAL_ID);
        assertThat(result.getTransactionId()).isEqualTo(TRANSACTION_ID);
        assertThat(result.getStatus()).isEqualTo("APPROVED");
        assertThat(result.getRespondedAt()).isNotNull();
        // 승인요청과 거래 상태를 같은 값으로 함께 갱신한다
        then(approvalRequestMapper).should()
            .updateDecision(eq(APPROVAL_ID), eq(GUARD_ID), eq("APPROVED"), any(LocalDateTime.class));
        then(transactionApprovalMapper).should().updateStatus(TRANSACTION_ID, "APPROVED");
    }

    @Test
    void reject_marksApprovalAndTransactionRejected() {
        givenPendingRequestFound();

        ApprovalDecisionResponse result = service.reject(APPROVAL_ID, GUARD_ID);

        assertThat(result.getStatus()).isEqualTo("REJECTED");
        then(approvalRequestMapper).should()
            .updateDecision(eq(APPROVAL_ID), eq(GUARD_ID), eq("REJECTED"), any(LocalDateTime.class));
        then(transactionApprovalMapper).should().updateStatus(TRANSACTION_ID, "REJECTED");
    }

    // 응답에 담기는 처리 시각은 DB 에 기록한 값과 같아야 한다
    @Test
    void approve_returnsSameRespondedAtAsPersisted() {
        givenPendingRequestFound();

        ApprovalDecisionResponse result = service.approve(APPROVAL_ID, GUARD_ID);

        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        then(approvalRequestMapper).should()
            .updateDecision(eq(APPROVAL_ID), eq(GUARD_ID), eq("APPROVED"), captor.capture());
        assertThat(result.getRespondedAt()).isEqualTo(captor.getValue());
    }

    // 조건부 UPDATE 가 0 행이면 이미 처리됐거나 만료된 것이므로 거래 상태를 건드리지 않는다
    @Test
    void approve_failsWithConflictWhenNoLongerPending() {
        given(approvalRequestMapper.findTransactionIdByIdAndGuardId(APPROVAL_ID, GUARD_ID))
            .willReturn(TRANSACTION_ID);
        given(approvalRequestMapper.updateDecision(
            eq(APPROVAL_ID), eq(GUARD_ID), anyString(), any(LocalDateTime.class))).willReturn(0);

        assertThatThrownBy(() -> service.approve(APPROVAL_ID, GUARD_ID))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT);
        then(transactionApprovalMapper).should(never()).updateStatus(anyLong(), anyString());
    }

    @Test
    void reject_failsWithConflictWhenNoLongerPending() {
        given(approvalRequestMapper.findTransactionIdByIdAndGuardId(APPROVAL_ID, GUARD_ID))
            .willReturn(TRANSACTION_ID);
        given(approvalRequestMapper.updateDecision(
            eq(APPROVAL_ID), eq(GUARD_ID), anyString(), any(LocalDateTime.class))).willReturn(0);

        assertThatThrownBy(() -> service.reject(APPROVAL_ID, GUARD_ID))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT);
        then(transactionApprovalMapper).should(never()).updateStatus(anyLong(), anyString());
    }

    // 담당이 아닌 보호자는 매퍼가 null 을 돌려주므로 갱신 자체에 도달하지 못한다
    @Test
    void approve_failsWithNotFoundWhenGuardIsNotInCharge() {
        given(approvalRequestMapper.findTransactionIdByIdAndGuardId(APPROVAL_ID, GUARD_ID))
            .willReturn(null);

        assertThatThrownBy(() -> service.approve(APPROVAL_ID, GUARD_ID))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
        then(approvalRequestMapper).should(never())
            .updateDecision(anyLong(), anyLong(), anyString(), any(LocalDateTime.class));
        then(transactionApprovalMapper).should(never()).updateStatus(anyLong(), anyString());
    }

    @Test
    void reject_failsWithNotFoundWhenGuardIsNotInCharge() {
        given(approvalRequestMapper.findTransactionIdByIdAndGuardId(APPROVAL_ID, GUARD_ID))
            .willReturn(null);

        assertThatThrownBy(() -> service.reject(APPROVAL_ID, GUARD_ID))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
        then(approvalRequestMapper).should(never())
            .updateDecision(anyLong(), anyLong(), anyString(), any(LocalDateTime.class));
    }
}
