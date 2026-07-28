package com.paywith.fds.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import com.paywith.approval.service.ApprovalRequestService;
import com.paywith.exception.BusinessException;
import com.paywith.fds.domain.RecipientRiskInfo;
import com.paywith.fds.domain.RiskEvaluation;
import com.paywith.fds.domain.RiskEvaluationDetail;
import com.paywith.fds.domain.RiskRule;
import com.paywith.fds.dto.FdsEvaluationRequest;
import com.paywith.fds.mapper.FdsHistoryMapper;
import com.paywith.fds.mapper.RiskEvaluationDetailMapper;
import com.paywith.fds.mapper.RiskEvaluationMapper;
import com.paywith.fds.service.rule.FdsScoreResult;
import com.paywith.fds.service.rule.RiskRuleCache;
import com.paywith.fds.service.rule.RiskRuleEvaluator;
import com.paywith.fds.service.rule.RuleContext;
import com.paywith.fds.service.rule.TriggeredRule;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FdsEvaluationServiceTest {

    private static final int THRESHOLD = 40;

    @Mock
    private RiskRuleCache riskRuleCache;

    @Mock
    private RiskEvaluationMapper riskEvaluationMapper;

    @Mock
    private RiskEvaluationDetailMapper riskEvaluationDetailMapper;

    @Mock
    private FdsHistoryMapper fdsHistoryMapper;

    @Mock
    private ApprovalRequestService approvalRequestService;

    private FdsEvaluationService service(List<RiskRuleEvaluator> evaluators) {
        return new FdsEvaluationService(
            riskRuleCache, evaluators, riskEvaluationMapper, riskEvaluationDetailMapper,
            fdsHistoryMapper, approvalRequestService,
            THRESHOLD, 30, 30, new String[] {"검찰", "안전계좌"}
        );
    }

    private static RiskRule rule(long ruleId, String ruleCode, int score) {
        RiskRule rule = new RiskRule();
        rule.setRuleId(ruleId);
        rule.setRuleCode(ruleCode);
        rule.setScore(score);
        rule.setActive(true);
        return rule;
    }

    /** 항상 지정된 결과를 반환하는 스텁 evaluator */
    private static RiskRuleEvaluator stubEvaluator(String ruleCode, boolean triggered) {
        return new RiskRuleEvaluator() {
            @Override
            public String getRuleCode() {
                return ruleCode;
            }

            @Override
            public boolean evaluate(RuleContext context) {
                return triggered;
            }
        };
    }

    private static RuleContext anyContext() {
        return new RuleContext(BigDecimal.valueOf(10000), null, java.time.LocalDateTime.now(),
            5, false, 0, 0, List.of());
    }

    // 발동한 룰(24+20)만 합산되고 미발동 룰(12)은 총점에서 제외된다
    @Test
    void score_sumsTriggeredRuleScores() {
        given(riskRuleCache.getActiveRules()).willReturn(List.of(
            rule(1L, "NEW_RECIPIENT", 24),
            rule(2L, "HIGH_AMOUNT", 20),
            rule(3L, "NIGHT_TIME", 12)
        ));
        FdsEvaluationService service = service(List.of(
            stubEvaluator("NEW_RECIPIENT", true),
            stubEvaluator("HIGH_AMOUNT", true),
            stubEvaluator("NIGHT_TIME", false)
        ));

        FdsScoreResult result = service.score(anyContext());

        assertThat(result.getTotalScore()).isEqualTo(44);
        assertThat(result.isHeld()).isTrue();
        assertThat(result.getTriggeredRules()).hasSize(2);
    }

    // 경계값: 총점이 threshold(40)와 정확히 같으면 보류된다 (>= 판정)
    @Test
    void score_holdsWhenScoreEqualsThreshold() {
        given(riskRuleCache.getActiveRules()).willReturn(List.of(rule(1L, "A", THRESHOLD)));
        FdsEvaluationService service = service(List.of(stubEvaluator("A", true)));

        FdsScoreResult result = service.score(anyContext());

        assertThat(result.getTotalScore()).isEqualTo(THRESHOLD);
        assertThat(result.isHeld()).isTrue();
    }

    // 경계값: 총점이 threshold보다 1점 모자라면 보류되지 않는다
    @Test
    void score_doesNotHoldBelowThreshold() {
        given(riskRuleCache.getActiveRules()).willReturn(List.of(rule(1L, "A", THRESHOLD - 1)));
        FdsEvaluationService service = service(List.of(stubEvaluator("A", true)));

        FdsScoreResult result = service.score(anyContext());

        assertThat(result.isHeld()).isFalse();
    }

    // 감점 룰(SAFE_ACCOUNT_CHECK -40)이 총점을 깎는다: 24+20-40=4점 → 통과
    @Test
    void score_appliesNegativeRuleScores() {
        given(riskRuleCache.getActiveRules()).willReturn(List.of(
            rule(1L, "NEW_RECIPIENT", 24),
            rule(2L, "HIGH_AMOUNT", 20),
            rule(3L, "SAFE_ACCOUNT_CHECK", -40)
        ));
        FdsEvaluationService service = service(List.of(
            stubEvaluator("NEW_RECIPIENT", true),
            stubEvaluator("HIGH_AMOUNT", true),
            stubEvaluator("SAFE_ACCOUNT_CHECK", true)
        ));

        FdsScoreResult result = service.score(anyContext());

        assertThat(result.getTotalScore()).isEqualTo(4);
        assertThat(result.isHeld()).isFalse();
    }

    // DB에 룰이 있어도 매핑된 evaluator가 없으면 건너뛴다 (룰 추가/코드 배포 시차 방어)
    @Test
    void score_ignoresRulesWithoutEvaluator() {
        given(riskRuleCache.getActiveRules()).willReturn(List.of(
            rule(1L, "NEW_RECIPIENT", 24),
            rule(99L, "UNKNOWN_RULE", 100)
        ));
        FdsEvaluationService service = service(List.of(stubEvaluator("NEW_RECIPIENT", true)));

        FdsScoreResult result = service.score(anyContext());

        assertThat(result.getTotalScore()).isEqualTo(24);
        assertThat(result.getTriggeredRules()).hasSize(1);
    }

    // mapper 조회 결과(수취인 정보·이체 이력)가 RuleContext에 정확히 조립되는지 캡처해서 필드별로 검증한다
    @Test
    void evaluate_buildsRuleContextFromHistoryAndRecipientInfo() {
        RecipientRiskInfo riskInfo = new RecipientRiskInfo();
        riskInfo.setSendCount(7);
        riskInfo.setRegisteredSafe(true);
        given(fdsHistoryMapper.findRecipientRiskInfo(20L)).willReturn(riskInfo);
        given(fdsHistoryMapper.countRecentTransfers(eq(10L), any())).willReturn(5);
        given(fdsHistoryMapper.countRecentDistinctRecipients(eq(10L), any())).willReturn(2);
        given(riskRuleCache.getActiveRules()).willReturn(List.of(rule(1L, "CAPTURE", 10)));

        RuleContext[] captured = new RuleContext[1];
        RiskRuleEvaluator capturingEvaluator = new RiskRuleEvaluator() {
            @Override
            public String getRuleCode() {
                return "CAPTURE";
            }

            @Override
            public boolean evaluate(RuleContext context) {
                captured[0] = context;
                return true;
            }
        };
        FdsEvaluationService service = service(List.of(capturingEvaluator));

        FdsScoreResult result = service.evaluate(
            new FdsEvaluationRequest(10L, 20L, BigDecimal.valueOf(600000), "안전계좌"));

        assertThat(result.getTotalScore()).isEqualTo(10);
        RuleContext context = captured[0];
        assertThat(context.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(600000));
        assertThat(context.getMemo()).isEqualTo("안전계좌");
        assertThat(context.getRecipientSendCount()).isEqualTo(7);
        assertThat(context.isRecipientRegisteredSafe()).isTrue();
        assertThat(context.getRecentTransferCount()).isEqualTo(5);
        assertThat(context.getRecentDistinctRecipientCount()).isEqualTo(2);
        assertThat(context.getMemoKeywords()).containsExactly("검찰", "안전계좌");
    }

    // 수취인 조회 결과가 없으면 BusinessException(404)을 던진다
    @Test
    void evaluate_throwsBusinessExceptionWhenRecipientNotFound() {
        given(fdsHistoryMapper.findRecipientRiskInfo(20L)).willReturn(null);
        FdsEvaluationService service = service(List.of());

        assertThatThrownBy(() -> service.evaluate(
            new FdsEvaluationRequest(10L, 20L, BigDecimal.valueOf(10000), null)))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("수취인");
    }

    // 평가 헤더 1건 + 걸린 룰 수만큼 상세가 저장되고, 값이 결과 그대로 매핑된다
    @Test
    void persist_savesEvaluationHeaderAndTriggeredRuleDetails() {
        FdsEvaluationService service = service(List.of());
        FdsScoreResult result = new FdsScoreResult(40, THRESHOLD, false,
            List.of(new TriggeredRule(1L, 24), new TriggeredRule(2L, 16)));

        service.persist(100L, result);

        ArgumentCaptor<RiskEvaluation> evaluationCaptor = ArgumentCaptor.forClass(RiskEvaluation.class);
        then(riskEvaluationMapper).should().insert(evaluationCaptor.capture());
        RiskEvaluation evaluation = evaluationCaptor.getValue();
        assertThat(evaluation.getTransactionId()).isEqualTo(100L);
        assertThat(evaluation.getTotalScore()).isEqualTo(40);
        assertThat(evaluation.getThreshold()).isEqualTo(THRESHOLD);
        assertThat(evaluation.isHeld()).isFalse();

        ArgumentCaptor<RiskEvaluationDetail> detailCaptor = ArgumentCaptor.forClass(RiskEvaluationDetail.class);
        then(riskEvaluationDetailMapper).should(times(2)).insert(detailCaptor.capture());
        assertThat(detailCaptor.getAllValues())
            .extracting(RiskEvaluationDetail::getRuleId, RiskEvaluationDetail::getScore)
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple(1L, 24),
                org.assertj.core.groups.Tuple.tuple(2L, 16)
            );
    }

    // 보류가 아니면 승인요청을 생성하지 않는다
    @Test
    void persist_doesNotCreateApprovalRequestWhenNotHeld() {
        FdsEvaluationService service = service(List.of());
        FdsScoreResult result = new FdsScoreResult(20, THRESHOLD, false, List.of());

        service.persist(100L, result);

        then(approvalRequestService).should(never()).create(anyLong());
    }

    // 보류(HELD)면 승인요청 생성까지 이어진다 (FDS 후처리의 핵심 분기)
    @Test
    void persist_createsApprovalRequestWhenHeld() {
        FdsEvaluationService service = service(List.of());
        FdsScoreResult result = new FdsScoreResult(64, THRESHOLD, true,
            List.of(new TriggeredRule(1L, 64)));

        service.persist(100L, result);

        then(approvalRequestService).should().create(100L);
    }
}
