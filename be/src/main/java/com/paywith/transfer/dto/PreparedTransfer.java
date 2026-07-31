package com.paywith.transfer.dto;

import com.paywith.external.openbanking.dto.RealNameInquiryResponse;
import com.paywith.recipient.domain.Recipient;
import com.paywith.transaction.domain.Transaction;
import com.paywith.wallet.domain.Wallet;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PreparedTransfer {
    private final Wallet wallet;
    private final Recipient recipient;
    private final Transaction transaction;
    private final RealNameInquiryResponse inquiryResponse;
}