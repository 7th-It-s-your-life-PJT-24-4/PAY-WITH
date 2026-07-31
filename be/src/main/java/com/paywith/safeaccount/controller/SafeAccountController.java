package com.paywith.safeaccount.controller;

import com.paywith.common.ApiResponse;
import com.paywith.safeaccount.dto.SafeAccountListResponse;
import com.paywith.safeaccount.dto.SafeAccountRegisterRequest;
import com.paywith.safeaccount.dto.SafeAccountResponse;
import com.paywith.safeaccount.service.SafeAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ward/safe-accounts")
@RequiredArgsConstructor
public class SafeAccountController {

    private final SafeAccountService safeAccountService;

    @PostMapping
    public ResponseEntity<ApiResponse<SafeAccountResponse>> register(
            @AuthenticationPrincipal Long userId,
            @RequestBody SafeAccountRegisterRequest request
            ){
        SafeAccountResponse response = safeAccountService.registerByWard(userId, request);
        HttpStatus status = Boolean.TRUE.equals(response.getNewlyRegistered())
                ? HttpStatus.CREATED // 201 신규 등록
                : HttpStatus.OK; // 200 기존 안전계좌 복구
        return ResponseEntity.status(status).body(ApiResponse.success(response));
    }

    @GetMapping
    public ApiResponse<SafeAccountListResponse> getList(
        @AuthenticationPrincipal Long userId
    ){
        SafeAccountListResponse response = safeAccountService.getSafeAccountListByWard(userId);
        return ApiResponse.success(response);
    }

 }
