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
        notes = "보호자가 담당 피보호자의 계좌를 안전계좌로 등록한다. 이 피보호자의 ACTIVE 보호자가 아니면 "
            + "404 LINK_001 \"연동된 피보호자를 찾을 수 없습니다.\". accountAlias 가 50자를 초과하면 "
            + "400 SAFE_ACCOUNT_004 \"입력값이 올바르지 않습니다.\"(공백만이면 null 로 저장). "
            + "같은 bankCode·accountNo 의 수취인이 이미 있으면 그 행을 안전계좌로 전환하며, 이미 ACTIVE 안전계좌면 "
            + "409 SAFE_ACCOUNT_001 \"이미 등록된 안전 계좌 입니다.\"(동시 요청으로 UPDATE 0행이어도 같은 409). "
            + "수취인이 없으면 실명조회 후 새 수취인 행을 만들며, 실명조회에 실패하면 404 ACCOUNT_005 "
            + "\"해당 계좌를 찾을 수 없습니다.\"(현재 실명조회 목 구현으로 미발생 — 목은 항상 성공하고 "
            + "예금주명은 계좌번호로 합성). 신규 등록이면 201, 비활성화됐던 계좌를 다시 등록하면 200"
            + "(별칭·등록자를 요청값으로 덮어씀). safeAccountId 는 recipientId 와 항상 같은 값. "
            + "bankCode·accountNo 는 서버 검증 없음: 누락·미등록 은행코드는 DB 제약 위반으로 500, "
            + "숫자 형식도 검사하지 않는다. 타입 불일치·JSON 파싱 실패·비숫자 경로값도 500.")
    @PostMapping
    public ResponseEntity<ApiResponse<SafeAccountResponse>> register(
            @ApiIgnore @AuthenticationPrincipal Long guardId,
            @ApiParam(value = "피보호자 ID (1 이상 정수 검증 없음, 비숫자는 500)", required = true, example = "1")
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
        notes = "보호자가 담당 피보호자의 ACTIVE 안전계좌 목록을 조회한다. 이 피보호자의 ACTIVE 보호자가 아니면 "
            + "403 AUTH_005 \"해당 시니어의 보호자가 아닙니다.\"(등록·삭제의 404 LINK_001 과 코드가 다름). "
            + "없으면 빈 배열, 등록 시각 내림차순. 목록에 recipientId 는 노출되지 않는다(null). "
            + "wardId 는 1 이상 정수 검증 없음(0·음수는 403, 비숫자는 500).")
    @GetMapping
    public ApiResponse<SafeAccountListResponse> getList(
            @ApiIgnore @AuthenticationPrincipal Long guardId,
            @ApiParam(value = "피보호자 ID (1 이상 정수 검증 없음, 비숫자는 500)", required = true, example = "1")
            @PathVariable Long wardId
    ){
        SafeAccountListResponse response = safeAccountService.getSafeAccountListByGuard(guardId, wardId);
        return ApiResponse.success(response);
    }

    @ApiOperation(
        value = "안전계좌 삭제 (보호자)",
        notes = "보호자가 담당 피보호자의 안전계좌를 비활성화한다. 이 피보호자의 ACTIVE 보호자가 아니면 "
            + "404 LINK_001 \"연동된 피보호자를 찾을 수 없습니다.\". safeAccountId(=recipientId)가 없거나 "
            + "그 피보호자의 수취인이 아니거나 안전계좌로 등록된 적 없으면 404 SAFE_ACCOUNT_003 "
            + "\"안전계좌를 찾을 수 없습니다.\". 이미 비활성화된 상태여도 멱등하게 200 "
            + "{safeAccountId, status: INACTIVE} 로 응답한다(204 아님). 비숫자 경로값은 500.")
    @DeleteMapping("/{safeAccountId}")
    public ApiResponse<SafeAccountDeleteResponse> delete(
            @ApiIgnore @AuthenticationPrincipal Long guardId,
            @ApiParam(value = "피보호자 ID (1 이상 정수 검증 없음, 비숫자는 500)", required = true, example = "1")
            @PathVariable Long wardId,
            @ApiParam(value = "안전계좌 ID (recipientId 와 동일, 1 이상 정수 검증 없음, 비숫자는 500)", required = true, example = "10")
            @PathVariable Long safeAccountId
    ){
        SafeAccountDeleteResponse response = safeAccountService.deactivateByGuard(guardId, wardId, safeAccountId);
        return ApiResponse.success(response);
    }
}
