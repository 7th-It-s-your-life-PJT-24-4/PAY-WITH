package com.paywith.transfer.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@ApiModel(description = "송금 상대 이력 목록")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class RecipientHistoryListResponse {
    @ApiModelProperty(value = "송금한 적 있는 수취인 목록")
    private List<RecipientHistoryItem> recipients;
}
