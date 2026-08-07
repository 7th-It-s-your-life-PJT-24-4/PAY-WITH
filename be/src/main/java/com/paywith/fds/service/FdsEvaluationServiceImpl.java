package com.paywith.fds.service;

import com.paywith.fds.domain.DecidedBy;
import com.paywith.fds.domain.RiskLevel;
import com.paywith.fds.domain.RiskRule;
import com.paywith.fds.dto.FdsDecision;
import com.paywith.fds.dto.FdsEvaluationRequest;
import com.paywith.fds.dto.TriggeredRule;
import com.paywith.fds.service.rule.RiskRuleCache;
import com.paywith.fds.service.rule.RiskRuleEvaluator;
import com.paywith.fds.service.rule.RuleContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class FdsEvaluationServiceImpl implements FdsEvaluationService {

    private static final String RULE_REJECTED_RECIPIENT = "BL_REJECTED_RECIPIENT";
    private static final String RULE_FRAUD_ACCOUNT = "BL_FRAUD_ACCOUNT";

    private final RuleContextCollectorService ruleContextCollectorService;
    private final FdsEvaluationResultService fdsEvaluationResultService;
    private final RiskRuleCache riskRuleCache;
    private final RiskGrader riskGrader;
    private final Map<String, RiskRuleEvaluator> evaluatorsByRuleCode;

    public FdsEvaluationServiceImpl(
        RuleContextCollectorService ruleContextCollectorService,
        FdsEvaluationResultService fdsEvaluationResultService,
        RiskRuleCache riskRuleCache,
        RiskGrader riskGrader,
        List<RiskRuleEvaluator> evaluators
    ) {
        this.ruleContextCollectorService = ruleContextCollectorService;
        this.fdsEvaluationResultService = fdsEvaluationResultService;
        this.riskRuleCache = riskRuleCache;
        this.riskGrader = riskGrader;
        this.evaluatorsByRuleCode = evaluators.stream()
            .collect(Collectors.toMap(RiskRuleEvaluator::getRuleCode, Function.identity()));
    }

    /** 수집·판정은 트랜잭션 밖, 저장만 별도 빈의 트랜잭션 안에서 일어난다. */
    @Override
    public FdsDecision evaluate(FdsEvaluationRequest request) {
        RuleContext context = ruleContextCollectorService.collect(request);
        FdsDecision decision = decide(context);
        fdsEvaluationResultService.save(request.getTransactionId(), decision);
        return decision;
    }

    /** 조회도 저장도 하지 않아 컨텍스트만 넣으면 판정 로직을 그대로 검증할 수 있다. */
    public FdsDecision decide(RuleContext context) {
        return evaluateBlacklist(context).orElseGet(() -> scoreByRules(context));
    }

    /**
     * 확정적으로 위험한 경우를 먼저 걸러 룰 점수 계산을 건너뛴다.
     *
     * <p>어느 항목이 먼저 걸리든 결과는 DANGER 라 순서는 판정에 영향을 주지 않는다.
     */
    private Optional<FdsDecision> evaluateBlacklist(RuleContext context) {
        if (context.isRecipientRejectedBefore()) {
            return blacklisted(RULE_REJECTED_RECIPIENT);
        }
        if (context.isRecipientReportedAsFraud()) {
            return blacklisted(RULE_FRAUD_ACCOUNT);
        }
        return Optional.empty();
    }

    /**
     * 블랙리스트 항목도 risk_rules 행이라 발동 내역과 배점을 그대로 쓴다. 룰 합산을 거치지 않을 뿐
     * 총점은 카탈로그 배점(만점)이라, 조회 화면에서 DANGER 인데 0점으로 보이지 않는다.
     * 활성 행이 없으면(is_active=FALSE 이거나 시드 누락) 차단하지 않는다.
     */
    private Optional<FdsDecision> blacklisted(String ruleCode) {
        return riskRuleCache.findByCode(ruleCode)
            .map(rule -> {
                // 룰 합산과 같은 척도(0~100)로 맞춘다. 시드가 잘못돼도 범위 밖 총점은 남지 않는다.
                int score = riskGrader.normalizeScore(rule.getScore());
                return new FdsDecision(
                    RiskLevel.DANGER,
                    DecidedBy.BLACKLIST,
                    score,
                    riskGrader.getCautionThreshold(),
                    riskGrader.getDangerThreshold(),
                    Collections.singletonList(new TriggeredRule(rule.getRuleId(), score))
                );
            });
    }

    private FdsDecision scoreByRules(RuleContext context) {
        List<TriggeredRule> triggeredRules = new ArrayList<>();
        int rawScore = 0;

        for (RiskRule rule : riskRuleCache.getActiveRules()) {
            RiskRuleEvaluator evaluator = evaluatorsByRuleCode.get(rule.getRuleCode());
            if (evaluator == null) {
                continue;   // 블랙리스트 전용 행(BL_*)은 평가기가 없다
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
}
