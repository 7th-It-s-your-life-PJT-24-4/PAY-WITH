package com.paywith.auth.dto;

import javax.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PhoneVerifyRequest {

    @NotBlank
    private String phone;

    @NotBlank
    private String code;
}
