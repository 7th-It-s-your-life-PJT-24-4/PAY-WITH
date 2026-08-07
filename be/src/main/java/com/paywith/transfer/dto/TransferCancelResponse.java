package com.paywith.transfer.dto;

import com.paywith.transaction.domain.TransactionStatus;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@ApiModel(description = "피보호자 대기 중 송금 취소 결과")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferCancelResponse {

    @ApiModelProperty(value = "취소된 송금 거래 번호", example = "74")
    private Long transactionId;

    @ApiModelProperty(value = "송금 처리 상태", example = "CANCELED", allowableValues = "CANCELED")
    private TransactionStatus status;
}
