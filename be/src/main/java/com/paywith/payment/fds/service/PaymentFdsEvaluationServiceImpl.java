package com.paywith.payment.fds.service;

import com.paywith.fds.domain.DecidedBy;
import com.paywith.fds.domain.RiskLevel;
import com.paywith.fds.domain.RiskRule;
import com.paywith.fds.dto.FdsDecision;
import com.paywith.fds.dto.TriggeredRule;
import com.paywith.fds.service.RiskGrader;
import com.paywith.fds.service.rule.RiskRuleCache;
import com.paywith.merchant.domain.Merchant;
import com.paywith.payment.fds.rule.PaymentRiskRuleEvaluator;
import com.paywith.payment.fds.rule.PaymentRuleContext;
import com.paywith.payment.fds.rule.impl.PaymentImpossibleTravelEvaluator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 송금 {@code FdsEvaluationServiceImpl}과 같은 파이프라인(단축평가 → 점수 합산 → 등급 환산)을
 * 결제 전용 평가기로 돌린다. RiskGrader·RiskRuleCache 는 송금과 같은 빈을 공유하고, 룰 순회는
 * 자기 평가기 맵에 있는 코드만 집으므로 카탈로그에 송금 룰이 섞여 있어도 무해하다(대칭 구조).
 */
@Service
public class PaymentFdsEvaluationServiceImpl implements PaymentFdsEvaluationService {

    private static final Logger log = LoggerFactory.getLogger(PaymentFdsEvaluationServiceImpl.class);

    private final PaymentRuleContextCollector paymentRuleContextCollector;
    private final PaymentImpossibleTravelEvaluator impossibleTravelEvaluator;
    private final RiskRuleCache riskRuleCache;
    private final RiskGrader riskGrader;
    private final Map<String, PaymentRiskRuleEvaluator> evaluatorsByRuleCode;

    public PaymentFdsEvaluationServiceImpl(
        PaymentRuleContextCollector paymentRuleContextCollector,
        PaymentImpossibleTravelEvaluator impossibleTravelEvaluator,
        RiskRuleCache riskRuleCache,
        RiskGrader riskGrader,
        List<PaymentRiskRuleEvaluator> evaluators
    ) {
        this.paymentRuleContextCollector = paymentRuleContextCollector;
        this.impossibleTravelEvaluator = impossibleTravelEvaluator;
        this.riskRuleCache = riskRuleCache;
        this.riskGrader = riskGrader;
        this.evaluatorsByRuleCode = evaluators.stream()
            .collect(Collectors.toMap(PaymentRiskRuleEvaluator::getRuleCode, Function.identity()));
    }

    /**
     * 내부 오류는 fail-open(D4) — error 로그 후 SAFE 취급으로 결제를 정상 진행시킨다.
     * 부가 방어층의 장애가 본질 기능(대면·동기 결제)을 중단시키지 않게 하는 확정 결정이며,
     * 적용 범위는 판정 불능에 한정한다(정상 판정된 DANGER 의 차단 경로는 별개).
     */
    @Override
    public FdsDecision evaluate(Long walletId, Long amount, Merchant merchant) {
        try {
            PaymentRuleContext context = paymentRuleContextCollector.collect(walletId, amount, merchant);
            return decide(context);
        } catch (RuntimeException e) {
            log.error("결제 FDS 판정 불능 — fail-open 으로 SAFE 처리. walletId={}, merchantId={}",
                walletId, merchant == null ? null : merchant.getMerchantId(), e);
            return safeFallback();
        }
    }

    /** 조회도 저장도 하지 않아 컨텍스트만 넣으면 판정 로직을 그대로 검증할 수 있다. */
    public FdsDecision decide(PaymentRuleContext context) {
        return evaluateShortcut(context).orElseGet(() -> scoreByRules(context));
    }

    /**
     * 확정적으로 위험한 경우(불가능한 이동)를 먼저 걸러 룰 점수 계산을 건너뛴다.
     * 활성 행이 없으면(is_active=FALSE 이거나 시드 누락) 차단하지 않는다 — 송금 블랙리스트와 동일.
     */
    private Optional<FdsDecision> evaluateShortcut(PaymentRuleContext context) {
        if (!impossibleTravelEvaluator.evaluate(context)) {
            return Optional.empty();
        }
        return riskRuleCache.findByCode(PaymentImpossibleTravelEvaluator.RULE_CODE)
            .map(rule -> new FdsDecision(
                RiskLevel.DANGER,
                DecidedBy.BLACKLIST,
                0,
                riskGrader.getCautionThreshold(),
                riskGrader.getDangerThreshold(),
                Collections.singletonList(new TriggeredRule(rule.getRuleId(), 0))
            ));
    }

    private FdsDecision scoreByRules(PaymentRuleContext context) {
        List<TriggeredRule> triggeredRules = new ArrayList<>();
        int rawScore = 0;

        for (RiskRule rule : riskRuleCache.getActiveRules()) {
            PaymentRiskRuleEvaluator evaluator = evaluatorsByRuleCode.get(rule.getRuleCode());
            if (evaluator == null) {
                continue;   // 송금 룰·단축평가 행은 결제 평가기가 없다
            }
            if (evaluator.evaluate(context)) {
                triggeredRules.add(new TriggeredRule(rule.getRuleId(), rule.getScore()));
                rawScore += rule.getScore();
            }
        }

        int totalScore = riskGrader.normalizeScore(rawScore);
        return new FdsDecision(
            riskGrader.grade(totalScore),
            DecidedBy.RULE,
            totalScore,
            riskGrader.getCautionThreshold(),
            riskGrader.getDangerThreshold(),
            triggeredRules
        );
    }

    /** 발동 룰 없는 SAFE — 진짜 무발동 SAFE 와 같은 모양이며, 판정 불능 사실은 로그로만 남는다. */
    private FdsDecision safeFallback() {
        return new FdsDecision(
            RiskLevel.SAFE,
            DecidedBy.RULE,
            0,
            riskGrader.getCautionThreshold(),
            riskGrader.getDangerThreshold(),
            Collections.emptyList()
        );
    }
}
