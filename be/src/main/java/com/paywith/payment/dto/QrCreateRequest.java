package com.paywith.payment.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

public class QrCreateRequest {

    /** 결제 비밀번호 — 송금 비밀번호와 동일한 wallets.pin(BCrypt) 대조 (필드명 pin — 송금·결제 통일) */
    @NotBlank(message = "결제 비밀번호를 입력해주세요.")
    @Pattern(regexp = "\\d{6}", message = "결제 비밀번호는 숫자 6자리여야 합니다.")
    private String pin;

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }
}
