package com.paywith.safeaccount.controller;

import com.paywith.common.ApiResponse;
import com.paywith.safeaccount.dto.GuardSafeAccountRegisterRequest;
import com.paywith.safeaccount.dto.SafeAccountDeleteResponse;
import com.paywith.safeaccount.dto.SafeAccountListResponse;
import com.paywith.safeaccount.dto.SafeAccountResponse;
import com.paywith.safeaccount.service.SafeAccountService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import springfox.documentation.annotations.ApiIgnore;
import org.springframework.web.bind.annotation.*;

@Api(tags = "보호자 안전계좌")
@RestController
@RequestMapping("/api/guard/wards/{wardId}/safe-accounts")
@RequiredArgsConstructor
public class GuardSafeAccountController {

    private final SafeAccountService safeAccountService;

    @ApiOperation(
        value = "안전계좌 등록 (보호자)",
        notes = "보호자가 담당 피보호자의 계좌를 안전계좌로 등록한다. 페어링된 보호자가 아니면 404. "
            + "기존에 등록된 수취인이 없으면 실명조회 후 새로 등록하며, 실명조회에 실패하면 404. "
            + "이미 등록된 안전계좌면 409. 신규 등록이면 201, 비활성화됐던 계좌를 다시 등록하면 200.")
    @PostMapping
    public ResponseEntity<ApiResponse<SafeAccountResponse>> register(
            @ApiIgnore @AuthenticationPrincipal Long guardId,
            @ApiParam(value = "피보호자 ID", required = true, example = "1")
            @PathVariable Long wardId,
            @RequestBody GuardSafeAccountRegisterRequest request
    ){
        SafeAccountResponse response = safeAccountService.registerByGuard(guardId,wardId, request);
        HttpStatus status = Boolean.TRUE.equals(response.getNewlyRegistered())
                ? HttpStatus.CREATED // 201 신규 등록
                : HttpStatus.OK; // 200 기존 계좌 활성화
        return ResponseEntity.status(status).body(ApiResponse.success(response));
    }

    @ApiOperation(
        value = "안전계좌 목록 조회 (보호자)",
        notes = "보호자가 담당 피보호자의 안전계좌 목록을 조회한다. 목록에 recipientId 는 노출되지 않는다.")
    @GetMapping
    public ApiResponse<SafeAccountListResponse> getList(
            @AuthenticationPrincipal Long guardId,
            @ApiParam(value = "피보호자 ID", required = true, example = "1")
            @PathVariable Long wardId
    ){
        SafeAccountListResponse response = safeAccountService.getSafeAccountListByGuard(guardId, wardId);
        return ApiResponse.success(response);
    }

    @ApiOperation(
        value = "안전계좌 삭제 (보호자)",
        notes = "보호자가 담당 피보호자의 안전계좌를 비활성화한다. 페어링된 보호자가 아니거나 그 "
            + "피보호자의 안전계좌가 아니면 404.")
    @DeleteMapping("/{safeAccountId}")
    public ApiResponse<SafeAccountDeleteResponse> delete(
            @ApiIgnore @AuthenticationPrincipal Long guardId,
            @ApiParam(value = "피보호자 ID", required = true, example = "1")
            @PathVariable Long wardId,
            @ApiParam(value = "안전계좌(수취인) ID", required = true, example = "10")
            @PathVariable Long safeAccountId
    ){
        SafeAccountDeleteResponse response = safeAccountService.deactivateByGuard(guardId, wardId, safeAccountId);
        return ApiResponse.success(response);
    }
}
