package com.paywith.payment.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;

@ApiModel(description = "결제 QR 생성 결과(201). qrToken을 QR로 표시하고 paymentId로 상태를 폴링한다")
@Getter
public class QrCreateResponse {

    @ApiModelProperty(value = "결제 요청 번호(payment_requests.payment_id). 상태 조회·취소 경로의 {id}로 사용", example = "42")
    private final Long paymentId;

    @ApiModelProperty(value = "60초 1회용 결제 토큰(\"pay_qr_\" + 영숫자 16자, 총 23자). 스캐너가 결제 실행 요청의 qrToken으로 보낸다",
        example = "pay_qr_a8F2kL9xQ1mNz7Rt")
    private final String qrToken;

    @ApiModelProperty(value = "현재 지갑 잔액(원, wallets.balance) 참고값. 잔액은 검증하지 않으므로 0원이어도 QR은 발급된다",
        example = "152000")
    private final Long availableBalance;

    @ApiModelProperty(value = "토큰 만료 시각(생성 시각 + 60초, ISO 8601 +09:00 오프셋). 서버 시각 기반이라 분수초(최대 마이크로초)가 "
        + "붙을 수 있고, 이후 상태 조회의 expiresAt은 DB DATETIME 기반이라 분수초 없이 내려간다(같은 건이라도 표기가 최대 1초 다를 수 있음)",
        example = "2026-08-25T10:01:00.123456+09:00")
    private final String expiresAt;

    @ApiModelProperty(value = "토큰 유효 시간(초). 항상 60", example = "60")
    private final int expiresInSeconds;

    public QrCreateResponse(
        Long paymentId,
        String qrToken,
        Long availableBalance,
        String expiresAt,
        int expiresInSeconds
    ) {
        this.paymentId = paymentId;
        this.qrToken = qrToken;
        this.availableBalance = availableBalance;
        this.expiresAt = expiresAt;
        this.expiresInSeconds = expiresInSeconds;
    }
}
