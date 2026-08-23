package com.paywith.safeaccount.controller;

import com.paywith.common.ApiResponse;
import com.paywith.safeaccount.dto.SafeAccountDeleteResponse;
import com.paywith.safeaccount.dto.SafeAccountListResponse;
import com.paywith.safeaccount.dto.SafeAccountRegisterRequest;
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

@Api(tags = "피보호자 안전계좌")
@RestController
@RequestMapping("/api/ward/safe-accounts")
@RequiredArgsConstructor
public class SafeAccountController {

    private final SafeAccountService safeAccountService;

    @ApiOperation(
        value = "안전계좌 등록",
        notes = "완료된 송금 이력이 있는 수취인을 안전계좌로 등록한다. 피보호자만 이용할 수 있고 "
            + "보호자와 페어링되어 있지 않으면 403. 완료된 송금 이력이 있는 수취인을 찾지 못하면 404, "
            + "이미 등록돼 있으면 409. 신규 등록이면 201, 비활성화됐던 안전계좌를 다시 등록하면 200으로 "
            + "응답한다.")
    @PostMapping
    public ResponseEntity<ApiResponse<SafeAccountResponse>> register(
            @ApiIgnore @AuthenticationPrincipal Long userId,
            @RequestBody SafeAccountRegisterRequest request
            ){
        SafeAccountResponse response = safeAccountService.registerByWard(userId, request);
        HttpStatus status = Boolean.TRUE.equals(response.getNewlyRegistered())
                ? HttpStatus.CREATED // 201 신규 등록
                : HttpStatus.OK; // 200 기존 안전계좌 복구
        return ResponseEntity.status(status).body(ApiResponse.success(response));
    }

    @ApiOperation(
        value = "안전계좌 목록 조회 (피보호자)",
        notes = "피보호자 본인의 안전계좌 목록을 조회한다. 목록에 recipientId 가 함께 노출된다.")
    @GetMapping
    public ApiResponse<SafeAccountListResponse> getList(
        @ApiIgnore @AuthenticationPrincipal Long userId
    ){
        SafeAccountListResponse response = safeAccountService.getSafeAccountListByWard(userId);
        return ApiResponse.success(response);
    }

    @ApiOperation(
        value = "안전계좌 삭제 (피보호자)",
        notes = "피보호자가 본인의 안전계좌를 비활성화한다. 본인 소유가 아니거나 안전계좌로 등록된 적 "
            + "없으면 404. 이미 비활성화된 상태여도 멱등하게 정상 응답한다.")
    @DeleteMapping("/{safeAccountId}")
    public ApiResponse<SafeAccountDeleteResponse> delete(
            @ApiIgnore @AuthenticationPrincipal Long userId,
            @ApiParam(value = "안전계좌(수취인) ID", required = true, example = "10")
            @PathVariable Long safeAccountId
    ){
        SafeAccountDeleteResponse response = safeAccountService.deactivateByWard(userId, safeAccountId);
        return ApiResponse.success(response);
    }

 }
