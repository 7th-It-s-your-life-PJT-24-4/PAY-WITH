package com.paywith.payment.controller;

import com.paywith.common.ApiResponse;
import com.paywith.exception.BusinessException;
import com.paywith.payment.dto.PaymentCancelResponse;
import com.paywith.payment.dto.PaymentStatusResponse;
import com.paywith.payment.dto.QrCreateRequest;
import com.paywith.payment.dto.QrCreateResponse;
import com.paywith.payment.service.PaymentService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponses;
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

@Api(tags = "피보호자 결제")
@RestController
@RequestMapping("/api/ward/payments")
public class WardPaymentController {

    private final PaymentService paymentService;

    public WardPaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @ApiOperation(
        value = "결제 QR 생성",
        notes = "결제 비밀번호(users.pin) 확인 후 60초 유효한 1회용 결제 토큰을 발급한다.")
    // io.swagger의 ApiResponse는 공용 응답 래퍼(common.ApiResponse)와 이름이 겹쳐 전체 경로로 쓴다.
    @ApiResponses({
        @io.swagger.annotations.ApiResponse(code = 400,
            message = "REQUEST_001 요청 형식 오류 / PAYMENT_005 결제 비밀번호 불일치"),
        @io.swagger.annotations.ApiResponse(code = 401,
            message = "AUTH_001 인증 필요 / AUTH_002 인증 만료"),
        @io.swagger.annotations.ApiResponse(code = 403,
            message = "AUTH_004 피보호자 아님 / WARD_001 페어링 미완료"),
        @io.swagger.annotations.ApiResponse(code = 404,
            message = "WALLET_001 지갑 없음"),
        @io.swagger.annotations.ApiResponse(code = 409,
            message = "WALLET_002 거래가 제한된 지갑")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<QrCreateResponse> createQr(
        @Valid @RequestBody QrCreateRequest request,
        Authentication authentication
    ) {
        return ApiResponse.success(paymentService.createQr(currentUserId(authentication), request.getPin()));
    }

    @ApiOperation(
        value = "결제 상태 조회 (폴링)",
        notes = "status는 PENDING·PROCESSING·COMPLETED·FAILED·EXPIRED·CANCELED 6종. "
            + "PENDING이면서 만료 시각이 지난 건은 조회 시점에 EXPIRED로 전이된다. "
            + "실패 건은 failureCode(INSUFFICIENT_BALANCE·FDS_BLOCKED)를 함께 준다.")
    @ApiResponses({
        @io.swagger.annotations.ApiResponse(code = 400,
            message = "PAYMENT_001 결제 요청 번호가 올바르지 않음"),
        @io.swagger.annotations.ApiResponse(code = 401,
            message = "AUTH_001 인증 필요 / AUTH_002 인증 만료"),
        @io.swagger.annotations.ApiResponse(code = 403,
            message = "AUTH_004 피보호자 아님"),
        @io.swagger.annotations.ApiResponse(code = 404,
            message = "PAYMENT_002 결제 요청 없음(타인 건 포함)")
    })
    @GetMapping("/{id}")
    public ApiResponse<PaymentStatusResponse> getStatus(
        @ApiParam(value = "결제 요청 번호", required = true, example = "42")
        @PathVariable("id") String id,
        Authentication authentication
    ) {
        return ApiResponse.success(paymentService.getStatus(currentUserId(authentication), parsePaymentId(id)));
    }

    @ApiOperation(
        value = "결제 요청 취소",
        notes = "PENDING만 취소할 수 있다. 이미 취소된 건의 재취소는 같은 응답을 재반환한다(멱등).")
    @ApiResponses({
        @io.swagger.annotations.ApiResponse(code = 400,
            message = "PAYMENT_001 결제 요청 번호가 올바르지 않음"),
        @io.swagger.annotations.ApiResponse(code = 401,
            message = "AUTH_001 인증 필요 / AUTH_002 인증 만료"),
        @io.swagger.annotations.ApiResponse(code = 403,
            message = "AUTH_004 피보호자 아님"),
        @io.swagger.annotations.ApiResponse(code = 404,
            message = "PAYMENT_002 결제 요청 없음(타인 건 포함)"),
        @io.swagger.annotations.ApiResponse(code = 409,
            message = "PAYMENT_003 처리 중·완료된 결제 / PAYMENT_004 이미 만료된 요청")
    })
    @PostMapping("/{id}/cancel")
    public ApiResponse<PaymentCancelResponse> cancel(
        @ApiParam(value = "취소할 결제 요청 번호", required = true, example = "42")
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
            throw new BusinessException(HttpStatus.BAD_REQUEST, "PAYMENT_001", "결제 요청 번호가 올바르지 않습니다.");
        }
    }
}
