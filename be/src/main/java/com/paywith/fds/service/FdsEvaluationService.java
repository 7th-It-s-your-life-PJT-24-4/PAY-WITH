package com.paywith.fds.service;

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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 송금 FDS 평가 진입점.
 *
 * <p>송금 API 쪽 사용 계약:
 * <ol>
 *   <li>수취인을 먼저 확보한다(recipients 조회, 없으면 insert). FDS는 recipientId로 위험 정보를
 *       조회하므로 등록되지 않은 수취인으로 호출하면 404가 발생한다. 신규 여부는 행 존재가 아니라
 *       send_count=0으로 판정하므로 방금 등록한 수취인도 정상 평가된다.</li>
 *   <li>transaction insert 전에 {@link #evaluate(FdsEvaluationRequest)}로 점수를 받는다.</li>
 *   <li>결과의 totalScore/held 값으로 transaction의 risk_score와 상태(HELD/PROCESSING)를 결정해 저장한다.</li>
 *   <li>transaction insert 후 {@link #persist(Long, FdsScoreResult)}를 같은 트랜잭션 안에서 호출한다.
 *       보류(HELD) 판정이면 보호자 승인요청(approval_requests) 생성까지 이 안에서 처리된다.</li>
 * </ol>
 * 이체 이력·수취인 위험 정보 조회는 FDS가 직접 수행하므로 호출 측은 룰 내부 기준(시간창 등)을 몰라도 된다.
 */
@Service
public class FdsEvaluationService {

    private final RiskRuleCache riskRuleCache;
    private final Map<String, RiskRuleEvaluator> evaluatorsByRuleCode;
    private final RiskEvaluationMapper riskEvaluationMapper;
    private final RiskEvaluationDetailMapper riskEvaluationDetailMapper;
    private final FdsHistoryMapper fdsHistoryMapper;
    private final ApprovalRequestService approvalRequestService;
    private final int threshold;
    private final int repeatedWindowMinutes;
    private final int divisionWindowMinutes;
    private final List<String> memoKeywords;

    public FdsEvaluationService(
        RiskRuleCache riskRuleCache,
        List<RiskRuleEvaluator> evaluators,
        RiskEvaluationMapper riskEvaluationMapper,
        RiskEvaluationDetailMapper riskEvaluationDetailMapper,
        FdsHistoryMapper fdsHistoryMapper,
        ApprovalRequestService approvalRequestService,
        @Value("${fds.threshold}") int threshold,
        @Value("${fds.repeated.window-minutes}") int repeatedWindowMinutes,
        @Value("${fds.division.window-minutes}") int divisionWindowMinutes,
        @Value("${fds.memo-keywords}") String[] memoKeywords
    ) {
        this.riskRuleCache = riskRuleCache;
        this.evaluatorsByRuleCode = evaluators.stream()
            .collect(Collectors.toMap(RiskRuleEvaluator::getRuleCode, Function.identity()));
        this.riskEvaluationMapper = riskEvaluationMapper;
        this.riskEvaluationDetailMapper = riskEvaluationDetailMapper;
        this.fdsHistoryMapper = fdsHistoryMapper;
        this.approvalRequestService = approvalRequestService;
        this.threshold = threshold;
        this.repeatedWindowMinutes = repeatedWindowMinutes;
        this.divisionWindowMinutes = divisionWindowMinutes;
        this.memoKeywords = Arrays.asList(memoKeywords);
    }

    public FdsScoreResult evaluate(FdsEvaluationRequest request) {
        return score(buildContext(request));
    }

    public FdsScoreResult score(RuleContext context) {
        List<TriggeredRule> triggeredRules = new ArrayList<>();
        int totalScore = 0;

        for (RiskRule rule : riskRuleCache.getActiveRules()) {
            RiskRuleEvaluator evaluator = evaluatorsByRuleCode.get(rule.getRuleCode());
            if (evaluator == null) {
                continue;
            }
            if (evaluator.evaluate(context)) {
                triggeredRules.add(new TriggeredRule(rule.getRuleId(), rule.getScore()));
                totalScore += rule.getScore();
            }
        }

        boolean held = totalScore >= threshold;
        return new FdsScoreResult(totalScore, threshold, held, triggeredRules);
    }

    @Transactional
    public void persist(Long transactionId, FdsScoreResult result) {
        RiskEvaluation evaluation = new RiskEvaluation();
        evaluation.setTransactionId(transactionId);
        evaluation.setTotalScore(result.getTotalScore());
        evaluation.setThreshold(result.getThreshold());
        evaluation.setHeld(result.isHeld());
        riskEvaluationMapper.insert(evaluation);

        for (TriggeredRule triggeredRule : result.getTriggeredRules()) {
            RiskEvaluationDetail detail = new RiskEvaluationDetail();
            detail.setEvaluationId(evaluation.getEvaluationId());
            detail.setRuleId(triggeredRule.getRuleId());
            detail.setScore(triggeredRule.getScore());
            riskEvaluationDetailMapper.insert(detail);
        }

        // 보류 판정 시 보호자 승인요청 생성까지 FDS 후처리로 묶는다.
        if (result.isHeld()) {
            approvalRequestService.create(transactionId);
        }
    }

    private RuleContext buildContext(FdsEvaluationRequest request) {
        RecipientRiskInfo recipientRiskInfo = fdsHistoryMapper.findRecipientRiskInfo(request.getRecipientId());
        if (recipientRiskInfo == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "수취인 정보를 찾을 수 없습니다.");
        }

        LocalDateTime now = LocalDateTime.now();
        int recentTransferCount = fdsHistoryMapper.countRecentTransfers(
            request.getWalletId(),
            now.minusMinutes(repeatedWindowMinutes)
        );
        int recentDistinctRecipientCount = fdsHistoryMapper.countRecentDistinctRecipients(
            request.getWalletId(),
            now.minusMinutes(divisionWindowMinutes)
        );

        return new RuleContext(
            request.getAmount(),
            request.getMemo(),
            now,
            recipientRiskInfo.getSendCount(),
            recipientRiskInfo.isRegisteredSafe(),
            recentTransferCount,
            recentDistinctRecipientCount,
            memoKeywords
        );
    }
}
