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
import springfox.documentation.annotations.ApiIgnore;
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
            + "일치하는 은행이 없으면 400이 아니라 빈 배열로 응답한다(후보 순서는 DB 반환 순서, 정렬 보장 없음). "
            + "검사 순서: WARD가 아니면 403 AUTH_004 → ACTIVE 페어링이 없으면 403 WARD_001 → "
            + "accountNo 가 없거나 숫자가 아니거나 1~20자리를 벗어나면 400 ACCOUNT_001. JSON 파싱 실패는 500.")
    @PostMapping
    public ApiResponse<BankFilterResponse> filterBanks(
        @ApiIgnore @AuthenticationPrincipal Long wardId,
        @RequestBody BankFilterRequest request
    ) {
        return ApiResponse.success(bankFilterService.filterBanks(wardId, request.getAccountNo()));
    }
}