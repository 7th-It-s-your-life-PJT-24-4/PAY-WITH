package com.paywith.account.controller;


import com.paywith.account.dto.AccountCreateRequest;
import com.paywith.account.dto.AccountResponse;
import com.paywith.account.service.AccountService;
import com.paywith.common.ApiResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import springfox.documentation.annotations.ApiIgnore;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "계좌 연동")
@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @ApiOperation(
        value = "계좌 등록",
        notes = "본인 명의 계좌를 실명조회한 뒤 연동 계좌로 등록한다. 이미 등록된 계좌(은행코드+계좌번호)면 "
            + "409, 실명조회에 실패하면 400.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AccountResponse> registerAccount(
            @ApiIgnore @AuthenticationPrincipal Long userId,
            @RequestBody AccountCreateRequest request
            ){
        AccountResponse response = accountService.registerAccount(userId, request);
        return ApiResponse.success(response);
    }

    @ApiOperation(
        value = "연동 계좌 목록 조회",
        notes = "로그인한 사용자가 등록한 연동 계좌 목록을 최신 등록순으로 반환한다.")
    @GetMapping
    public ApiResponse<List<AccountResponse>> getAccounts(
            @ApiIgnore @AuthenticationPrincipal Long userId
    ){
        List<AccountResponse> response = accountService.getAccounts(userId);
        return ApiResponse.success(response);
    }

}
