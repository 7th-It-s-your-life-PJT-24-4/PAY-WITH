package com.paywith.payment.controller;

import com.paywith.common.ApiResponse;
import com.paywith.payment.dto.ExecuteRequest;
import com.paywith.payment.dto.ExecuteResponse;
import com.paywith.payment.service.PaymentExecuteService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponses;
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
@Api(tags = "결제 실행 (가맹점 스캐너)")
@RestController
@RequestMapping("/api/payments/execute")
public class PaymentExecuteController {

    private final PaymentExecuteService paymentExecuteService;

    public PaymentExecuteController(PaymentExecuteService paymentExecuteService) {
        this.paymentExecuteService = paymentExecuteService;
    }

    @ApiOperation(
        value = "QR 결제 실행",
        notes = "스캐너가 읽은 qrToken으로 결제를 실행한다. 같은 토큰 재요청은 최초 처리 결과를 "
            + "그대로 재응답한다(멱등) — 완료 건은 200, 실패 건은 최초와 동일한 오류.")
    // io.swagger의 ApiResponse는 공용 응답 래퍼(common.ApiResponse)와 이름이 겹쳐 전체 경로로 쓴다.
    @ApiResponses({
        @io.swagger.annotations.ApiResponse(code = 400,
            message = "REQUEST_001 요청 형식 오류 / PAY_001 유효하지 않거나 만료된 QR 토큰"),
        @io.swagger.annotations.ApiResponse(code = 403,
            message = "PAYMENT_006 이상거래 차단(FDS) — 재시도에도 동일 응답"),
        @io.swagger.annotations.ApiResponse(code = 404,
            message = "MERCHANT_001 가맹점이 없거나 결제 불가"),
        @io.swagger.annotations.ApiResponse(code = 422,
            message = "WALLET_003 잔액 부족 — 재시도에도 동일 응답")
    })
    @PostMapping
    public ApiResponse<ExecuteResponse> execute(@Valid @RequestBody ExecuteRequest request) {
        return ApiResponse.success(paymentExecuteService.execute(request));
    }
}
