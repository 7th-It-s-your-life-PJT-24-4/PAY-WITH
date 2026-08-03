package com.paywith.transfer.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@ApiModel(description = "송금 상대 이력의 한 건")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class RecipientHistoryItem {
    @ApiModelProperty(value = "수취인 ID", example = "10")
    private Long recipientId;

    @ApiModelProperty(value = "예금주명", example = "김수취")
    private String holderName;

    @ApiModelProperty(value = "은행 표준코드", example = "004")
    private String bankCode;

    @ApiModelProperty(value = "은행명", example = "KB국민은행")
    private String bankName;

    @ApiModelProperty(value = "계좌번호", example = "11012300006781")
    private String accountNo;

    @ApiModelProperty(value = "최근 송금 시각")
    private LocalDateTime lastSentAt;

    @ApiModelProperty(value = "누적 송금 횟수", example = "3")
    private Integer sendCount;

    @ApiModelProperty(value = "안전계좌로 등록되어 있는지 여부", example = "true")
    private Boolean isRegisteredSafe;

    @ApiModelProperty(value = "안전계좌로 등록되어 있으면 그 안전계좌 ID, 아니면 null", example = "10")
    private Long safeAccountId;

    @ApiModelProperty(value = "안전계좌 별칭. 등록되어 있지 않으면 null", example = "용돈용")
    private String accountAlias;

}
