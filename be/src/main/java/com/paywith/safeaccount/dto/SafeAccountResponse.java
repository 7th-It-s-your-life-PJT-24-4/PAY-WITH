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
    @ApiModelProperty(value = "안전계좌 ID. recipientId 와 항상 같은 값(recipients 행의 recipient_id)", example = "10")
    private Long safeAccountId;

    @ApiModelProperty(value = "수취인 ID. safeAccountId 와 항상 같은 값이며 등록 응답에는 항상 포함된다", example = "10")
    private Long recipientId;

    @ApiModelProperty(value = "은행 표준코드", example = "004")
    private String bankCode;

    @ApiModelProperty(value = "은행명", example = "KB국민은행")
    private String bankName;

    @ApiModelProperty(value = "계좌번호", example = "11012300006781")
    private String accountNo;

    @ApiModelProperty(value = "예금주명. 보호자가 새 계좌를 등록한 경우 현재 실명조회 목이 계좌번호로 합성한 이름", example = "김수취")
    private String holderName;

    @ApiModelProperty(value = "안전계좌 별칭. 요청값이 null·공백만이면 null", example = "용돈용")
    private String accountAlias;

    @ApiModelProperty(value = "실명조회 검증 여부. 항상 true", example = "true")
    private Boolean isVerified;

    @ApiModelProperty(value = "안전계좌 상태. 항상 ACTIVE", example = "ACTIVE", allowableValues = "ACTIVE")
    private SafeAccountStatus status;

    @ApiModelProperty(value = "등록 처리 시각(서버 LocalDateTime.now()). 오프셋 없는 ISO 형식이며 마이크로초가 붙을 수 있다", example = "2026-07-21T14:30:00.123456")
    private LocalDateTime createdAt;

    // 컨트롤러가 201/200 분기에만 쓰는 내부 플래그라 응답 바디에는 노출하지 않는다(@JsonIgnore).
    @JsonIgnore
    private Boolean newlyRegistered;
}