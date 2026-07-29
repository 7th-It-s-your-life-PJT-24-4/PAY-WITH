package com.paywith.fds.service;

import com.paywith.approval.service.ApprovalRequestService;
import com.paywith.exception.BusinessException;
import com.paywith.fds.domain.DecidedBy;
import com.paywith.fds.domain.RiskEvaluation;
import com.paywith.fds.domain.RiskEvaluationDetail;
import com.paywith.fds.domain.RiskLevel;
import com.paywith.fds.domain.RiskRule;
import com.paywith.fds.dto.FdsDecision;
import com.paywith.fds.dto.FdsEvaluationRequest;
import com.paywith.fds.dto.TriggeredRule;
import com.paywith.fds.mapper.RiskEvaluationDetailMapper;
import com.paywith.fds.mapper.RiskEvaluationMapper;
import com.paywith.fds.mapper.TransactionRiskMapper;
import com.paywith.fds.service.prefilter.FdsPreFilter;
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FdsEvaluationServiceImpl implements FdsEvaluationService {

    private final RuleContextCollectorService ruleContextCollectorService;
    private final RiskRuleCache riskRuleCache;
    private final RiskGrader riskGrader;
    private final List<FdsPreFilter> preFilters;
    private final Map<String, RiskRuleEvaluator> evaluatorsByRuleCode;
    private final RiskEvaluationMapper riskEvaluationMapper;
    private final RiskEvaluationDetailMapper riskEvaluationDetailMapper;
    private final TransactionRiskMapper transactionRiskMapper;
    private final ApprovalRequestService approvalRequestService;

    /**
     * @param preFilters @Order 순으로 주입된다. 블랙리스트가 화이트리스트보다 앞서야 한다.
     */
    public FdsEvaluationServiceImpl(
        RuleContextCollectorService ruleContextCollectorService,
        RiskRuleCache riskRuleCache,
        RiskGrader riskGrader,
        List<FdsPreFilter> preFilters,
        List<RiskRuleEvaluator> evaluators,
        RiskEvaluationMapper riskEvaluationMapper,
        RiskEvaluationDetailMapper riskEvaluationDetailMapper,
        TransactionRiskMapper transactionRiskMapper,
        ApprovalRequestService approvalRequestService
    ) {
        this.ruleContextCollectorService = ruleContextCollectorService;
        this.riskRuleCache = riskRuleCache;
        this.riskGrader = riskGrader;
        this.preFilters = preFilters;
        this.evaluatorsByRuleCode = evaluators.stream()
            .collect(Collectors.toMap(RiskRuleEvaluator::getRuleCode, Function.identity()));
        this.riskEvaluationMapper = riskEvaluationMapper;
        this.riskEvaluationDetailMapper = riskEvaluationDetailMapper;
        this.transactionRiskMapper = transactionRiskMapper;
        this.approvalRequestService = approvalRequestService;
    }

    @Override
    public FdsDecision evaluate(FdsEvaluationRequest request) {
        return decide(ruleContextCollectorService.collect(request));
    }

    /**
     * 컨텍스트로부터 등급을 판정한다. 조회를 수행하지 않으므로 테스트에서 컨텍스트를 직접 넣어
     * 판정 로직만 검증할 수 있다.
     */
    public FdsDecision decide(RuleContext context) {
        return applyPreFilters(context).orElseGet(() -> scoreByRules(context));
    }

    /** 1단계: 단축평가. 첫 판정이 서는 즉시 확정하고 룰 순회를 건너뛴다. */
    private Optional<FdsDecision> applyPreFilters(RuleContext context) {
        for (FdsPreFilter preFilter : preFilters) {
            Optional<RiskLevel> level = preFilter.apply(context);
            if (level.isPresent()) {
                return Optional.of(shortCircuit(preFilter, level.get()));
            }
        }
        return Optional.empty();
    }

    private FdsDecision shortCircuit(FdsPreFilter preFilter, RiskLevel level) {
        // 단축평가 항목도 risk_rules 행이므로 발동 내역을 다른 룰과 동일하게 남긴다.
        // 점수 합산에는 참여하지 않으므로 총점은 0이다.
        List<TriggeredRule> triggered = riskRuleCache.findByCode(preFilter.getRuleCode())
            .map(rule -> Collections.singletonList(new TriggeredRule(rule.getRuleId(), 0)))
            .orElseGet(Collections::emptyList);

        return new FdsDecision(
            level,
            preFilter.getDecidedBy(),
            0,
            riskGrader.getCautionThreshold(),
            riskGrader.getDangerThreshold(),
            triggered
        );
    }

    /** 2~3단계: 룰 점수 합산 후 등급 판정. */
    private FdsDecision scoreByRules(RuleContext context) {
        List<TriggeredRule> triggeredRules = new ArrayList<>();
        int rawScore = 0;

        for (RiskRule rule : riskRuleCache.getActiveRules()) {
            RiskRuleEvaluator evaluator = evaluatorsByRuleCode.get(rule.getRuleCode());
            if (evaluator == null) {
                // 단축평가 전용 행(BL_*/WL_*)처럼 평가기가 없는 룰은 여기서 걸러진다.
                continue;
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

    @Override
    @Transactional
    public void saveDecision(Long transactionId, FdsDecision decision) {
        RiskEvaluation evaluation = new RiskEvaluation();
        evaluation.setTransactionId(transactionId);
        evaluation.setTotalScore(decision.getTotalScore());
        evaluation.setCautionThreshold(decision.getCautionThreshold());
        evaluation.setDangerThreshold(decision.getDangerThreshold());
        evaluation.setRiskLevel(decision.getRiskLevel());
        evaluation.setDecidedBy(decision.getDecidedBy());
        riskEvaluationMapper.insert(evaluation);

        for (TriggeredRule triggeredRule : decision.getTriggeredRules()) {
            RiskEvaluationDetail detail = new RiskEvaluationDetail();
            detail.setEvaluationId(evaluation.getEvaluationId());
            detail.setRuleId(triggeredRule.getRuleId());
            detail.setScore(triggeredRule.getScore());
            riskEvaluationDetailMapper.insert(detail);
        }

        // risk_score 는 total_score 의 비정규화 복사본이다. 같은 트랜잭션에서 갱신해야 어긋나지 않는다.
        // risk_evaluations 는 FK 로 걸려 있어 없는 거래면 터지지만 UPDATE 는 조용히 0행이 되므로 직접 확인한다.
        int updated = transactionRiskMapper.updateRiskScore(transactionId, decision.getTotalScore());
        if (updated != 1) {
            throw new BusinessException(HttpStatus.NOT_FOUND,
                "위험도를 반영할 거래를 찾을 수 없습니다. transactionId=" + transactionId);
        }

        if (decision.isHeld()) {
            approvalRequestService.create(transactionId);
        }
        // CAUTION 알림 발송은 아직 미구현이다. notifications 행 생성은 이 트랜잭션 안에서,
        // 실제 발송은 커밋 이후에 수행해야 롤백된 거래의 알림이 나가는 일이 없다.
    }
}
