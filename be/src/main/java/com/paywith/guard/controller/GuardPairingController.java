package com.paywith.guard.controller;

import com.paywith.common.ApiResponse;
import com.paywith.guard.dto.GuardPairingCodeResponse;
import com.paywith.guard.service.GuardService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

@Api(tags = "보호자 페어링")
@RestController
@RequestMapping("/api/guard/pairing")
public class GuardPairingController {

    private final GuardService guardService;

    public GuardPairingController(GuardService guardService) {
        this.guardService = guardService;
    }

    @ApiOperation(
        value = "페어링 코드 발급",
        notes = "보호자가 5자리 인증코드와 초대링크를 발급받는다. 5분간 유효하며, 재발급하면 이전 코드는 "
            + "즉시 무효화된다. GUARD가 아니면 403.")
    @PostMapping
    public ApiResponse<GuardPairingCodeResponse> issueCode(@ApiIgnore @AuthenticationPrincipal Long guardId) {
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
