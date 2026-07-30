package com.paywith.transfer.service;

import com.paywith.transfer.dto.RecipientInquiryRequest;
import com.paywith.transfer.dto.RecipientInquiryResponse;
import com.paywith.transfer.dto.TransferRequest;
import com.paywith.transfer.dto.TransferResponse;

public interface TransferService {

    // 송금 수취인 확인
    RecipientInquiryResponse inquireRecipient(RecipientInquiryRequest request);

    // 송금
    TransferResponse transfer(Long userId, TransferRequest request);
}
