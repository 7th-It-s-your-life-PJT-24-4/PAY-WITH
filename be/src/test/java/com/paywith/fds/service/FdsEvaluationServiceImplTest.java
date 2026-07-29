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
import com.paywith.fds.domain.RiskLevel;
import com.paywith.fds.domain.RiskRule;
import com.paywith.fds.dto.FdsDecision;
import com.paywith.fds.dto.FdsEvaluationRequest;
import com.paywith.fds.mapper.RiskEvaluationDetailMapper;
import com.paywith.fds.mapper.RiskEvaluationMapper;
import com.paywith.fds.mapper.TransactionRiskMapper;
import com.paywith.fds.service.rule.RiskRuleCache;
import com.paywith.fds.service.rule.RuleContext;
import com.paywith.fds.support.FdsTestWiring;
import com.paywith.fds.support.RiskRules;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("FDS 평가 서비스")
class FdsEvaluationServiceImplTest {

    private static final Long TRANSACTION_ID = 100L;

    @Mock
    private RuleContextCollectorService ruleContextCollectorService;
    @Mock
    private RiskRuleCache riskRuleCache;
    @Mock
    private RiskEvaluationMapper riskEvaluationMapper;
    @Mock
    private RiskEvaluationDetailMapper riskEvaluationDetailMapper;
    @Mock
    private TransactionRiskMapper transactionRiskMapper;
    @Mock
    private ApprovalRequestService approvalRequestService;

    private FdsEvaluationServiceImpl service;

    @BeforeEach
    void setUp() {
        List<RiskRule> activeRules = RiskRules.activeRules();
        lenient().when(riskRuleCache.getActiveRules()).thenReturn(activeRules);
        for (RiskRule rule : activeRules) {
            lenient().when(riskRuleCache.findByCode(rule.getRuleCode()))
                .thenReturn(Optional.of(rule));
        }
        // 정상 경로에서는 대상 거래가 존재하므로 1행이 갱신된다
        lenient().when(transactionRiskMapper.updateRiskScore(anyLong(), anyInt())).thenReturn(1);

        service = new FdsEvaluationServiceImpl(
            ruleContextCollectorService,
            riskRuleCache,
            FdsTestWiring.grader(),
            FdsTestWiring.preFilters(),
            FdsTestWiring.evaluators(),
            riskEvaluationMapper,
            riskEvaluationDetailMapper,
            transactionRiskMapper,
            approvalRequestService
        );
    }

    @Nested
    @DisplayName("판정")
    class Evaluate {

        @Test
        void evaluate_delegatesContextCollectingToCollector() {
            FdsEvaluationRequest request =
                new FdsEvaluationRequest(TRANSACTION_ID, 1L, 2L, won("30000"), null);
            given(ruleContextCollectorService.collect(request)).willReturn(normal().build());

            FdsDecision decision = service.evaluate(request);

            assertThat(decision.getRiskLevel()).isEqualTo(RiskLevel.SAFE);
            then(ruleContextCollectorService).should().collect(request);
        }

        @Test
        void decide_sumsScoresOfTriggeredRulesOnly() {
            // 신규(15) + 고액 L1(10) = 25
            RuleContext context = normal().recipientSendCount(0).amount(won("800000")).build();

            FdsDecision decision = service.decide(context);

            assertThat(decision.getTotalScore()).isEqualTo(25);
            assertThat(decision.getDecidedBy()).isEqualTo(DecidedBy.RULE);
            assertThat(decision.getTriggeredRules()).hasSize(2);
        }

        // 감점만 발동하면 합산이 음수가 되지만 저장·판정에는 0을 쓴다
        @Test
        void decide_clampsNegativeTotalToZero() {
            RuleContext context = normal().recipientRegisteredSafe(true).build();

            FdsDecision decision = service.decide(context);

            assertThat(decision.getTotalScore()).isZero();
            assertThat(decision.getRiskLevel()).isEqualTo(RiskLevel.SAFE);
        }

        // 단축평가 전용 행(BL_*/WL_*)은 평가기가 없으므로 점수 합산에 섞이면 안 된다
        @Test
        void decide_ignoresRuleRowsWithoutEvaluator() {
            FdsDecision decision = service.decide(normal().build());

            assertThat(decision.getTotalScore()).isZero();
            assertThat(decision.getTriggeredRules()).isEmpty();
        }

        @Test
        void decide_carriesThresholdSnapshot() {
            FdsDecision decision = service.decide(normal().build());

            assertThat(decision.getCautionThreshold()).isEqualTo(FdsTestWiring.CAUTION_THRESHOLD);
            assertThat(decision.getDangerThreshold()).isEqualTo(FdsTestWiring.DANGER_THRESHOLD);
        }
    }

    @Nested
    @DisplayName("판정 결과 저장")
    class SaveDecision {

        @Test
        void saveDecision_savesEvaluationWithLevelAndThresholds() {
            FdsDecision decision = service.decide(
                normal().recipientSendCount(0).amount(won("800000")).build());

            service.saveDecision(TRANSACTION_ID, decision);

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
        void saveDecision_savesOneDetailPerTriggeredRule() {
            FdsDecision decision = service.decide(
                normal().recipientSendCount(0).amount(won("800000")).build());

            service.saveDecision(TRANSACTION_ID, decision);

            then(riskEvaluationDetailMapper).should(org.mockito.Mockito.times(2))
                .insert(any(RiskEvaluationDetail.class));
        }

        // risk_score 는 total_score 의 비정규화 복사본이라 같은 트랜잭션에서 갱신해야 한다
        @Test
        void saveDecision_updatesDenormalizedRiskScore() {
            FdsDecision decision = service.decide(
                normal().recipientSendCount(0).amount(won("800000")).build());

            service.saveDecision(TRANSACTION_ID, decision);

            then(transactionRiskMapper).should().updateRiskScore(TRANSACTION_ID, 25);
        }

        @Test
        void saveDecision_createsApprovalRequestWhenDangerous() {
            FdsDecision decision = service.decide(
                normal().recipientSendCount(0).amount(won("3000000")).build());
            assertThat(decision.getRiskLevel()).isEqualTo(RiskLevel.DANGER);

            service.saveDecision(TRANSACTION_ID, decision);

            then(approvalRequestService).should().create(TRANSACTION_ID);
        }

        // 주의 등급은 알림만 보내고 송금은 진행하므로 승인요청을 만들지 않는다
        @Test
        void saveDecision_doesNotCreateApprovalRequestWhenCaution() {
            FdsDecision decision = service.decide(
                normal().recipientSendCount(0).amount(won("800000")).build());
            assertThat(decision.getRiskLevel()).isEqualTo(RiskLevel.CAUTION);

            service.saveDecision(TRANSACTION_ID, decision);

            then(approvalRequestService).should(never()).create(anyLong());
        }

        @Test
        void saveDecision_doesNotCreateApprovalRequestWhenSafe() {
            FdsDecision decision = service.decide(normal().build());

            service.saveDecision(TRANSACTION_ID, decision);

            then(approvalRequestService).should(never()).create(anyLong());
        }

        @Test
        void saveDecision_savesPrefilterRuleAsDetailWithZeroScore() {
            FdsDecision decision = service.decide(normal().recipientRejectedBefore(true).build());

            service.saveDecision(TRANSACTION_ID, decision);

            ArgumentCaptor<RiskEvaluationDetail> captor =
                ArgumentCaptor.forClass(RiskEvaluationDetail.class);
            then(riskEvaluationDetailMapper).should().insert(captor.capture());
            assertThat(captor.getValue().getScore()).isZero();
            then(transactionRiskMapper).should().updateRiskScore(eq(TRANSACTION_ID), eq(0));
        }

        // UPDATE 는 대상이 없어도 조용히 0행이 되므로, 데이터 불일치를 여기서 잡아야 한다
        @Test
        void saveDecision_failsWhenTransactionRowIsMissing() {
            given(transactionRiskMapper.updateRiskScore(anyLong(), anyInt())).willReturn(0);
            FdsDecision decision = service.decide(normal().build());

            assertThatThrownBy(() -> service.saveDecision(TRANSACTION_ID, decision))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(String.valueOf(TRANSACTION_ID));
        }
    }
}
