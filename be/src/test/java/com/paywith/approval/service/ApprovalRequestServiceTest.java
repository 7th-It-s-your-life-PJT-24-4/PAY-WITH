package com.paywith.approval.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

import com.paywith.approval.domain.ApprovalRequest;
import com.paywith.approval.mapper.ApprovalRequestMapper;
import com.paywith.approval.mapper.TransactionApprovalMapper;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApprovalRequestServiceTest {

    private static final int EXPIRE_MINUTES = 30;

    @Mock
    private ApprovalRequestMapper approvalRequestMapper;

    @Mock
    private TransactionApprovalMapper transactionApprovalMapper;

    // PENDING 상태와 만료시각(생성 시점 +30분)이 설정되어 저장된다
    @Test
    void create_savesPendingRequestWithExpiryTime() {
        ApprovalRequestService service = new ApprovalRequestServiceImpl(
            approvalRequestMapper, transactionApprovalMapper, EXPIRE_MINUTES);
        LocalDateTime before = LocalDateTime.now();

        ApprovalRequest created = service.create(100L);

        LocalDateTime after = LocalDateTime.now();
        ArgumentCaptor<ApprovalRequest> captor = ArgumentCaptor.forClass(ApprovalRequest.class);
        then(approvalRequestMapper).should().insert(captor.capture());
        ApprovalRequest saved = captor.getValue();
        assertThat(saved).isSameAs(created);
        assertThat(saved.getTransactionId()).isEqualTo(100L);
        assertThat(saved.getStatus()).isEqualTo("PENDING");
        // 만료 시각 = 생성 시점 + 30분 (호출 전후 시각 사이 범위로 검증)
        assertThat(saved.getExpiredAt())
            .isAfterOrEqualTo(before.plusMinutes(EXPIRE_MINUTES))
            .isBeforeOrEqualTo(after.plusMinutes(EXPIRE_MINUTES));
    }
}
