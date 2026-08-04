package com.paywith.guard.dto;

import lombok.Getter;

@Getter
public class WardTabResponse {

    private final Long wardId;
    private final String name;
    private final boolean hasPending;

    public WardTabResponse(Long wardId, String name, boolean hasPending) {
        this.wardId = wardId;
        this.name = name;
        this.hasPending = hasPending;
    }
}