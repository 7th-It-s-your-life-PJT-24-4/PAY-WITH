package com.paywith.bank.controller;

import com.paywith.bank.dto.BankFilterRequest;
import com.paywith.bank.dto.BankFilterResponse;
import com.paywith.bank.service.BankFilterService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.paywith.common.ApiResponse;

@Api(tags = "피보호자 은행 필터링")
@RestController
@RequestMapping("/api/ward/filter-bank")
public class BankFilterController {

    private final BankFilterService bankFilterService;

    public BankFilterController(BankFilterService bankFilterService) {
        this.bankFilterService = bankFilterService;
    }

    @ApiOperation(
        value = "계좌번호 기반 은행 필터링",
        notes = "계좌번호 형식 규칙(은행별 자릿수 범위·앞자리 prefix)과 일치하는 활성 은행 후보 목록을 반환한다. "
            + "일치하는 은행이 없으면 400이 아니라 빈 배열로 응답한다. 계좌번호가 숫자가 아니거나 "
            + "1~20자리를 벗어나면 400, WARD가 아니면 403, 페어링 미완료면 403.")
    @PostMapping
    public ApiResponse<BankFilterResponse> filterBanks(
        @AuthenticationPrincipal Long wardId,
        @RequestBody BankFilterRequest request
    ) {
        return ApiResponse.success(bankFilterService.filterBanks(wardId, request.getAccountNo()));
    }
}