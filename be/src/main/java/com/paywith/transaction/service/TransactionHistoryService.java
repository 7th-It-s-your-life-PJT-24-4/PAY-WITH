package com.paywith.transaction.service;

import com.paywith.transaction.dto.TransactionHistoryListResponse;

public interface TransactionHistoryService {
    // 피보호자 본인의 거래 내역 조회
    TransactionHistoryListResponse findMyTransactions(
            Long userId,
            String category,
            String keyword,
            Integer page,
            Integer size
    );
}
