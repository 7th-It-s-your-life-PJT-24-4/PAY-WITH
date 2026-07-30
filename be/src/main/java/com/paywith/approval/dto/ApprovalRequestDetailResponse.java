package com.paywith.approval.dto;

import com.paywith.approval.domain.ApprovalRequestView;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;

@ApiModel(description = "승인 화면 상세. 보호자가 판단할 수 있도록 보류 사유를 함께 담는다")
@Getter
public class ApprovalRequestDetailResponse {

    @ApiModelProperty(value = "승인요청 ID", example = "1")
    private final Long approvalId;

    @ApiModelProperty(value = "송금을 요청한 시니어 이름", example = "김시니어")
    private final String seniorName;

    @ApiModelProperty(value = "송금 금액(원)", example = "2000000")
    private final BigDecimal amount;

    @ApiModelProperty(value = "송금 메모. 위험 키워드 판정 대상", example = "검찰 수사 협조 요청")
    private final String memo;

    @ApiModelProperty(value = "수취인 예금주명", example = "박수취")
    private final String recipientName;

    @ApiModelProperty(value = "수취 은행", example = "신한은행")
    private final String bankName;

    @ApiModelProperty(value = "수취 계좌번호", example = "110234567890")
    private final String accountNo;

    @ApiModelProperty(value = "위험 등급", example = "DANGER", allowableValues = "SAFE,CAUTION,DANGER")
    private final String riskLevel;

    @ApiModelProperty(value = "룰 합산 점수. 블랙리스트 확정 건은 0", example = "64")
    private final Integer totalScore;

    @ApiModelProperty(value = "승인요청 생성 시각")
    private final LocalDateTime requestedAt;

    @ApiModelProperty(value = "승인 만료 시각")
    private final LocalDateTime expiredAt;

    @ApiModelProperty(value = "보류 사유. 점수가 큰 순")
    private final List<ApprovalRuleHitResponse> ruleHits;

    public ApprovalRequestDetailResponse(ApprovalRequestView view, List<ApprovalRuleHitResponse> ruleHits) {
        this.approvalId = view.getApprovalId();
        this.seniorName = view.getSeniorName();
        this.amount = view.getAmount();
        this.memo = view.getMemo();
        this.recipientName = view.getRecipientName();
        this.bankName = view.getBankName();
        this.accountNo = view.getAccountNo();
        this.riskLevel = view.getRiskLevel();
        this.totalScore = view.getTotalScore();
        this.requestedAt = view.getRequestedAt();
        this.expiredAt = view.getExpiredAt();
        this.ruleHits = ruleHits;
    }
}
