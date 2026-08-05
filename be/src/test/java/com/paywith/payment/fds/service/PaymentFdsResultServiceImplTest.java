package com.paywith.payment.fds.service;

import static com.paywith.payment.fds.support.PaymentRuleContexts.BUSAN_LAT;
import static com.paywith.payment.fds.support.PaymentRuleContexts.BUSAN_LNG;
import static com.paywith.payment.fds.support.PaymentRuleContexts.at;
import static com.paywith.payment.fds.support.PaymentRuleContexts.lastPayment;
import static com.paywith.payment.fds.support.PaymentRuleContexts.normal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;

import com.paywith.fds.domain.DecidedBy;
import com.paywith.fds.domain.RiskEvaluation;
import com.paywith.fds.domain.RiskEvaluationDetail;
import com.paywith.fds.domain.RiskLevel;
import com.paywith.fds.domain.RiskRule;
import com.paywith.fds.dto.FdsDecision;
import com.paywith.fds.mapper.RiskEvaluationDetailMapper;
import com.paywith.fds.mapper.RiskEvaluationMapper;
import com.paywith.fds.mapper.TransactionRiskMapper;
import com.paywith.fds.service.rule.RiskRuleCache;
import com.paywith.payment.fds.support.PaymentFdsTestWiring;
import com.paywith.payment.fds.support.PaymentRiskRules;
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
@DisplayName("결제 FDS 판정 결과 저장")
class PaymentFdsResultServiceImplTest {

    private static final Long TRANSACTION_ID = 200L;

    @Mock
    private RiskRuleCache riskRuleCache;
    @Mock
    private RiskEvaluationMapper riskEvaluationMapper;
    @Mock
    private RiskEvaluationDetailMapper riskEvaluationDetailMapper;
    @Mock
    private TransactionRiskMapper transactionRiskMapper;

    private PaymentFdsResultServiceImpl service;

    /** 저장할 FdsDecision 을 실제 판정으로 만들기 위한 것. */
    private PaymentFdsEvaluationServiceImpl decider;

    @BeforeEach
    void setUp() {
        List<RiskRule> activeRules = PaymentRiskRules.activeRules();
        lenient().when(riskRuleCache.getActiveRules()).thenReturn(activeRules);
        for (RiskRule rule : activeRules) {
            lenient().when(riskRuleCache.findByCode(rule.getRuleCode()))
                .thenReturn(Optional.of(rule));
        }
        lenient().when(transactionRiskMapper.updateRiskScore(anyLong(), anyInt())).thenReturn(1);

        service = new PaymentFdsResultServiceImpl(
            riskEvaluationMapper,
            riskEvaluationDetailMapper,
            transactionRiskMapper
        );
        decider = new PaymentFdsEvaluationServiceImpl(
            null,
            PaymentFdsTestWiring.travelEvaluator(),
            riskRuleCache,
            PaymentFdsTestWiring.grader(),
            PaymentFdsTestWiring.evaluators()
        );
    }

    /** 위험업종 단독(25) → CAUTION */
    private FdsDecision cautionDecision() {
        return decider.decide(normal().merchantCategoryCode("JEWELRY").amount(90_000L).build());
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
        assertThat(saved.getCautionThreshold()).isEqualTo(PaymentFdsTestWiring.CAUTION_THRESHOLD);
        assertThat(saved.getDangerThreshold()).isEqualTo(PaymentFdsTestWiring.DANGER_THRESHOLD);
    }

    @Test
    void save_savesOneDetailPerTriggeredRule() {
        // 위험업종(25) + 고액 L3(35) = 발동 2건
        FdsDecision decision =
            decider.decide(normal().merchantCategoryCode("JEWELRY").amount(500_000L).build());

        service.save(TRANSACTION_ID, decision);

        then(riskEvaluationDetailMapper).should(org.mockito.Mockito.times(2))
            .insert(any(RiskEvaluationDetail.class));
    }

    @Test
    void save_updatesDenormalizedRiskScore() {
        service.save(TRANSACTION_ID, cautionDecision());

        then(transactionRiskMapper).should().updateRiskScore(TRANSACTION_ID, 25);
    }

    /** 단축평가 확정 건도 근거는 details 에 남는다 — 점수는 0. */
    @Test
    void save_savesShortcutRuleAsDetailWithZeroScore() {
        FdsDecision decision = decider.decide(normal()
            .lastCompletedPayment(lastPayment(BUSAN_LAT, BUSAN_LNG, at(14).minusMinutes(5)))
            .build());
        assertThat(decision.getDecidedBy()).isEqualTo(DecidedBy.BLACKLIST);

        service.save(TRANSACTION_ID, decision);

        ArgumentCaptor<RiskEvaluationDetail> captor =
            ArgumentCaptor.forClass(RiskEvaluationDetail.class);
        then(riskEvaluationDetailMapper).should().insert(captor.capture());
        assertThat(captor.getValue().getScore()).isZero();
        then(transactionRiskMapper).should().updateRiskScore(TRANSACTION_ID, 0);
    }

    /**
     * 송금과 달리 거래 행 부재에도 예외를 던지지 않는다(D4) — 저장 실패가
     * @Transactional 경계를 넘어 결제 트랜잭션을 뒤집으면 안 되기 때문. 로그로만 남긴다.
     */
    @Test
    void save_doesNotThrowWhenTransactionRowIsMissing() {
        given(transactionRiskMapper.updateRiskScore(anyLong(), anyInt())).willReturn(0);

        assertThatCode(() -> service.save(TRANSACTION_ID, cautionDecision()))
            .doesNotThrowAnyException();
    }

    /** 저장 도중 어떤 예외가 나도 밖으로 새지 않는다 — 결제는 유지된다(D4). */
    @Test
    void save_swallowsMapperFailure() {
        willThrow(new RuntimeException("INSERT 실패"))
            .given(riskEvaluationMapper).insert(any(RiskEvaluation.class));

        assertThatCode(() -> service.save(TRANSACTION_ID, cautionDecision()))
            .doesNotThrowAnyException();
        then(transactionRiskMapper).should(never()).updateRiskScore(anyLong(), anyInt());
    }
}
