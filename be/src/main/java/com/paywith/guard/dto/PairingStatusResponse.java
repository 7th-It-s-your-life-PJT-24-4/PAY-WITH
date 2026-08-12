package com.paywith.guard.dto;

import lombok.Getter;

@Getter
public class PairingStatusResponse {
    private final String status;

    public PairingStatusResponse(String status) {
        this.status = status;
    }
}