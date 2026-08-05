package com.paywith.approval.dto;

import com.paywith.approval.domain.ApprovalRequestView;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import lombok.Getter;

/**
 * 종결된 승인요청 상세.
 *
 * <p>프런트가 처리 전 상세와 종결 결과를 sessionStorage 에 합쳐 보관하지 않아도 되도록,
 * DB에 남은 승인요청·거래 상태를 두 응답 객체로 다시 조립한다.
 */
@ApiModel(description = "승인·거절·피보호자 취소·자동 만료가 끝난 이상거래 상세")
@Getter
public class ApprovalHistoryResultResponse {

    @ApiModelProperty(value = "종결 전 이상거래 상세")
    private final ApprovalRequestDetailResponse detail;

    @ApiModelProperty(value = "종결 상태와 시각")
    private final ApprovalDecisionResponse decision;

    public ApprovalHistoryResultResponse(
        ApprovalRequestView view, List<ApprovalRuleHitResponse> ruleHits) {
        this.detail = new ApprovalRequestDetailResponse(view, ruleHits);
        this.decision = buildDecision(view);
    }

    private ApprovalDecisionResponse buildDecision(ApprovalRequestView view) {
        ApprovalDecisionResponse response = new ApprovalDecisionResponse(
            view.getApprovalId(),
            view.getTransactionId(),
            view.getStatus(),
            view.getRespondedAt());

        if (!"APPROVED".equals(view.getStatus())) {
            return response;
        }
        if ("COMPLETED".equals(view.getTransactionStatus())) {
            return response.withTransfer(
                TransferResultResponse.completed(view.getCompletedAt(), view.getBalanceAfter()));
        }
        if ("FAILED".equals(view.getTransactionStatus())) {
            // 실패 사유는 현재 거래 원장에 별도 저장하지 않으므로 상태만 복원한다.
            return response.withTransfer(TransferResultResponse.failed(null));
        }
        return response;
    }
}
