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
        notes = "스캐너가 읽은 qrToken으로 결제를 실행한다(인증 불필요, permitAll). 같은 토큰 재요청은 최초 처리 결과를 "
            + "그대로 재응답한다(멱등) — 완료 건은 200 동일 본문, 잔액 부족 건은 422 WALLET_003, FDS 차단 건은 403 PAYMENT_006, "
            + "그 외(PROCESSING·EXPIRED·CANCELED·만료된 PENDING·미존재 토큰)는 400 PAY_001. "
            + "응답 status는 항상 \"COMPLETED\". createdAt은 ISO 8601 +09:00 형식이며 최초 완료 응답은 서버 시각 기반이라 "
            + "분수초(최대 마이크로초, 예: 2026-08-25T10:00:00.123456+09:00)가 붙을 수 있고, 멱등 재응답은 DB DATETIME 기반이라 "
            + "분수초 없이 초 단위로 내려간다. "
            + "요청 검증 실패(qrToken 공백, merchantId·amount 누락 또는 0 이하)는 400 REQUEST_001이며 message는 "
            + "\"<필드>: <검증 메시지>\" 형식(예: \"amount: 결제 금액은 1원 이상이어야 합니다.\"). "
            + "JSON 타입 불일치(예: amount에 문자열)·본문 파싱 실패는 400이 아니라 500(code 없음, \"서버 오류가 발생했습니다.\"). "
            + "가맹점 검증(404)은 토큰 상태 최종 확인보다 먼저 수행된다(이미 종결된 토큰의 재요청은 예외).")
    // io.swagger의 ApiResponse는 공용 응답 래퍼(common.ApiResponse)와 이름이 겹쳐 전체 경로로 쓴다.
    @ApiResponses({
        @io.swagger.annotations.ApiResponse(code = 400,
            message = "REQUEST_001 요청 검증 실패(Bean Validation — qrToken 공백·merchantId/amount 누락 또는 0 이하, "
                + "message \"<필드>: <검증 메시지>\") / PAY_001 유효하지 않은 QR 토큰(미존재·만료·이미 사용(PROCESSING)·"
                + "EXPIRED·CANCELED·지갑 불일치) \"유효하지 않은 QR입니다. 새 QR로 다시 시도해주세요.\""),
        @io.swagger.annotations.ApiResponse(code = 403,
            message = "PAYMENT_006 이상거래 차단(FDS DANGER) \"결제가 차단되었습니다. 보호자에게 문의해 주세요.\" — "
                + "잔액 차감 없이 FAILED(FDS_BLOCKED) 기록, 재요청에도 동일 응답(멱등)"),
        @io.swagger.annotations.ApiResponse(code = 404,
            message = "MERCHANT_001 가맹점 없음 \"가맹점을 찾을 수 없습니다.\" / 좌표 미등록 가맹점 \"결제할 수 없는 가맹점입니다.\""),
        @io.swagger.annotations.ApiResponse(code = 422,
            message = "WALLET_003 잔액 부족 \"결제 가능한 잔액이 부족합니다.\" — FAILED(INSUFFICIENT_BALANCE) 기록, "
                + "재요청에도 동일 응답(멱등)")
    })
    @PostMapping
    public ApiResponse<ExecuteResponse> execute(@Valid @RequestBody ExecuteRequest request) {
        return ApiResponse.success(paymentExecuteService.execute(request));
    }
}
