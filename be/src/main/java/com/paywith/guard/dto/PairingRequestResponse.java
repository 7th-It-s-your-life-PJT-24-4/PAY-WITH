package com.paywith.guard.dto;

import lombok.Getter;

@Getter
public class PairingRequestResponse {
    private final String requestId;

    public PairingRequestResponse(String requestId) {
        this.requestId = requestId;
    }
}