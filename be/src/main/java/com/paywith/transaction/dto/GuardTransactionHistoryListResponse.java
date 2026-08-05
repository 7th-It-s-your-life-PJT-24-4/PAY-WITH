package com.paywith.transaction.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;

import java.util.List;

@ApiModel(description = "보호자용 거래내역 목록 조회 응답")
@Getter
public class GuardTransactionHistoryListResponse {

    private final List<GuardTransactionHistoryItem> transactions;
    private final int page;
    private final int size;
    private final int totalElements;
    private final int totalPages;
    private final boolean hasNext;

    public GuardTransactionHistoryListResponse(
            List<GuardTransactionHistoryItem> transactions, int page, int size, int totalElements
    ) {
        this.transactions = transactions;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = (int) Math.ceil((double) totalElements / size);
        this.hasNext = (page + 1) < this.totalPages;
    }
}