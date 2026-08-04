package com.paywith.auth.dto;

import javax.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordResetRequest {

    @NotBlank
    private String phone;

    @NotBlank
    private String verificationToken;

    @NotBlank
    private String newPassword;
}