package com.paywith.charge.controller;

import com.paywith.charge.dto.ChargeDetailResponse;
import com.paywith.charge.dto.ChargeHistoryListResponse;
import com.paywith.charge.service.ChargeService;
import com.paywith.common.ApiResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import springfox.documentation.annotations.ApiIgnore;
import org.springframework.web.bind.annotation.*;

@Api(tags = "보호자 충전 내역")
@RestController
@RequestMapping("api/guard/charges")
@RequiredArgsConstructor
public class GuardChargeHistoryController {

    private final ChargeService chargeService;

    @ApiOperation(
        value = "보호자 충전 내역 목록",
        notes = "로그인한 보호자가 실행한 충전 내역을 최신순으로 반환한다.")
    @GetMapping
    public ApiResponse<ChargeHistoryListResponse> getChargeHistories(
            @ApiIgnore @AuthenticationPrincipal Long guardId
    ){
        ChargeHistoryListResponse response = chargeService.getChargeHistories(guardId);
        return ApiResponse.success(response);
    }

    @ApiOperation(
        value = "보호자 충전 내역 상세",
        notes = "충전 내역 한 건의 상세 정보를 조회한다. 로그인한 보호자가 실행한 충전 건이 아니거나 "
            + "존재하지 않으면 404.")
    @GetMapping("/{id}")
    public ApiResponse<ChargeDetailResponse> getChargeDetail(
            @ApiIgnore @AuthenticationPrincipal Long guardId,
            @ApiParam(value = "충전 거래 ID", required = true, example = "999")
            @PathVariable Long id
    ){
        ChargeDetailResponse response = chargeService.getChargeDetail(guardId, id);
        return ApiResponse.success(response);
    }
}
