package com.paywith.payment.dto;

import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QrCreateRequest {

    /** 결제 비밀번호 — 송금 비밀번호와 동일한 users.pin(BCrypt) 대조 (필드명 pin — 송금·결제 통일) */
    @ApiModelProperty(value = "결제 비밀번호(숫자 6자리, users.pin과 BCrypt 대조). 누락·형식 오류는 400 REQUEST_001, 불일치는 400 PAYMENT_005",
        required = true, example = "123456")
    @NotBlank(message = "결제 비밀번호를 입력해주세요.")
    @Pattern(regexp = "\\d{6}", message = "결제 비밀번호는 숫자 6자리여야 합니다.")
    private String pin;
}
