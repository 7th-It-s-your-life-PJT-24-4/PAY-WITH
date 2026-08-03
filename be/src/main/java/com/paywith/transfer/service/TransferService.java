package com.paywith.transfer.service;

import com.paywith.transfer.dto.*;

public interface TransferService {

    // 송금 수취인 확인
    RecipientInquiryResponse inquireRecipient(Long userId, RecipientInquiryRequest request);

    // 송금
    TransferResponse transfer(Long userId, String idempotencyKey, TransferRequest request);

    // 수취인 목록 조회(최근 거래 계좌)
    RecipientHistoryListResponse getRecipientHistory(Long userId, String keyword, String sort, Integer size);
}
