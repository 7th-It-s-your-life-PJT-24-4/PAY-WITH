package com.paywith.safeaccount.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@ApiModel(description = "안전계좌 목록")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SafeAccountListResponse {
    @ApiModelProperty(value = "ACTIVE 안전계좌 목록(등록 시각 내림차순). 없으면 빈 배열")
    private List<SafeAccountListItem> safeAccounts;
}
