package com.paywith.merchant.controller;

import com.paywith.common.ApiResponse;
import com.paywith.merchant.dto.MerchantListResponse;
import com.paywith.merchant.service.MerchantService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 가맹점 스캐너(비로그인 제3 액터)용 API — B7 확정으로 permitAll.
 * SecurityConfig 예외는 별도 커밋으로 추가한다.
 */
@RestController
@RequestMapping("/api/merchants")
public class MerchantController {

    private final MerchantService merchantService;

    public MerchantController(MerchantService merchantService) {
        this.merchantService = merchantService;
    }

    @GetMapping
    public ApiResponse<MerchantListResponse> findAllPayable() {
        return ApiResponse.success(merchantService.findAllPayable());
    }
}
