package com.paywith.charge.controller;

import com.paywith.charge.dto.ChargeRequest;
import com.paywith.charge.dto.ChargeResponse;
import com.paywith.charge.service.ChargeService;
import com.paywith.common.ApiResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import springfox.documentation.annotations.ApiIgnore;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Api(tags = "보호자 충전")
@RestController
@RequestMapping("api/guard/wards/{wardId}/charges")
@RequiredArgsConstructor
public class GuardChargeController {

    private final ChargeService chargeService;

    @ApiOperation(
        value = "보호자 충전",
        notes = "보호자가 담당 피보호자의 지갑에 자신의 연동 계좌로 충전한다. "
            + "accountId 누락·amount 누락 또는 0 이하면 400 REQUEST_001(message 는 \"<필드>: <검증 메시지>\" 형식). "
            + "이후 검사 순서: ACTIVE 페어링된 보호자가 아니면(WARD 호출 포함) 404 LINK_001 → 충전 비밀번호(pin)가 "
            + "없거나 틀리면 400 CHARGE_003 → 보호자 본인 소유 계좌가 아니면 404 ACCOUNT_004 → 피보호자 지갑이 없으면 "
            + "404(code 없음) \"지갑을 찾을 수 없습니다.\". 출금 후 확정 실패는 500(code 없음, message 에 transactionId "
            + "포함 — 출금 실패 분기는 현재 목 구현으로 미발생). 타입 불일치·JSON 파싱 실패·비숫자 wardId 는 500. "
            + "응답 createdAt 은 소수초가 붙을 수 있다.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ChargeResponse> chargeByGuard(
            @ApiIgnore @AuthenticationPrincipal Long guardId,
            @ApiParam(value = "피보호자 ID(비숫자면 500)", required = true, example = "1")
            @PathVariable Long wardId,
            @Valid @RequestBody ChargeRequest request
    ){
        ChargeResponse response = chargeService.chargeByGuard(guardId, wardId, request);
        return ApiResponse.success(response);
    }


}
