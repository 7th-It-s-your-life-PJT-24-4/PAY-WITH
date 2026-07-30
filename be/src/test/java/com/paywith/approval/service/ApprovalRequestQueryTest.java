package com.paywith.approval.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.paywith.approval.domain.ApprovalRequestView;
import com.paywith.approval.dto.ApprovalRequestDetailResponse;
import com.paywith.approval.dto.ApprovalRequestSummaryResponse;
import com.paywith.approval.dto.ApprovalRuleHitResponse;
import com.paywith.approval.mapper.ApprovalRequestMapper;
import com.paywith.approval.mapper.TransactionApprovalMapper;
import com.paywith.exception.BusinessException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
@DisplayName("보호자 승인요청 조회")
class ApprovalRequestQueryTest {

    private static final Long GUARD_ID = 10L;
    private static final Long SENIOR_ID = 42L;
    private static final Long APPROVAL_ID = 100L;
    private static final Long TRANSACTION_ID = 500L;

    @Mock
    private ApprovalRequestMapper approvalRequestMapper;

    @Mock
    private TransactionApprovalMapper transactionApprovalMapper;

    private ApprovalRequestService service;

    @BeforeEach
    void setUp() {
        service = new ApprovalRequestServiceImpl(
            approvalRequestMapper, transactionApprovalMapper, 30);
    }

    private ApprovalRequestView view() {
        ApprovalRequestView view = new ApprovalRequestView();
        view.setApprovalId(APPROVAL_ID);
        view.setTransactionId(TRANSACTION_ID);
        view.setSeniorId(SENIOR_ID);
        view.setSeniorName("김시니어");
        view.setAmount(new BigDecimal("2000000"));
        view.setMemo("검찰 수사 협조 요청");
        view.setHolderName("박수취");
        view.setBankName("신한은행");
        view.setAccountNo("110234567890");
        view.setRiskLevel("DANGER");
        view.setTotalScore(64);
        view.setRequestedAt(LocalDateTime.now());
        view.setExpiredAt(LocalDateTime.now().plusMinutes(30));
        return view;
    }

    private ApprovalRuleHitResponse ruleHit(String code, String description) {
        ApprovalRuleHitResponse hit = new ApprovalRuleHitResponse();
        hit.setRuleCode(code);
        hit.setDescription(description);
        return hit;
    }

    @Test
    void findPending_mapsViewToSummary() {
        given(approvalRequestMapper.findPendingByGuardId(GUARD_ID, null)).willReturn(List.of(view()));

        List<ApprovalRequestSummaryResponse> result = service.findPending(GUARD_ID, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getApprovalId()).isEqualTo(APPROVAL_ID);
        assertThat(result.get(0).getSeniorId()).isEqualTo(SENIOR_ID);
        assertThat(result.get(0).getSeniorName()).isEqualTo("김시니어");
        assertThat(result.get(0).getRiskLevel()).isEqualTo("DANGER");
    }

    @Test
    void findPending_returnsEmptyWhenGuardHasNoWards() {
        given(approvalRequestMapper.findPendingByGuardId(GUARD_ID, null)).willReturn(List.of());

        assertThat(service.findPending(GUARD_ID, null)).isEmpty();
    }

    // seniorId 는 그대로 매퍼에 전달되어 SQL 조건으로 쓰인다
    @Test
    void findPending_passesSeniorIdToMapper() {
        given(approvalRequestMapper.findPendingByGuardId(GUARD_ID, SENIOR_ID))
            .willReturn(List.of(view()));

        List<ApprovalRequestSummaryResponse> result = service.findPending(GUARD_ID, SENIOR_ID);

        assertThat(result).hasSize(1);
        then(approvalRequestMapper).should().findPendingByGuardId(GUARD_ID, SENIOR_ID);
    }

    // 담당이 아닌 피보호자 ID 를 줘도 매퍼의 담당 조인에서 걸려 빈 목록이 된다(권한 오류가 아님)
    @Test
    void findPending_returnsEmptyWhenSeniorIsNotInCharge() {
        given(approvalRequestMapper.findPendingByGuardId(GUARD_ID, 999L)).willReturn(List.of());

        assertThat(service.findPending(GUARD_ID, 999L)).isEmpty();
    }

    @Test
    void findDetail_includesRuleHits() {
        given(approvalRequestMapper.findByIdAndGuardId(APPROVAL_ID, GUARD_ID)).willReturn(view());
        given(approvalRequestMapper.findRuleHits(TRANSACTION_ID)).willReturn(List.of(
            ruleHit("SUSPICIOUS_MEMO", "메모에 위험 키워드 포함"),
            ruleHit("NEW_RECIPIENT", "처음 송금하는 신규 수취인")));

        ApprovalRequestDetailResponse result = service.findDetail(APPROVAL_ID, GUARD_ID);

        // 합산 점수는 유지하고 룰별 점수만 내보내지 않는다
        assertThat(result.getTotalScore()).isEqualTo(64);
        assertThat(result.getRuleHits()).hasSize(2);
        // 배열 순서가 근거의 우선순위(점수 큰 순)를 전달한다
        assertThat(result.getRuleHits().get(0).getRuleCode()).isEqualTo("SUSPICIOUS_MEMO");
        assertThat(result.getRuleHits().get(1).getRuleCode()).isEqualTo("NEW_RECIPIENT");
    }

    // 담당이 아닌 보호자, 그리고 이미 처리·만료된 건은 매퍼가 null 을 돌려주므로 조회 자체가 막힌다
    @Test
    void findDetail_failsWhenNotPendingRequestOfGuard() {
        given(approvalRequestMapper.findByIdAndGuardId(APPROVAL_ID, GUARD_ID)).willReturn(null);

        assertThatThrownBy(() -> service.findDetail(APPROVAL_ID, GUARD_ID))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
        then(approvalRequestMapper).should(never()).findRuleHits(anyLong());
    }
}
