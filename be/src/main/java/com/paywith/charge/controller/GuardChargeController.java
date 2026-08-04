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
        notes = "보호자가 담당 피보호자의 지갑에 자신의 연동 계좌로 충전한다. 페어링된 보호자가 아니면 "
            + "404, 보호자 본인 소유 계좌가 아니면 404.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ChargeResponse> chargeByGuard(
            @ApiIgnore @AuthenticationPrincipal Long guardId,
            @ApiParam(value = "피보호자 ID", required = true, example = "1")
            @PathVariable Long wardId,
            @Valid @RequestBody ChargeRequest request
    ){
        ChargeResponse response = chargeService.chargeByGuard(guardId, wardId, request);
        return ApiResponse.success(response);
    }


}
