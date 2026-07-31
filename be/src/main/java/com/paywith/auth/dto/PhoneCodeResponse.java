package com.paywith.auth.dto;

import lombok.Getter;

@Getter
public class PhoneCodeResponse {

    private final int expireIn;

    public PhoneCodeResponse(int expireIn) {
        this.expireIn = expireIn;
    }
}