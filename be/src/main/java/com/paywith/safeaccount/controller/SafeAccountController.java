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
        notes = "완료된 송금 이력(TRANSFER_OUT·COMPLETED)이 있는 본인 수취인을 안전계좌로 등록한다. "
            + "피보호자(WARD)가 아니면 403 AUTH_004 \"피보호자만 접근할 수 있습니다.\", 보호자와 ACTIVE 페어링이 "
            + "없으면 403 WARD_001 \"페어링 완료 후 이용할 수 있습니다.\". accountAlias 가 50자를 초과하면 "
            + "400 SAFE_ACCOUNT_004 \"입력값이 올바르지 않습니다.\"(공백만이면 null 로 저장). "
            + "recipientId 가 누락(null)·타인 수취인·완료 송금 이력 없음이면 404 RECIPIENT_003 "
            + "\"송금 이력이 있는 수취인을 찾을 수 없습니다.\"(별도 400 없음). 이미 ACTIVE 안전계좌면 "
            + "409 SAFE_ACCOUNT_001 \"이미 등록된 안전계좌입니다.\"(동시 요청으로 UPDATE 0행이어도 같은 409). "
            + "신규 등록이면 201, 비활성화됐던 안전계좌를 다시 등록하면 200(별칭은 요청값으로 덮어씀)으로 "
            + "응답한다. safeAccountId 는 recipientId 와 항상 같은 값. "
            + "recipientId 비숫자 등 타입 불일치·JSON 파싱 실패는 500.")
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
        notes = "피보호자 본인의 ACTIVE 안전계좌 목록을 조회한다. 피보호자(WARD)가 아니면 403 AUTH_004 "
            + "\"피보호자만 접근할 수 있습니다.\", 보호자와 ACTIVE 페어링이 없으면 403 WARD_001 "
            + "\"페어링 완료 후 이용할 수 있습니다.\". 없으면 빈 배열, 등록 시각 내림차순. "
            + "목록에 recipientId 가 함께 노출된다(safeAccountId 와 같은 값).")
    @GetMapping
    public ApiResponse<SafeAccountListResponse> getList(
        @ApiIgnore @AuthenticationPrincipal Long userId
    ){
        SafeAccountListResponse response = safeAccountService.getSafeAccountListByWard(userId);
        return ApiResponse.success(response);
    }

    @ApiOperation(
        value = "안전계좌 삭제 (피보호자)",
        notes = "피보호자가 본인의 안전계좌를 비활성화한다. 피보호자(WARD)가 아니면 403 AUTH_004 — 단 이 API 만 "
            + "message 가 \"안전계좌를 찾을 수 없습니다.\"(다른 피보호자 API 의 \"피보호자만 접근할 수 있습니다.\" 와 "
            + "다름). 페어링 여부는 검사하지 않는다. safeAccountId(=recipientId)가 없거나 본인 수취인이 아니거나 "
            + "안전계좌로 등록된 적 없으면 404 SAFE_ACCOUNT_003 \"안전계좌를 찾을 수 없습니다.\". "
            + "이미 비활성화된 상태여도 멱등하게 200 {safeAccountId, status: INACTIVE} 로 응답한다(204 아님). "
            + "비숫자 경로값은 500.")
    @DeleteMapping("/{safeAccountId}")
    public ApiResponse<SafeAccountDeleteResponse> delete(
            @ApiIgnore @AuthenticationPrincipal Long userId,
            @ApiParam(value = "안전계좌 ID (recipientId 와 동일, 1 이상 정수 검증 없음, 비숫자는 500)", required = true, example = "10")
            @PathVariable Long safeAccountId
    ){
        SafeAccountDeleteResponse response = safeAccountService.deactivateByWard(userId, safeAccountId);
        return ApiResponse.success(response);
    }

 }
