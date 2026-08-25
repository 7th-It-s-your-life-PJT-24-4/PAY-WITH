package com.paywith.charge.controller;

import com.paywith.charge.dto.ChargeRequest;
import com.paywith.charge.dto.ChargeResponse;
import com.paywith.charge.service.ChargeService;
import com.paywith.common.ApiResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import springfox.documentation.annotations.ApiIgnore;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Api(tags = "피보호자 충전")
@RestController
@RequestMapping("api/ward/charges")
@RequiredArgsConstructor
public class ChargeController {

    private final ChargeService chargeService;

    @ApiOperation(
        value = "충전",
        notes = "본인 명의 연동 계좌에서 출금해 지갑 잔액을 충전한다. 역할 검사는 없고 pin 은 이 경로에서 무시된다. "
            + "accountId 누락·amount 누락 또는 0 이하면 400 REQUEST_001(message 는 \"<필드>: <검증 메시지>\" 형식), "
            + "연동 계좌가 없거나 본인 소유가 아니면 404 ACCOUNT_004, 계좌 검사를 통과했는데 지갑이 없으면"
            + "(보호자가 본인 계좌로 호출한 경우 등) 404(code 없음) \"지갑을 찾을 수 없습니다.\", "
            + "출금 후 확정 실패는 500(code 없음, message 에 transactionId 포함 — 출금 실패 분기는 현재 목 구현으로 미발생). "
            + "타입 불일치·JSON 파싱 실패는 500. 응답의 wardId·wardName 은 항상 null, createdAt 은 소수초가 붙을 수 있다.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ChargeResponse> charge(
            @ApiIgnore @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ChargeRequest request
            ){
        ChargeResponse response = chargeService.charge(userId, request);
        return ApiResponse.success(response);
    }

}
