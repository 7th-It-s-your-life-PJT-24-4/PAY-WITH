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
    @ApiModelProperty(value = "안전계좌 목록")
    private List<SafeAccountListItem> safeAccounts;
}
