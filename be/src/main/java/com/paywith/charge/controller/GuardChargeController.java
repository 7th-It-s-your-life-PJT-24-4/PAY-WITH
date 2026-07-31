package com.paywith.charge.controller;

import com.paywith.charge.dto.ChargeRequest;
import com.paywith.charge.dto.ChargeResponse;
import com.paywith.charge.service.ChargeService;
import com.paywith.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import springfox.documentation.annotations.ApiIgnore;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/guard/wards/{wardId}/charges")
@RequiredArgsConstructor
public class GuardChargeController {

    private final ChargeService chargeService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ChargeResponse> chargeByGuard(
            @ApiIgnore @AuthenticationPrincipal Long guardId,
            @PathVariable Long wardId,
            @RequestBody ChargeRequest request
    ){
        ChargeResponse response = chargeService.chargeByGuard(guardId, wardId, request);
        return ApiResponse.success(response);
    }


}
