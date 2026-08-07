package com.paywith.safeaccount.dto;

import com.paywith.safeaccount.domain.SafeAccountStatus;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@ApiModel(description = "안전계좌 삭제(비활성화) 결과")
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SafeAccountDeleteResponse {
    @ApiModelProperty(value = "안전계좌 ID", example = "10")
    private Long safeAccountId;

    @ApiModelProperty(value = "안전계좌 상태. 항상 INACTIVE", example = "INACTIVE", allowableValues = "INACTIVE")
    private SafeAccountStatus status;
}
