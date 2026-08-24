package com.paywith.transfer.dto;

import com.paywith.transaction.domain.TransactionStatus;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@ApiModel(description = "송금 결과")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferResponse {
    @ApiModelProperty(value = "송금 거래 ID", example = "999")
    private Long transactionId;

    @ApiModelProperty(
        value = "거래 상태. 블랙리스트 차단이면 BLOCKED(200), 그 밖의 위험 판정이면 HELD(202), "
            + "정상이면 COMPLETED(201). BLOCKED·HELD 는 이체가 일어나지 않아 이 필드와 "
            + "transactionId 만 채워지고 나머지는 null 이다.",
        example = "COMPLETED", allowableValues = "BLOCKED,HELD,COMPLETED")
    private TransactionStatus status;

    @ApiModelProperty(value = "수취인 예금주명", example = "김시니어")
    private String holderName;      // recipientName → holderName (DB: recipients.holder_name)

    @ApiModelProperty(value = "수취 은행 표준코드", example = "004")
    private String bankCode;

    @ApiModelProperty(value = "수취 은행명", example = "KB국민은행")
    private String bankName;

    @ApiModelProperty(value = "수취 계좌번호", example = "11012300006781")
    private String accountNo;

    @ApiModelProperty(value = "송금 금액(원)", example = "50000")
    private Long amount;

    @ApiModelProperty(value = "송금 메모", example = "생활비")
    private String memo;

    @ApiModelProperty(value = "송금 완료 시각. COMPLETED에서만 채워지고 HELD·BLOCKED에서는 null")
    private LocalDateTime completedAt;

    @ApiModelProperty(value = "송금 후 지갑 잔액(원)", example = "50000")
    private Long balanceAfter;
}