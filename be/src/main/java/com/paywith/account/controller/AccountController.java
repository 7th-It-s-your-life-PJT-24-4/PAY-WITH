package com.paywith.account.controller;


import com.paywith.account.dto.AccountCreateRequest;
import com.paywith.account.dto.AccountResponse;
import com.paywith.account.service.AccountService;
import com.paywith.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AccountResponse> registerAccount(
            @AuthenticationPrincipal Long userId,
            @RequestBody AccountCreateRequest request
            ){
        AccountResponse response = accountService.registerAccount(userId, request);
        return ApiResponse.success(response);
    }

    @GetMapping
    public ApiResponse<List<AccountResponse>> getAccounts(
            @AuthenticationPrincipal Long userId
    ){
        List<AccountResponse> response = accountService.getAccounts(userId);
        return ApiResponse.success(response);
    }

}
