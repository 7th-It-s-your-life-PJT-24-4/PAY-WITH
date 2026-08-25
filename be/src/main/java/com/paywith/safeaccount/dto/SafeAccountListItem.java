package com.paywith.safeaccount.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@ApiModel(description = "안전계좌 목록의 한 건")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SafeAccountListItem {
    @ApiModelProperty(value = "안전계좌 ID. recipientId 와 같은 값(recipients 행의 recipient_id)", example = "10")
    private Long safeAccountId;

    @ApiModelProperty(value = "수취인 ID(safeAccountId 와 같은 값). 보호자 조회 응답에서는 노출되지 않는다(null)", example = "10")
    private Long recipientId;

    @ApiModelProperty(value = "은행 표준코드", example = "004")
    private String bankCode;

    @ApiModelProperty(value = "은행명", example = "KB국민은행")
    private String bankName;

    @ApiModelProperty(value = "계좌번호", example = "11012300006781")
    private String accountNo;

    @ApiModelProperty(value = "예금주명", example = "김수취")
    private String holderName;

    @ApiModelProperty(value = "안전계좌 별칭", example = "용돈용")
    private String accountAlias;

    @ApiModelProperty(value = "실명조회 검증 여부. 항상 true", example = "true")
    private Boolean isVerified;

    @ApiModelProperty(value = "안전계좌 등록 시각(safe_registered_at, DB DATETIME 이라 초 단위). 오프셋 없는 ISO 형식", example = "2026-07-21T14:30:00")
    private LocalDateTime createdAt;
}
