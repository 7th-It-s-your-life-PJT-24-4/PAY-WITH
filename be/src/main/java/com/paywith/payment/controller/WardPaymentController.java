package com.paywith.payment.controller;

import com.paywith.common.ApiResponse;
import com.paywith.exception.BusinessException;
import com.paywith.payment.dto.PaymentCancelResponse;
import com.paywith.payment.dto.PaymentStatusResponse;
import com.paywith.payment.dto.QrCreateRequest;
import com.paywith.payment.dto.QrCreateResponse;
import com.paywith.payment.service.PaymentService;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ward/payments")
public class WardPaymentController {

    private final PaymentService paymentService;

    public WardPaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<QrCreateResponse> createQr(
        @Valid @RequestBody QrCreateRequest request,
        Authentication authentication
    ) {
        return ApiResponse.success(paymentService.createQr(currentUserId(authentication), request.getPin()));
    }

    @GetMapping("/{id}")
    public ApiResponse<PaymentStatusResponse> getStatus(
        @PathVariable("id") String id,
        Authentication authentication
    ) {
        return ApiResponse.success(paymentService.getStatus(currentUserId(authentication), parsePaymentId(id)));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<PaymentCancelResponse> cancel(
        @PathVariable("id") String id,
        Authentication authentication
    ) {
        return ApiResponse.success(paymentService.cancel(currentUserId(authentication), parsePaymentId(id)));
    }

    private Long currentUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }

    /** 숫자가 아닌 id는 명세대로 400으로 응답 (@PathVariable Long의 타입 불일치는 500으로 새기 때문) */
    private Long parsePaymentId(String id) {
        try {
            long paymentId = Long.parseLong(id);
            if (paymentId <= 0) {
                throw new NumberFormatException();
            }
            return paymentId;
        } catch (NumberFormatException exception) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "결제 요청 번호가 올바르지 않습니다.");
        }
    }
}
