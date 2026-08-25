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
import springfox.documentation.annotations.ApiIgnore;
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
        notes = "결제 비밀번호(users.pin) 확인 후 60초 유효한 1회용 결제 토큰을 발급한다. "
            + "검증 순서는 역할(WARD) → 페어링(ACTIVE) → 지갑 존재 → 지갑 LOCKED → 비밀번호 순이며, "
            + "잔액은 검증하지 않는다(availableBalance는 현재 잔액 참고값 — 0원이어도 QR은 발급된다). "
            + "qrToken은 \"pay_qr_\" + 영숫자 16자(총 23자), expiresInSeconds는 항상 60. "
            + "expiresAt은 ISO 8601 +09:00 형식으로 서버 시각 기반이라 분수초(최대 마이크로초)가 붙을 수 있고, "
            + "이후 상태 조회의 expiresAt은 DB DATETIME 기반이라 분수초 없이 내려간다(같은 건이라도 표기가 최대 1초 다를 수 있음). "
            + "pin 누락·숫자 6자리가 아니면 400 REQUEST_001이며 message는 \"pin: <검증 메시지>\" 형식. "
            + "본문 없음·JSON 파싱 실패는 500(code 없음, \"서버 오류가 발생했습니다.\").")
    // io.swagger의 ApiResponse는 공용 응답 래퍼(common.ApiResponse)와 이름이 겹쳐 전체 경로로 쓴다.
    @ApiResponses({
        @io.swagger.annotations.ApiResponse(code = 400,
            message = "REQUEST_001 요청 검증 실패(pin 누락·숫자 6자리 아님, message \"pin: <검증 메시지>\") / "
                + "PAYMENT_005 결제 비밀번호 불일치 \"결제 비밀번호가 올바르지 않습니다.\""),
        @io.swagger.annotations.ApiResponse(code = 401,
            message = "AUTH_001 인증 필요(토큰 없음·무효) / AUTH_002 인증 만료"),
        @io.swagger.annotations.ApiResponse(code = 403,
            message = "AUTH_004 피보호자 아님 \"피보호자만 접근할 수 있습니다.\" / "
                + "WARD_001 페어링 미완료 \"페어링 완료 후 이용할 수 있습니다.\""),
        @io.swagger.annotations.ApiResponse(code = 404,
            message = "WALLET_001 지갑 없음 \"지갑 정보를 찾을 수 없습니다.\""),
        @io.swagger.annotations.ApiResponse(code = 409,
            message = "WALLET_002 거래가 제한된 지갑(status LOCKED) \"현재 거래가 제한된 지갑입니다.\"")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<QrCreateResponse> createQr(
        @Valid @RequestBody QrCreateRequest request,
        @ApiIgnore Authentication authentication
    ) {
        return ApiResponse.success(paymentService.createQr(currentUserId(authentication), request.getPin()));
    }

    @ApiOperation(
        value = "결제 상태 조회 (폴링)",
        notes = "status는 PENDING·PROCESSING·COMPLETED·FAILED·EXPIRED·CANCELED 6종. "
            + "PENDING이면서 만료 시각이 지난 건은 조회 시점에 EXPIRED로 전이된다. "
            + "실패 건은 failureCode(INSUFFICIENT_BALANCE·FDS_BLOCKED)와 failureMessage를 함께 준다 — "
            + "INSUFFICIENT_BALANCE는 \"결제 가능한 잔액이 부족합니다.\", FDS_BLOCKED는 \"결제에 실패했습니다.\"(범용 문구, "
            + "결제 실행 403의 차단 문구와 다름). FAILED 건은 merchantName·amount는 채워지고 transactionId·remainingBalance는 null. "
            + "COMPLETED 건만 transactionId·paidAt·remainingBalance가 채워진다. "
            + "paidAt·expiresAt은 ISO 8601 +09:00 형식이며 DB DATETIME 기반이라 분수초 없음. "
            + "페어링 여부는 검사하지 않는다. id가 숫자가 아니거나 0 이하이면 400 PAYMENT_001.")
    @ApiResponses({
        @io.swagger.annotations.ApiResponse(code = 400,
            message = "PAYMENT_001 결제 요청 번호가 올바르지 않음(비숫자·0 이하) \"결제 요청 번호가 올바르지 않습니다.\""),
        @io.swagger.annotations.ApiResponse(code = 401,
            message = "AUTH_001 인증 필요(토큰 없음·무효) / AUTH_002 인증 만료"),
        @io.swagger.annotations.ApiResponse(code = 403,
            message = "AUTH_004 피보호자 아님 \"피보호자만 접근할 수 있습니다.\""),
        @io.swagger.annotations.ApiResponse(code = 404,
            message = "PAYMENT_002 결제 요청 없음(타인 건 포함) \"결제 요청을 찾을 수 없습니다.\"")
    })
    @GetMapping("/{id}")
    public ApiResponse<PaymentStatusResponse> getStatus(
        @ApiParam(value = "결제 요청 번호(양의 정수, 비숫자·0 이하는 400 PAYMENT_001)", required = true, example = "42")
        @PathVariable("id") String id,
        @ApiIgnore Authentication authentication
    ) {
        return ApiResponse.success(paymentService.getStatus(currentUserId(authentication), parsePaymentId(id)));
    }

    @ApiOperation(
        value = "결제 요청 취소",
        notes = "PENDING만 취소할 수 있다. 이미 취소된 건의 재취소는 409가 아니라 200으로 같은 응답을 재반환한다"
            + "(멱등, canceledAt은 최초 취소 시각). 취소 성공 시 Redis의 QR 토큰도 즉시 삭제된다. "
            + "PROCESSING·COMPLETED뿐 아니라 FAILED(잔액 부족·FDS 차단으로 종결된 건)도 409 PAYMENT_003이며, "
            + "PENDING이지만 만료 시각이 지난 건은 조회 시점에 EXPIRED로 전이된 뒤 409 PAYMENT_004. "
            + "canceledAt은 ISO 8601 +09:00 형식으로 DB updated_at 기반이라 분수초 없음. "
            + "페어링 여부는 검사하지 않는다. id가 숫자가 아니거나 0 이하이면 400 PAYMENT_001.")
    @ApiResponses({
        @io.swagger.annotations.ApiResponse(code = 400,
            message = "PAYMENT_001 결제 요청 번호가 올바르지 않음(비숫자·0 이하) \"결제 요청 번호가 올바르지 않습니다.\""),
        @io.swagger.annotations.ApiResponse(code = 401,
            message = "AUTH_001 인증 필요(토큰 없음·무효) / AUTH_002 인증 만료"),
        @io.swagger.annotations.ApiResponse(code = 403,
            message = "AUTH_004 피보호자 아님 \"피보호자만 접근할 수 있습니다.\""),
        @io.swagger.annotations.ApiResponse(code = 404,
            message = "PAYMENT_002 결제 요청 없음(타인 건 포함) \"결제 요청을 찾을 수 없습니다.\""),
        @io.swagger.annotations.ApiResponse(code = 409,
            message = "PAYMENT_003 PROCESSING·COMPLETED·FAILED 건 \"이미 처리 중이거나 완료된 결제는 취소할 수 없습니다.\" / "
                + "PAYMENT_004 EXPIRED 건(조회 시점 만료 전이 포함) \"이미 만료된 결제 요청입니다.\"")
    })
    @PostMapping("/{id}/cancel")
    public ApiResponse<PaymentCancelResponse> cancel(
        @ApiParam(value = "취소할 결제 요청 번호(양의 정수, 비숫자·0 이하는 400 PAYMENT_001)", required = true, example = "42")
        @PathVariable("id") String id,
        @ApiIgnore Authentication authentication
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
