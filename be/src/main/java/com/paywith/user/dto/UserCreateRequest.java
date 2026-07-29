package com.paywith.user.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserCreateRequest {

    @NotBlank
    private String role;

    @NotBlank
    private String phone;

    @NotBlank
    private String password;

    @NotBlank
    private String name;

    @NotBlank
    @Pattern(regexp = "\\d{8}", message = "생년월일은 YYYYMMDD 8자리여야 합니다.")
    private String birthDate;

    @NotBlank
    @Pattern(regexp = "[남여]", message = "gender는 남 또는 여여야 합니다.")
    private String gender;

    @Pattern(regexp = "\\d{6}", message = "결제 비밀번호는 숫자 6자리여야 합니다.")
    private String paymentPassword;
}
