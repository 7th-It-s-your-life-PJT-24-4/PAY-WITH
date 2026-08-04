package com.paywith.transaction.service;

import com.paywith.transaction.dto.GuardTransactionHistoryListResponse;
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

    // 보호자가 피보호자의 거래 내역 조회
    GuardTransactionHistoryListResponse findWardTransactions(
            Long guardId,
            Long wardId,
            String type,
            String riskLevel,
            Integer page,
            Integer size
    );
}
