package com.paywith.charge.controller;

import com.paywith.charge.dto.ChargeDetailResponse;
import com.paywith.charge.dto.ChargeHistoryListResponse;
import com.paywith.charge.service.ChargeService;
import com.paywith.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/guard/charges")
@RequiredArgsConstructor
public class GuardChargeHistoryController {

    private final ChargeService chargeService;

    @GetMapping
    public ApiResponse<ChargeHistoryListResponse> getChargeHistories(
            @AuthenticationPrincipal Long guardId
    ){
        ChargeHistoryListResponse response = chargeService.getChargeHistories(guardId);
        return ApiResponse.success(response);
    }

    @GetMapping("/{id}")
    public ApiResponse<ChargeDetailResponse> getChargeDetail(
            @AuthenticationPrincipal Long guardId, @PathVariable Long id
    ){
        ChargeDetailResponse response = chargeService.getChargeDetail(guardId, id);
        return ApiResponse.success(response);
    }
}
