package com.paywith.payment.dto;

import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExecuteRequest {

    /** QR에서 읽은 60초 1회용 결제 토큰 */
    @ApiModelProperty(value = "QR에서 읽은 60초 1회용 결제 토큰(\"pay_qr_\" + 영숫자 16자). 공백이면 400 REQUEST_001",
        required = true, example = "pay_qr_a8F2kL9xQ1mNz7Rt")
    @NotBlank(message = "QR 토큰을 입력해주세요.")
    private String qrToken;

    /** 결제 가맹점 번호 — 좌표 등 가맹점 정보는 서버 조회값만 사용(클라이언트 좌표 전달 금지) */
    @ApiModelProperty(value = "결제 가맹점 번호(양의 정수). 누락·0 이하는 400 REQUEST_001, 미존재·좌표 미등록은 404 MERCHANT_001",
        required = true, example = "3")
    @NotNull(message = "가맹점 번호를 입력해주세요.")
    @Positive(message = "가맹점 번호가 올바르지 않습니다.")
    private Long merchantId;

    @ApiModelProperty(value = "결제 금액(원, 1 이상). 누락·0 이하는 400 REQUEST_001. 숫자가 아닌 문자열은 500",
        required = true, example = "15000")
    @NotNull(message = "결제 금액을 입력해주세요.")
    @Positive(message = "결제 금액은 1원 이상이어야 합니다.")
    private Long amount;
}
