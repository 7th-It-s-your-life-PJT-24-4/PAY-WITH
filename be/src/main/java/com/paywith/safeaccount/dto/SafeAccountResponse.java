package com.paywith.safeaccount.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.paywith.safeaccount.domain.SafeAccountStatus;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@ApiModel(description = "안전계좌 등록 결과")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SafeAccountResponse {
    @ApiModelProperty(value = "안전계좌 ID(recipientId와 동일)", example = "10")
    private Long safeAccountId;

    @ApiModelProperty(value = "수취인 ID. 등록 응답에는 항상 포함되며, 목록 조회의 보호자 응답에서만 null", example = "10")
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

    @ApiModelProperty(value = "실명조회 검증 여부", example = "true")
    private Boolean isVerified;

    @ApiModelProperty(value = "안전계좌 상태", example = "ACTIVE", allowableValues = "ACTIVE")
    private SafeAccountStatus status;

    @ApiModelProperty(value = "등록 처리 시각")
    private LocalDateTime createdAt;

    // 컨트롤러가 201/200 분기에만 쓰는 내부 플래그라 응답 바디에는 노출하지 않는다(@JsonIgnore).
    @JsonIgnore
    private Boolean newlyRegistered;
}