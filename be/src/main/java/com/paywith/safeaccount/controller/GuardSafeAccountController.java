package com.paywith.safeaccount.controller;

import com.paywith.common.ApiResponse;
import com.paywith.safeaccount.dto.GuardSafeAccountRegisterRequest;
import com.paywith.safeaccount.dto.SafeAccountListResponse;
import com.paywith.safeaccount.dto.SafeAccountResponse;
import com.paywith.safeaccount.service.SafeAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import springfox.documentation.annotations.ApiIgnore;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/guard/wards/{wardId}/safe-accounts")
@RequiredArgsConstructor
public class GuardSafeAccountController {

    private final SafeAccountService safeAccountService;

    @PostMapping
    public ResponseEntity<ApiResponse<SafeAccountResponse>> register(
            @ApiIgnore @AuthenticationPrincipal Long guardId,
            @PathVariable Long wardId,
            @RequestBody GuardSafeAccountRegisterRequest request
    ){
        SafeAccountResponse response = safeAccountService.registerByGuard(guardId,wardId, request);
        HttpStatus status = Boolean.TRUE.equals(response.getNewlyRegistered())
                ? HttpStatus.CREATED // 201 신규 등록
                : HttpStatus.OK; // 200 기존 계좌 활성화
        return ResponseEntity.status(status).body(ApiResponse.success(response));
    }

    @GetMapping
    public ApiResponse<SafeAccountListResponse> getList(
            @AuthenticationPrincipal Long guardId,
            @PathVariable Long wardId
    ){
        SafeAccountListResponse response = safeAccountService.getSafeAccountListByGuard(guardId, wardId);
        return ApiResponse.success(response);
    }
}
