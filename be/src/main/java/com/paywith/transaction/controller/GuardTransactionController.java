package com.paywith.transaction.controller;

import com.paywith.common.ApiResponse;
import com.paywith.transaction.dto.GuardTransactionDetailResponse;
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
                    + "담당이 아니면 404 LINK_001. 보호자가 대신 실행한 거래(대리 충전)는 이 목록에서 "
                    + "제외된다(충전 이력은 보호자 충전 내역 API 사용). type(CHARGE·TRANSFER·PAYMENT)· "
                    + "riskLevel(SAFE·CAUTION·DANGER) 허용값 위반, page 음수, size 1~100 이탈은 "
                    + "400 REQUEST_001.")
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

    @ApiOperation(
            value = "피보호자 거래내역 상세 조회 (보호자)",
            notes = "보호자가 담당 피보호자의 거래 상세를 조회한다. "
                    + "담당이 아니면 404 LINK_001, 거래가 없거나 다른 피보호자 것이면 404 TRANSACTION_003.")
    @GetMapping("/{id}")
    public ApiResponse<GuardTransactionDetailResponse> findWardTransactionDetail(
            @ApiIgnore @AuthenticationPrincipal Long guardId,
            @ApiParam(value = "피보호자 ID", required = true, example = "1")
            @PathVariable Long wardId,
            @ApiParam(value = "거래 식별자", required = true, example = "1031")
            @PathVariable Long id
    ) {
        return ApiResponse.success(
                transactionHistoryService.findWardTransactionDetail(guardId, wardId, id)
        );
    }
}