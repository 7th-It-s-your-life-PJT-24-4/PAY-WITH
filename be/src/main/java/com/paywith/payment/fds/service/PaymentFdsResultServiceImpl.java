package com.paywith.payment.fds.service;

import com.paywith.fds.domain.RiskEvaluation;
import com.paywith.fds.domain.RiskEvaluationDetail;
import com.paywith.fds.dto.FdsDecision;
import com.paywith.fds.dto.TriggeredRule;
import com.paywith.fds.mapper.RiskEvaluationDetailMapper;
import com.paywith.fds.mapper.RiskEvaluationMapper;
import com.paywith.fds.mapper.TransactionRiskMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 판정 결과를 송금과 같은 테이블(risk_evaluations + details + transactions.risk_score)에
 * 기록한다 — 근거 조회 경로가 송금과 같아져 보호자 화면·운영 조회를 재사용할 수 있다.
 * 매퍼 3종은 송금 빈을 그대로 주입받는다(상태 없는 공유 컴포넌트, §3-4).
 */
@Service
public class PaymentFdsResultServiceImpl implements PaymentFdsResultService {

    private static final Logger log = LoggerFactory.getLogger(PaymentFdsResultServiceImpl.class);

    private final RiskEvaluationMapper riskEvaluationMapper;
    private final RiskEvaluationDetailMapper riskEvaluationDetailMapper;
    private final TransactionRiskMapper transactionRiskMapper;

    public PaymentFdsResultServiceImpl(
        RiskEvaluationMapper riskEvaluationMapper,
        RiskEvaluationDetailMapper riskEvaluationDetailMapper,
        TransactionRiskMapper transactionRiskMapper
    ) {
        this.riskEvaluationMapper = riskEvaluationMapper;
        this.riskEvaluationDetailMapper = riskEvaluationDetailMapper;
        this.transactionRiskMapper = transactionRiskMapper;
    }

    /**
     * 저장 실패는 결제를 뒤집지 않는다(D4) — 로그만 남긴다. 호출자가 아니라 여기서 잡는 이유:
     * 예외가 @Transactional 경계를 넘는 순간 진행 중인 결제 트랜잭션이 rollback-only 로 표시되어,
     * 호출자가 잡아도 커밋 시점에 결제 전체가 뒤집히기 때문이다.
     */
    @Override
    @Transactional
    public void save(Long transactionId, FdsDecision decision) {
        try {
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
            int updated = transactionRiskMapper.updateRiskScore(transactionId, decision.getTotalScore());
            if (updated != 1) {
                log.error("위험도를 반영할 거래 행이 없다 — 호출 순서 버그 신호. transactionId={}", transactionId);
            }
        } catch (RuntimeException e) {
            log.error("결제 FDS 결과 저장 실패 — 결제는 유지(D4). transactionId={}", transactionId, e);
        }
    }
}
