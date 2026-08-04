package com.paywith.approval.dto;

import com.paywith.approval.domain.ApprovalRequestView;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;

/**
 * 피보호자가 보는 승인 대기 건 상세.
 *
 * <p>보호자용 {@link ApprovalRequestDetailResponse} 와 원본(ApprovalRequestView)은 같지만 세 가지를
 * 내보내지 않는다.
 *
 * <ul>
 *   <li>wardId/wardName — 본인 화면이라 호출자가 이미 아는 값이다.
 *       홈 목록의 {@code PendingApprovalItemResponse} 와 같은 이유다.
 *   <li>ruleHits/totalScore — 어떤 FDS 룰에 걸렸는지는 보호자가 판단하기 위한 근거다. 피보호자가
 *       보이스피싱으로 조종당하는 중일 수 있어, 판정 근거를 그대로 알려주면 회피 수단이 된다.
 * </ul>
 *
 * <p>대신 홈 목록에 없는 memo 를 담는다. 본인이 입력한 값이라 감출 이유가 없고, 상세 화면에서
 * "내가 무엇을 보내려 했는지" 확인하는 데 쓴다.
 */
@ApiModel(description = "피보호자가 보는 승인 대기 거래 상세")
@Getter
public class WardApprovalDetailResponse {

    @ApiModelProperty(value = "승인요청 ID", example = "1")
    private final Long approvalId;

    @ApiModelProperty(value = "대상 거래 ID", example = "88")
    private final Long transactionId;

    @ApiModelProperty(value = "거래 종류. 현재는 송금만 승인 대기가 생긴다",
        example = "TRANSFER_OUT", allowableValues = "TRANSFER_OUT")
    private final String type;

    @ApiModelProperty(value = "거래 금액(원)", example = "2000000")
    private final BigDecimal amount;

    @ApiModelProperty(value = "송금 메모", example = "생활비")
    private final String memo;

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

    @ApiModelProperty(value = "승인 만료 시각. 지나면 조회되지 않고 거래는 취소된다")
    private final LocalDateTime expiredAt;

    public WardApprovalDetailResponse(ApprovalRequestView view) {
        this.approvalId = view.getApprovalId();
        this.transactionId = view.getTransactionId();
        this.type = view.getType();
        this.amount = view.getAmount();
        this.memo = view.getMemo();
        this.holderName = view.getHolderName();
        this.bankName = view.getBankName();
        this.accountNo = view.getAccountNo();
        this.riskLevel = view.getRiskLevel();
        this.requestedAt = view.getRequestedAt();
        this.expiredAt = view.getExpiredAt();
    }
}
