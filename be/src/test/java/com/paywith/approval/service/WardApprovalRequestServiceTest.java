package com.paywith.approval.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.paywith.approval.domain.ApprovalRequestView;
import com.paywith.approval.dto.WardApprovalCancelResponse;
import com.paywith.approval.dto.WardApprovalDetailResponse;
import com.paywith.approval.mapper.ApprovalRequestMapper;
import com.paywith.approval.mapper.TransactionApprovalMapper;
import com.paywith.exception.BusinessException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
@DisplayName("피보호자 승인요청 조회")
class WardApprovalRequestServiceTest {

    private static final Long WARD_ID = 42L;
    private static final Long APPROVAL_ID = 100L;
    private static final Long TRANSACTION_ID = 500L;

    @Mock
    private ApprovalRequestMapper approvalRequestMapper;

    @Mock
    private TransactionApprovalMapper transactionApprovalMapper;

    private WardApprovalRequestService service;

    @BeforeEach
    void setUp() {
        service = new WardApprovalRequestServiceImpl(
            approvalRequestMapper, transactionApprovalMapper);
    }

    private ApprovalRequestView view() {
        ApprovalRequestView view = new ApprovalRequestView();
        view.setApprovalId(APPROVAL_ID);
        view.setTransactionId(TRANSACTION_ID);
        view.setWardId(WARD_ID);
        view.setWardName("김시니어");
        view.setType("TRANSFER_OUT");
        view.setAmount(new BigDecimal("2000000"));
        view.setMemo("생활비");
        view.setHolderName("박수취");
        view.setBankName("신한은행");
        view.setAccountNo("110234567890");
        view.setRiskLevel("DANGER");
        view.setTotalScore(64);
        view.setRequestedAt(LocalDateTime.now());
        view.setExpiredAt(LocalDateTime.now().plusMinutes(30));
        return view;
    }

    @Test
    void findDetailByWard_mapsViewToResponse() {
        given(approvalRequestMapper.findByIdAndWardId(APPROVAL_ID, WARD_ID)).willReturn(view());

        WardApprovalDetailResponse result = service.findDetailByWard(APPROVAL_ID, WARD_ID);

        assertThat(result.getApprovalId()).isEqualTo(APPROVAL_ID);
        assertThat(result.getTransactionId()).isEqualTo(TRANSACTION_ID);
        assertThat(result.getType()).isEqualTo("TRANSFER_OUT");
        assertThat(result.getAmount()).isEqualByComparingTo("2000000");
        assertThat(result.getHolderName()).isEqualTo("박수취");
        assertThat(result.getBankName()).isEqualTo("신한은행");
        assertThat(result.getAccountNo()).isEqualTo("110234567890");
        assertThat(result.getRiskLevel()).isEqualTo("DANGER");
    }

    // 홈 목록에는 없는 memo 를 상세에서는 담는다. 본인이 입력한 값이라 감출 이유가 없다
    @Test
    void findDetailByWard_includesMemo() {
        given(approvalRequestMapper.findByIdAndWardId(APPROVAL_ID, WARD_ID)).willReturn(view());

        assertThat(service.findDetailByWard(APPROVAL_ID, WARD_ID).getMemo()).isEqualTo("생활비");
    }

    // 보류 사유는 피보호자에게 내보내지 않는다. 조회 자체를 하지 않아야 응답에 섞일 여지가 없다
    @Test
    void findDetailByWard_neverQueriesRuleHits() {
        given(approvalRequestMapper.findByIdAndWardId(APPROVAL_ID, WARD_ID)).willReturn(view());

        service.findDetailByWard(APPROVAL_ID, WARD_ID);

        then(approvalRequestMapper).should(never()).findRuleHits(anyLong());
    }

    // 인가 판정 축이 보호자용과 다르다. 담당 관계(guard_senior)가 아니라 지갑 소유자로 가른다
    @Test
    void findDetailByWard_authorizesByWalletOwnerNotGuardRelation() {
        given(approvalRequestMapper.findByIdAndWardId(APPROVAL_ID, WARD_ID)).willReturn(view());

        service.findDetailByWard(APPROVAL_ID, WARD_ID);

        then(approvalRequestMapper).should().findByIdAndWardId(APPROVAL_ID, WARD_ID);
        then(approvalRequestMapper).should(never()).findByIdAndGuardId(anyLong(), anyLong());
    }

    // 없는 ID·남의 건·이미 처리·만료된 건을 매퍼가 모두 null 로 돌려주므로 셋 다 404 로 합쳐진다.
    // 남의 건에만 403 을 주면 승인요청의 존재 여부가 새어나간다
    @Test
    void findDetailByWard_failsWhenNotOwnPendingRequest() {
        given(approvalRequestMapper.findByIdAndWardId(APPROVAL_ID, WARD_ID)).willReturn(null);

        assertThatThrownBy(() -> service.findDetailByWard(APPROVAL_ID, WARD_ID))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
    }

    // 지갑은 WARD 에게만 생성되므로 보호자 ID 로는 조인이 비어 같은 404 가 된다.
    // 서비스가 역할 검사를 따로 두지 않는 근거다
    @Test
    void findDetailByWard_failsForGuardIdWithoutRoleCheck() {
        given(approvalRequestMapper.findByIdAndWardId(APPROVAL_ID, 10L)).willReturn(null);

        assertThatThrownBy(() -> service.findDetailByWard(APPROVAL_ID, 10L))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
    }

    @Test
    void cancel_changesOwnedPendingRequestAndHeldTransaction() {
        given(approvalRequestMapper.findTransactionIdByIdAndWardId(APPROVAL_ID, WARD_ID))
            .willReturn(TRANSACTION_ID);
        given(approvalRequestMapper.cancelByWard(
            org.mockito.ArgumentMatchers.eq(APPROVAL_ID),
            org.mockito.ArgumentMatchers.eq(WARD_ID),
            any(LocalDateTime.class)))
            .willReturn(1);
        given(transactionApprovalMapper.cancelHeldByWard(TRANSACTION_ID)).willReturn(1);

        WardApprovalCancelResponse result = service.cancel(APPROVAL_ID, WARD_ID);

        assertThat(result.getApprovalId()).isEqualTo(APPROVAL_ID);
        assertThat(result.getTransactionId()).isEqualTo(TRANSACTION_ID);
        assertThat(result.getStatus()).isEqualTo("CANCELED");
        assertThat(result.getCanceledAt()).isNotNull();
        then(transactionApprovalMapper).should().cancelHeldByWard(TRANSACTION_ID);
    }

    @Test
    void cancel_returnsNotFoundForMissingOrOtherWardsRequest() {
        given(approvalRequestMapper.findTransactionIdByIdAndWardId(APPROVAL_ID, WARD_ID))
            .willReturn(null);

        assertThatThrownBy(() -> service.cancel(APPROVAL_ID, WARD_ID))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);

        then(approvalRequestMapper).should(never())
            .cancelByWard(anyLong(), anyLong(), any(LocalDateTime.class));
        then(transactionApprovalMapper).should(never()).cancelHeldByWard(anyLong());
    }

    @Test
    void cancel_returnsConflictWhenRequestWasProcessedOrExpired() {
        given(approvalRequestMapper.findTransactionIdByIdAndWardId(APPROVAL_ID, WARD_ID))
            .willReturn(TRANSACTION_ID);
        given(approvalRequestMapper.cancelByWard(
            org.mockito.ArgumentMatchers.eq(APPROVAL_ID),
            org.mockito.ArgumentMatchers.eq(WARD_ID),
            any(LocalDateTime.class)))
            .willReturn(0);

        assertThatThrownBy(() -> service.cancel(APPROVAL_ID, WARD_ID))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT);

        then(transactionApprovalMapper).should(never()).cancelHeldByWard(anyLong());
    }

    @Test
    void cancel_returnsConflictWhenTransactionIsNotHeld() {
        given(approvalRequestMapper.findTransactionIdByIdAndWardId(APPROVAL_ID, WARD_ID))
            .willReturn(TRANSACTION_ID);
        given(approvalRequestMapper.cancelByWard(
            org.mockito.ArgumentMatchers.eq(APPROVAL_ID),
            org.mockito.ArgumentMatchers.eq(WARD_ID),
            any(LocalDateTime.class)))
            .willReturn(1);
        given(transactionApprovalMapper.cancelHeldByWard(TRANSACTION_ID)).willReturn(0);

        assertThatThrownBy(() -> service.cancel(APPROVAL_ID, WARD_ID))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT);
    }
}
