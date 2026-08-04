package com.paywith.transaction.controller;

import com.paywith.common.ApiResponse;
import com.paywith.transaction.dto.GuardTransactionHistoryListResponse;
import com.paywith.transaction.service.TransactionHistoryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

@Api(tags = "보호자 거래내역")
@RestController
@RequestMapping("/api/guard/wards/{wardId}/transactions")
@RequiredArgsConstructor
public class GuardTransactionController {

    private final TransactionHistoryService transactionHistoryService;

    @ApiOperation(
            value = "피보호자 거래내역 조회 (보호자)",
            notes = "보호자가 담당 피보호자의 거래내역을 최신순으로 조회한다. "
                    + "담당이 아니면 404 LINK_001.")
    @GetMapping
    public ApiResponse<GuardTransactionHistoryListResponse> findWardTransactions(
            @ApiIgnore @AuthenticationPrincipal Long guardId,
            @ApiParam(value = "피보호자 ID", required = true, example = "1")
            @PathVariable Long wardId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String riskLevel,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return ApiResponse.success(
                transactionHistoryService.findWardTransactions(guardId, wardId, type, riskLevel, page, size)
        );
    }
}