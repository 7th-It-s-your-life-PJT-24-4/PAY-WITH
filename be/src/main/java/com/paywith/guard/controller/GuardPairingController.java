package com.paywith.guard.controller;

import com.paywith.common.ApiResponse;
import com.paywith.guard.dto.GuardPairingCodeResponse;
import com.paywith.guard.service.GuardService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/guard/pairing")
public class GuardPairingController {

    private final GuardService guardService;

    public GuardPairingController(GuardService guardService) {
        this.guardService = guardService;
    }

    @PostMapping
    public ApiResponse<GuardPairingCodeResponse> issueCode(@AuthenticationPrincipal Long guardId) {
        return ApiResponse.success(guardService.issuePairingCode(guardId));
    }

    @DeleteMapping("/{wardId}")
    public ApiResponse<Void> unpairWard(
        @AuthenticationPrincipal Long guardId,
        @PathVariable Long wardId
    ) {
        guardService.unpairWard(guardId, wardId);
        return ApiResponse.success(null);
    }
}
