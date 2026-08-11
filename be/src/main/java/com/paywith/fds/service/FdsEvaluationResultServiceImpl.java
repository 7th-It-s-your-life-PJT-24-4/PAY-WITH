package com.paywith.fds.service;

import com.paywith.approval.service.ApprovalRequestService;
import com.paywith.exception.BusinessException;
import com.paywith.fds.domain.RiskEvaluation;
import com.paywith.fds.domain.RiskEvaluationDetail;
import com.paywith.fds.dto.FdsDecision;
import com.paywith.fds.dto.TriggeredRule;
import com.paywith.fds.mapper.RiskEvaluationDetailMapper;
import com.paywith.fds.mapper.RiskEvaluationMapper;
import com.paywith.fds.mapper.TransactionRiskMapper;
import com.paywith.notification.service.TransferNotifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FdsEvaluationResultServiceImpl implements FdsEvaluationResultService {

    private final RiskEvaluationMapper riskEvaluationMapper;
    private final RiskEvaluationDetailMapper riskEvaluationDetailMapper;
    private final TransactionRiskMapper transactionRiskMapper;
    private final ApprovalRequestService approvalRequestService;
    private final TransferNotifier transferNotifier;

    public FdsEvaluationResultServiceImpl(
        RiskEvaluationMapper riskEvaluationMapper,
        RiskEvaluationDetailMapper riskEvaluationDetailMapper,
        TransactionRiskMapper transactionRiskMapper,
        ApprovalRequestService approvalRequestService,
        TransferNotifier transferNotifier
    ) {
        this.riskEvaluationMapper = riskEvaluationMapper;
        this.riskEvaluationDetailMapper = riskEvaluationDetailMapper;
        this.transactionRiskMapper = transactionRiskMapper;
        this.approvalRequestService = approvalRequestService;
        this.transferNotifier = transferNotifier;
    }

    @Override
    @Transactional
    public void save(Long transactionId, FdsDecision decision) {
        RiskEvaluation evaluation = new RiskEvaluation();
        evaluation.setTransactionId(transactionId);
        evaluation.setTotalScore(decision.getTotalScore());
        evaluation.setCautionThreshold(decision.getCautionThreshold());
        evaluation.setDangerThreshold(decision.getDangerThreshold());
        evaluation.setRiskLevel(decision.getRiskLevel());
        evaluation.setDecidedBy(decision.getDecidedBy());
        riskEvaluationMapper.insert(evaluation);

        // 위 insert 로 채워진 PK 가 필요해 순서를 바꿀 수 없다.
        for (TriggeredRule triggeredRule : decision.getTriggeredRules()) {
            RiskEvaluationDetail detail = new RiskEvaluationDetail();
            detail.setEvaluationId(evaluation.getEvaluationId());
            detail.setRuleId(triggeredRule.getRuleId());
            detail.setScore(triggeredRule.getScore());
            riskEvaluationDetailMapper.insert(detail);
        }

        // risk_score 는 total_score 의 비정규화 복사본이라 같은 트랜잭션에서 갱신한다.
        // UPDATE 는 대상이 없어도 조용히 0행이 되므로 직접 확인한다.
        int updated = transactionRiskMapper.updateRiskScore(transactionId, decision.getTotalScore());
        if (updated != 1) {
            throw new BusinessException(HttpStatus.NOT_FOUND,
                "위험도를 반영할 거래를 찾을 수 없습니다. transactionId=" + transactionId);
        }

        if (decision.isHeld()) {
            approvalRequestService.create(transactionId);
        }

        // 알림 행은 이 트랜잭션에서 남기고 발송은 커밋 이후에 나간다(NotificationServiceImpl).
        // 판정이 롤백되면 알림도 함께 사라져야 하기 때문이다.
        transferNotifier.notifyRiskDetected(transactionId, decision);
    }
}
