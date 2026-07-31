package com.paywith.transfer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecipientInquiryResponse {
    private String bankCode;
    private String bankName;
    private String accountNo;
    private String recipientName;
}