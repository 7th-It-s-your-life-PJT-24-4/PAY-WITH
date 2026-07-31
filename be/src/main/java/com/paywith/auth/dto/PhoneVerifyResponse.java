package com.paywith.auth.dto;

import lombok.Getter;

@Getter
public class PhoneVerifyResponse {

    private final String verificationToken;

    public PhoneVerifyResponse(String verificationToken) {
        this.verificationToken = verificationToken;
    }
}
