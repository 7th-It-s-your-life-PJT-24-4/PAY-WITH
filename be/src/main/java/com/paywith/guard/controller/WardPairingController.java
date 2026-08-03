package com.paywith.guard.controller;

import com.paywith.common.ApiResponse;
import com.paywith.guard.dto.WardPairingRequest;
import com.paywith.guard.dto.WardPairingResponse;
import com.paywith.guard.service.GuardService;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ward/pairing")
public class WardPairingController {

    private final GuardService guardService;

    public WardPairingController(GuardService guardService) {
        this.guardService = guardService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<WardPairingResponse> pair(
        @AuthenticationPrincipal Long wardId,
        @Valid @RequestBody WardPairingRequest request
    ) {
        return ApiResponse.success(guardService.pairWithCode(wardId, request.getPairingCode()));
    }
}