package com.paywith.payment.controller;

import com.paywith.common.ApiResponse;
import com.paywith.payment.dto.ExecuteRequest;
import com.paywith.payment.dto.ExecuteResponse;
import com.paywith.payment.service.PaymentExecuteService;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 가맹점 스캐너(비로그인 제3 액터)의 결제 실행 API — 60초 1회용 qrToken이
 * 사실상 자격증명이라 인증 주체 없이 동작한다(permitAll).
 * SecurityConfig에 무토큰 예외가 등록돼 있다.
 */
@RestController
@RequestMapping("/api/payments/execute")
public class PaymentExecuteController {

    private final PaymentExecuteService paymentExecuteService;

    public PaymentExecuteController(PaymentExecuteService paymentExecuteService) {
        this.paymentExecuteService = paymentExecuteService;
    }

    @PostMapping
    public ApiResponse<ExecuteResponse> execute(@Valid @RequestBody ExecuteRequest request) {
        return ApiResponse.success(paymentExecuteService.execute(request));
    }
}
