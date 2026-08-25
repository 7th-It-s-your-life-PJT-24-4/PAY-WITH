package com.paywith.transaction.controller;

import com.paywith.common.ApiResponse;
import com.paywith.transaction.dto.TransactionDetailResponse;
import com.paywith.transaction.dto.TransactionHistoryListResponse;
import com.paywith.transaction.service.TransactionHistoryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

@Api(tags = "피보호자 거래내역")
@RestController
@RequestMapping("/api/ward/transactions")
@RequiredArgsConstructor
public class WardTransactionController {

    private final TransactionHistoryService transactionHistoryService;

    @ApiOperation(
            value = "피보호자 거래내역 목록 조회",
            notes = "로그인한 피보호자 본인의 거래내역을 최신순(created_at 내림차순)으로 조회한다. "
                    + "대상을 파라미터로 받지 않고 인증 주체로만 정하므로 다른 사람의 내역은 조회할 수 없다. "
                    + "먼저 역할·지갑을 확인하므로 보호자 계정은 403 AUTH_004, 지갑이 없으면 404 WALLET_001. "
                    + "category 생략·빈 값 시 전체(ALL) 조회, keyword 로 수취인 예금주명·가맹점명·충전 은행명 또는 금액을 "
                    + "부분 일치로 검색한다. 보호자 대리 충전도 CHARGE 로 포함된다. "
                    + "category(ALL·CHARGE·TRANSFER·PAYMENT) 허용값 위반(대소문자 구분), page 음수, size 1~100 이탈은 "
                    + "400 TRANSACTION_001 \"조회 조건이 올바르지 않습니다.\". page 생략 시 0, size 생략 시 20. "
                    + "비숫자 page·size 는 500. 시각 필드는 오프셋 없는 ISO-8601.")
    @GetMapping
    public ApiResponse<TransactionHistoryListResponse> findMyTransactions(
            @ApiIgnore @AuthenticationPrincipal Long userId,
            @ApiParam(value = "거래 분류. 생략·빈 값이면 ALL. 대소문자 구분",
                    allowableValues = "ALL,CHARGE,TRANSFER,PAYMENT", example = "PAYMENT")
            @RequestParam(required = false) String category,
            @ApiParam(value = "수취인 예금주명·가맹점명·충전 은행명 또는 금액 검색어(부분 일치)", example = "이마트")
            @RequestParam(required = false) String keyword,
            @ApiParam(value = "페이지 번호(0부터 시작). 생략 시 0, 음수면 400", example = "0")
            @RequestParam(required = false) Integer page,
            @ApiParam(value = "페이지당 거래 수(1~100). 생략 시 20", example = "20")
            @RequestParam(required = false) Integer size
    ) {
        return ApiResponse.success(
                transactionHistoryService.findMyTransactions(userId, category, keyword, page, size)
        );
    }

    @ApiOperation(
            value = "피보호자 거래내역 상세 조회",
            notes = "로그인한 피보호자 본인의 거래 상세를 조회한다. "
                    + "목록과 달리 역할·지갑 검사를 하지 않고 본인 지갑의 거래인지로만 판단하므로 "
                    + "다른 사람의 거래, 없는 id, 보호자 계정의 호출은 모두 404 TRANSACTION_003(403 이 아님). "
                    + "riskAnalysis 는 type 이 CHARGE 이거나 위험 평가 기록이 없으면 null(SAFE 라도 평가 기록이 있으면 채워짐). "
                    + "비숫자 id 는 500. 시각 필드는 오프셋 없는 ISO-8601.")
    @GetMapping("/{id}")
    public ApiResponse<TransactionDetailResponse> findMyTransactionDetail(
            @ApiIgnore @AuthenticationPrincipal Long userId,
            @ApiParam(value = "조회할 거래 번호", required = true, example = "138")
            @PathVariable Long id
    ) {
        return ApiResponse.success(
                transactionHistoryService.findMyTransactionDetail(userId, id)
        );
    }
}