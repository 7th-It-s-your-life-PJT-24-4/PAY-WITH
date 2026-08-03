package com.paywith.guard.dto;

import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class GuardPairingCodeResponse {

    private final String code;
    private final String inviteUrl;
    private final LocalDateTime expiresAt;

    public GuardPairingCodeResponse(String code, String inviteUrl, LocalDateTime expiresAt) {
        this.code = code;
        this.inviteUrl = inviteUrl;
        this.expiresAt = expiresAt;
    }
}