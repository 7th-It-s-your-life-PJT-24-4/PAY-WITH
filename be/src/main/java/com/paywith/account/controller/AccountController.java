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
        notes = "본인 명의 계좌를 실명조회한 뒤 연동 계좌로 등록한다. 실명조회는 accountPassword 가 아니라 "
            + "가입 시 저장된 생년월일로 수행한다. 같은 사용자가 이미 등록한 계좌(은행코드+계좌번호)면 "
            + "409 ACCOUNT_003(다른 사용자의 동일 계좌 등록은 허용), 실명조회에 실패하면 400 ACCOUNT_002"
            + "(현재 실명조회 목 구현으로 미발생). 요청 본문에 서버 검증이 없어 bankCode·accountNo 누락이나 "
            + "미등록 은행코드는 400 이 아니라 DB 제약 위반으로 500. JSON 파싱 실패도 500.")
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
        notes = "로그인한 사용자가 등록한 연동 계좌 목록을 최신 등록순으로 반환한다. 등록 계좌가 없으면 빈 배열.")
    @GetMapping
    public ApiResponse<List<AccountResponse>> getAccounts(
            @ApiIgnore @AuthenticationPrincipal Long userId
    ){
        List<AccountResponse> response = accountService.getAccounts(userId);
        return ApiResponse.success(response);
    }

}
