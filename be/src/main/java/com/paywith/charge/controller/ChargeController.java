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
        notes = "본인 명의 연동 계좌에서 출금해 지갑 잔액을 충전한다. 본인 소유 계좌가 아니면 404, "
            + "연동 계좌 자체가 존재하지 않아도 404.")
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
