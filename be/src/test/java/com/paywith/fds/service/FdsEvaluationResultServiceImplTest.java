package com.paywith.fds.service;

import static com.paywith.fds.support.RuleContexts.normal;
import static com.paywith.fds.support.RuleContexts.won;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;

import com.paywith.approval.service.ApprovalRequestService;
import com.paywith.exception.BusinessException;
import com.paywith.fds.domain.DecidedBy;
import com.paywith.fds.domain.RiskEvaluation;
import com.paywith.fds.domain.RiskEvaluationDetail;
import com.paywith.approval.domain.ApprovalRequest;
import com.paywith.fds.domain.RiskLevel;
import com.paywith.fds.domain.RiskRule;
import com.paywith.fds.dto.FdsDecision;
import com.paywith.fds.mapper.RiskEvaluationDetailMapper;
import com.paywith.fds.mapper.RiskEvaluationMapper;
import com.paywith.fds.mapper.TransactionRiskMapper;
import com.paywith.notification.service.TransferNotifier;
import com.paywith.fds.service.rule.RiskRuleCache;
import com.paywith.fds.support.FdsTestWiring;
import com.paywith.fds.support.RiskRules;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("FDS 판정 결과 저장")
class FdsEvaluationResultServiceImplTest {

    private static final Long APPROVAL_ID = 500L;
    private static final Long TRANSACTION_ID = 100L;

    @Mock
    private RiskRuleCache riskRuleCache;
    @Mock
    private RiskEvaluationMapper riskEvaluationMapper;
    @Mock
    private RiskEvaluationDetailMapper riskEvaluationDetailMapper;
    @Mock
    private TransactionRiskMapper transactionRiskMapper;
    @Mock
    private TransferNotifier transferNotifier;
    @Mock
    private ApprovalRequestService approvalRequestService;

    private FdsEvaluationResultServiceImpl service;

    /** 저장할 FdsDecision 을 실제 판정으로 만들기 위한 것. */
    private FdsEvaluationServiceImpl decider;

    @BeforeEach
    void setUp() {
        List<RiskRule> activeRules = RiskRules.activeRules();
        lenient().when(riskRuleCache.getActiveRules()).thenReturn(activeRules);
        for (RiskRule rule : activeRules) {
            lenient().when(riskRuleCache.findByCode(rule.getRuleCode()))
                .thenReturn(Optional.of(rule));
        }
        lenient().when(transactionRiskMapper.updateRiskScore(anyLong(), anyInt())).thenReturn(1);

        service = new FdsEvaluationResultServiceImpl(
            riskEvaluationMapper,
            riskEvaluationDetailMapper,
            transactionRiskMapper,
            approvalRequestService,
            transferNotifier
        );
        decider = new FdsEvaluationServiceImpl(
            null, null, riskRuleCache, FdsTestWiring.grader(), FdsTestWiring.evaluators()
        );
    }

    /** 신규(15) + 고액 L1(10) = 25 → CAUTION */
    private FdsDecision cautionDecision() {
        return decider.decide(normal().recipientSendCount(0).amount(won("800000")).build());
    }

    @Test
    void save_savesEvaluationWithLevelAndThresholds() {
        service.save(TRANSACTION_ID, cautionDecision());

        ArgumentCaptor<RiskEvaluation> captor = ArgumentCaptor.forClass(RiskEvaluation.class);
        then(riskEvaluationMapper).should().insert(captor.capture());
        RiskEvaluation saved = captor.getValue();
        assertThat(saved.getTransactionId()).isEqualTo(TRANSACTION_ID);
        assertThat(saved.getTotalScore()).isEqualTo(25);
        assertThat(saved.getRiskLevel()).isEqualTo(RiskLevel.CAUTION);
        assertThat(saved.getDecidedBy()).isEqualTo(DecidedBy.RULE);
        assertThat(saved.getCautionThreshold()).isEqualTo(FdsTestWiring.CAUTION_THRESHOLD);
        assertThat(saved.getDangerThreshold()).isEqualTo(FdsTestWiring.DANGER_THRESHOLD);
    }

    @Test
    void save_savesOneDetailPerTriggeredRule() {
        service.save(TRANSACTION_ID, cautionDecision());

        then(riskEvaluationDetailMapper).should(org.mockito.Mockito.times(2))
            .insert(any(RiskEvaluationDetail.class));
    }

    @Test
    void save_updatesDenormalizedRiskScore() {
        service.save(TRANSACTION_ID, cautionDecision());

        then(transactionRiskMapper).should().updateRiskScore(TRANSACTION_ID, 25);
    }

    @Test
    void save_createsApprovalRequestWhenDangerous() {
        FdsDecision decision =
            decider.decide(normal().recipientSendCount(0).amount(won("3000000")).build());
        assertThat(decision.getRiskLevel()).isEqualTo(RiskLevel.DANGER);
        given(approvalRequestService.create(TRANSACTION_ID)).willReturn(approvalRequest(APPROVAL_ID));

        service.save(TRANSACTION_ID, decision);

        then(approvalRequestService).should().create(TRANSACTION_ID);
    }

    /**
     * 보호자가 알림을 누르면 승인 화면으로 가야 하는데 그 화면은 approvalId 로 열린다.
     * 방금 만든 승인요청의 ID 를 그대로 넘겨야 하고, 다시 조회해서는 안 된다.
     */
    @Test
    void save_passesCreatedApprovalIdToNotification() {
        FdsDecision decision =
            decider.decide(normal().recipientSendCount(0).amount(won("3000000")).build());
        given(approvalRequestService.create(TRANSACTION_ID)).willReturn(approvalRequest(APPROVAL_ID));

        service.save(TRANSACTION_ID, decision);

        then(transferNotifier).should().notifyApprovalRequested(TRANSACTION_ID, APPROVAL_ID);
        then(transferNotifier).should(never()).notifyRiskDetected(anyLong(), any());
    }

    private ApprovalRequest approvalRequest(Long approvalId) {
        ApprovalRequest approvalRequest = new ApprovalRequest();
        approvalRequest.setApprovalId(approvalId);
        return approvalRequest;
    }

    // 블랙리스트는 즉시 차단이라 보호자 승인 경로를 타지 않는다.
    // 승인 요청이 없다는 것은 보호자가 나중에 풀어줄 수도 없다는 뜻이다.
    @Test
    void save_doesNotCreateApprovalRequestWhenBlacklisted() {
        FdsDecision decision = decider.decide(normal().recipientRejectedBefore(true).build());
        assertThat(decision.getRiskLevel()).isEqualTo(RiskLevel.DANGER);
        assertThat(decision.isBlocked()).isTrue();

        service.save(TRANSACTION_ID, decision);

        then(approvalRequestService).should(never()).create(anyLong());
    }

    // 사기계좌도 거절이력과 동일하게 차단된다
    @Test
    void save_doesNotCreateApprovalRequestWhenFraudAccount() {
        FdsDecision decision = decider.decide(normal().recipientReportedAsFraud(true).build());
        assertThat(decision.isBlocked()).isTrue();

        service.save(TRANSACTION_ID, decision);

        then(approvalRequestService).should(never()).create(anyLong());
    }

    @Test
    void save_doesNotCreateApprovalRequestWhenCaution() {
        service.save(TRANSACTION_ID, cautionDecision());

        then(approvalRequestService).should(never()).create(anyLong());
    }

    @Test
    void save_doesNotCreateApprovalRequestWhenSafe() {
        service.save(TRANSACTION_ID, decider.decide(normal().build()));

        then(approvalRequestService).should(never()).create(anyLong());
    }

    // 상세 배점·총점·비정규화 복사본이 모두 카탈로그 배점으로 일치해야 한다
    @Test
    void save_savesPrefilterRuleAsDetailWithMaxScore() {
        FdsDecision decision = decider.decide(normal().recipientRejectedBefore(true).build());

        service.save(TRANSACTION_ID, decision);

        ArgumentCaptor<RiskEvaluationDetail> captor =
            ArgumentCaptor.forClass(RiskEvaluationDetail.class);
        then(riskEvaluationDetailMapper).should().insert(captor.capture());
        assertThat(captor.getValue().getScore()).isEqualTo(RiskRules.PREFILTER_SCORE);
        then(transactionRiskMapper).should()
            .updateRiskScore(eq(TRANSACTION_ID), eq(RiskRules.PREFILTER_SCORE));
    }

    // UPDATE 는 대상이 없어도 조용히 0행이 된다
    @Test
    void save_failsWhenTransactionRowIsMissing() {
        given(transactionRiskMapper.updateRiskScore(anyLong(), anyInt())).willReturn(0);
        FdsDecision decision = decider.decide(normal().build());

        assertThatThrownBy(() -> service.save(TRANSACTION_ID, decision))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining(String.valueOf(TRANSACTION_ID));
    }
}
