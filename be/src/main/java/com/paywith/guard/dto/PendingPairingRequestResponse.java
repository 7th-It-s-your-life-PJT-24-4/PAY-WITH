package com.paywith.guard.dto;

import lombok.Getter;

@Getter
public class PendingPairingRequestResponse {
    private final String requestId;
    private final String wardName;
    private final String wardPhoneMasked;

    public PendingPairingRequestResponse(String requestId, String wardName, String wardPhoneMasked) {
        this.requestId = requestId;
        this.wardName = wardName;
        this.wardPhoneMasked = wardPhoneMasked;
    }
}