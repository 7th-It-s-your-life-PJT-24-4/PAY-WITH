package com.paywith.guard.dto;

import lombok.Getter;

@Getter
public class WardTabResponse {

    private final Long wardId;
    private final String name;
    private final Integer avatarId;
    private final boolean hasPending;

    public WardTabResponse(Long wardId, String name, Integer avatarId, boolean hasPending) {
        this.wardId = wardId;
        this.name = name;
        this.avatarId = avatarId;
        this.hasPending = hasPending;
    }
}
