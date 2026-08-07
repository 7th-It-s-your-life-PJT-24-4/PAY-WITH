package com.paywith.merchant.controller;

import com.paywith.common.ApiResponse;
import com.paywith.merchant.dto.MerchantListResponse;
import com.paywith.merchant.service.MerchantService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 가맹점 스캐너(비로그인 제3 액터)용 API — 무토큰 개방(permitAll).
 * SecurityConfig에 무토큰 예외가 등록돼 있다.
 */
@Api(tags = "가맹점 (가맹점 스캐너)")
@RestController
@RequestMapping("/api/merchants")
public class MerchantController {

    private final MerchantService merchantService;

    public MerchantController(MerchantService merchantService) {
        this.merchantService = merchantService;
    }

    @ApiOperation(
        value = "결제 가능 가맹점 목록",
        notes = "좌표가 등록된 가맹점만 반환한다(결제 가능 = 좌표 등록). 좌표 자체는 응답에 없다.")
    @GetMapping
    public ApiResponse<MerchantListResponse> findAllPayable() {
        return ApiResponse.success(merchantService.findAllPayable());
    }
}
