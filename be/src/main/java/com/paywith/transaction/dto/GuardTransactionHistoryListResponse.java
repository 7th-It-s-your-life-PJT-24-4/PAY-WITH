package com.paywith.transaction.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;

import java.util.List;

@ApiModel(description = "보호자용 거래내역 목록 조회 응답")
@Getter
public class GuardTransactionHistoryListResponse {

    @ApiModelProperty(value = "거래 내역")
    private final List<GuardTransactionHistoryItem> transactions;

    @ApiModelProperty(value = "현재 페이지 번호", example = "0")
    private final int page;

    @ApiModelProperty(value = "페이지당 거래 수", example = "20")
    private final int size;

    @ApiModelProperty(value = "검색 조건에 해당하는 전체 거래 수", example = "43")
    private final int totalElements;

    @ApiModelProperty(value = "전체 페이지 수", example = "3")
    private final int totalPages;

    @ApiModelProperty(value = "다음 페이지 존재 여부", example = "true")
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