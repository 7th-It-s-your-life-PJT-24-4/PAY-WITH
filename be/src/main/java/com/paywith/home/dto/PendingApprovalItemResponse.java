package com.paywith.home.dto;

import com.paywith.approval.domain.ApprovalRequestView;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;

/**
 * 홈 화면의 승인 대기 카드 한 건.
 *
 * <p>보호자용 {@code ApprovalRequestSummaryResponse} 와 원본(ApprovalRequestView)은 같지만
 * wardId/wardName 을 내보내지 않는다. 본인 화면이라 호출자가 이미 아는 값이다.
 */
@ApiModel(description = "홈 화면의 승인 대기 거래 한 건")
@Getter
public class PendingApprovalItemResponse {

    @ApiModelProperty(value = "승인요청 ID. 상세 조회에 쓴다", example = "1")
    private final Long approvalId;

    @ApiModelProperty(value = "대상 거래 ID", example = "88")
    private final Long transactionId;

    @ApiModelProperty(value = "거래 종류. 현재는 송금만 승인 대기가 생긴다",
        example = "TRANSFER_OUT", allowableValues = "TRANSFER_OUT")
    private final String type;

    @ApiModelProperty(value = "거래 금액(원)", example = "30000")
    private final BigDecimal amount;

    @ApiModelProperty(value = "수취인 예금주명", example = "박지연")
    private final String holderName;

    @ApiModelProperty(value = "수취 은행", example = "신한은행")
    private final String bankName;

    @ApiModelProperty(value = "수취 계좌번호", example = "110-234-567890")
    private final String accountNo;

    @ApiModelProperty(value = "위험 등급. 평가 기록이 없으면 null",
        example = "DANGER", allowableValues = "SAFE,CAUTION,DANGER")
    private final String riskLevel;

    @ApiModelProperty(value = "승인요청 생성 시각")
    private final LocalDateTime requestedAt;

    @ApiModelProperty(value = "승인 만료 시각. 지나면 목록에서 빠지고 거래는 취소된다")
    private final LocalDateTime expiredAt;

    public PendingApprovalItemResponse(ApprovalRequestView view) {
        this.approvalId = view.getApprovalId();
        this.transactionId = view.getTransactionId();
        this.type = view.getType();
        this.amount = view.getAmount();
        this.holderName = view.getHolderName();
        this.bankName = view.getBankName();
        this.accountNo = view.getAccountNo();
        this.riskLevel = view.getRiskLevel();
        this.requestedAt = view.getRequestedAt();
        this.expiredAt = view.getExpiredAt();
    }
}
