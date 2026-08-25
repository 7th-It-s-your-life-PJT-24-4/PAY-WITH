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

    @ApiModelProperty(value = "최근 송금 시각. 오프셋 없는 ISO-8601(LocalDateTime)", example = "2026-08-21T15:30:00")
    private LocalDateTime lastSentAt;

    @ApiModelProperty(value = "누적 송금 횟수(1 이상 — 송금한 적 없는 수취인은 목록에서 제외)", example = "3")
    private Integer sendCount;

    @ApiModelProperty(value = "안전계좌로 등록되어 있는지 여부", example = "true")
    private Boolean isRegisteredSafe;

    @ApiModelProperty(value = "안전계좌로 등록되어 있으면 recipientId 와 같은 값(안전계좌 ID = 수취인 ID), 아니면 null", example = "10")
    private Long safeAccountId;

    @ApiModelProperty(value = "안전계좌 별칭. 등록 시 지정하지 않았으면 null. 안전계좌 해제 시 지우지 않으므로 "
        + "isRegisteredSafe=false 여도 값이 남아 있을 수 있다", example = "용돈용")
    private String accountAlias;

}
