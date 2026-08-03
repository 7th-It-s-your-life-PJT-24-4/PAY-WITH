package com.paywith.approval.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;

import com.paywith.approval.mapper.ApprovalRequestMapper;
import com.paywith.approval.mapper.TransactionApprovalMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("승인요청 만료 처리")
class ApprovalExpiryServiceImplTest {

    @Mock
    private ApprovalRequestMapper approvalRequestMapper;

    @Mock
    private TransactionApprovalMapper transactionApprovalMapper;

    private ApprovalExpiryService service;

    @BeforeEach
    void setUp() {
        service = new ApprovalExpiryServiceImpl(approvalRequestMapper, transactionApprovalMapper);
    }

    @Test
    void expireOverdue_cancelsTransactionsBeforeExpiringApprovals() {
        given(transactionApprovalMapper.cancelHeldForExpiredApprovals()).willReturn(3);
        given(approvalRequestMapper.expireOverdue()).willReturn(3);

        int result = service.expireOverdue();

        assertThat(result).isEqualTo(3);
        // 승인요청을 먼저 EXPIRED 로 바꾸면 거래 쪽 조인이 PENDING 을 찾지 못해 HELD 가 남는다
        InOrder order = inOrder(transactionApprovalMapper, approvalRequestMapper);
        order.verify(transactionApprovalMapper).cancelHeldForExpiredApprovals();
        order.verify(approvalRequestMapper).expireOverdue();
    }

    @Test
    void expireOverdue_returnsZeroWhenNothingIsOverdue() {
        given(transactionApprovalMapper.cancelHeldForExpiredApprovals()).willReturn(0);
        given(approvalRequestMapper.expireOverdue()).willReturn(0);

        assertThat(service.expireOverdue()).isZero();
    }

    @Test
    void expireOverdue_propagatesFailureSoTransactionRollsBack() {
        given(transactionApprovalMapper.cancelHeldForExpiredApprovals())
            .willThrow(new IllegalStateException("db down"));

        assertThatThrownBy(() -> service.expireOverdue())
            .isInstanceOf(IllegalStateException.class);

        // 거래 취소가 실패하면 승인요청도 만료시키지 않는다. 한쪽만 종결되면 상태가 어긋난다
        then(approvalRequestMapper).should(never()).expireOverdue();
    }
}
