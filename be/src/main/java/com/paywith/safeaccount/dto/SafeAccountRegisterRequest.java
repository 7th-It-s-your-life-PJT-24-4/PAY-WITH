package com.paywith.safeaccount.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@ApiModel(description = "안전계좌 등록 요청 (피보호자)")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SafeAccountRegisterRequest {
    @ApiModelProperty(value = "완료된 송금 이력이 있는 수취인 ID", required = true, example = "10")
    private Long recipientId;

    @ApiModelProperty(value = "안전계좌 별칭. 50자를 초과하면 400", example = "용돈용")
    private String accountAlias;
}