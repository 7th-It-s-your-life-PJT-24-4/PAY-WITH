package com.paywith.bank.controller;

import com.paywith.bank.dto.BankFilterRequest;
import com.paywith.bank.dto.BankFilterResponse;
import com.paywith.bank.service.BankFilterService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.paywith.common.ApiResponse;

@RestController
@RequestMapping("/api/ward/filter-bank")
public class BankFilterController {

    private final BankFilterService bankFilterService;

    public BankFilterController(BankFilterService bankFilterService) {
        this.bankFilterService = bankFilterService;
    }

    @PostMapping
    public ApiResponse<BankFilterResponse> filterBanks(
        @AuthenticationPrincipal Long wardId,
        @RequestBody BankFilterRequest request
    ) {
        return ApiResponse.success(bankFilterService.filterBanks(wardId, request.getAccountNo()));
    }
}