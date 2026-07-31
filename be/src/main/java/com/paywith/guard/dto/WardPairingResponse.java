package com.paywith.guard.dto;

import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class WardPairingResponse {

    private final Long relationId;
    private final Long guardId;
    private final String guardName;
    private final String status;
    private final LocalDateTime connectedAt;

    public WardPairingResponse(Long relationId, Long guardId, String guardName, String status, LocalDateTime connectedAt) {
        this.relationId = relationId;
        this.guardId = guardId;
        this.guardName = guardName;
        this.status = status;
        this.connectedAt = connectedAt;
    }
}