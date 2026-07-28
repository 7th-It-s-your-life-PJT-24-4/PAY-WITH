package com.paywith.account.controller;


import com.paywith.account.dto.AccountCreateRequest;
import com.paywith.account.dto.AccountResponse;
import com.paywith.account.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<AccountResponse> registerAccount(
            @RequestParam Long userId, // TODO: 로그인 기능 구현되면 여기부터 수정하기 !!
            @RequestBody AccountCreateRequest request
            ){
        AccountResponse response = accountService.registerAccount(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<AccountResponse>> getAccounts(
            @RequestParam Long userId // Todo: 로그인 후 인증 정보에서 추출해야 함
    ){
        List<AccountResponse> response = accountService.getAccounts(userId);
        return ResponseEntity.ok(response);
    }

}
