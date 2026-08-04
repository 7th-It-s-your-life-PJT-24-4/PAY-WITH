package com.paywith.transaction.controller;

import com.paywith.common.ApiResponse;
import com.paywith.transaction.dto.TransactionHistoryListResponse;
import com.paywith.transaction.service.TransactionHistoryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

@Api(tags = "피보호자 거래내역")
@RestController
@RequestMapping("/api/ward/transactions")
@RequiredArgsConstructor
public class WardTransactionController {

    private final TransactionHistoryService transactionHistoryService;

    @ApiOperation(
            value = "피보호자 거래내역 목록 조회",
            notes = "로그인한 피보호자 본인의 거래내역을 최신순으로 조회한다. "
                    + "대상을 파라미터로 받지 않고 인증 주체로만 정하므로 다른 사람의 내역은 조회할 수 없다. "
                    + "category 생략 시 전체 조회, keyword로 상대방 이름·가맹점명·금액을 검색할 수 있다.")
    @GetMapping
    public ApiResponse<TransactionHistoryListResponse> findMyTransactions(
            @ApiIgnore @AuthenticationPrincipal Long userId,
            @ApiParam(value = "거래 분류", example = "PAYMENT")
            @RequestParam(required = false) String category,
            @ApiParam(value = "상호, 상대방 이름 또는 금액 검색어", example = "이마트")
            @RequestParam(required = false) String keyword,
            @ApiParam(value = "페이지 번호(0부터 시작)", example = "0")
            @RequestParam(required = false) Integer page,
            @ApiParam(value = "페이지당 거래 수", example = "20")
            @RequestParam(required = false) Integer size
    ) {
        return ApiResponse.success(
                transactionHistoryService.findMyTransactions(userId, category, keyword, page, size)
        );
    }
}