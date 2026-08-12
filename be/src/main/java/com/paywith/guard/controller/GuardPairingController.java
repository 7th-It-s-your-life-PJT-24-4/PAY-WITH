package com.paywith.guard.controller;

import com.paywith.common.ApiResponse;
import com.paywith.guard.dto.GuardPairingCodeResponse;
import com.paywith.guard.dto.PendingPairingRequestResponse;
import com.paywith.guard.dto.WardPairingResponse;
import com.paywith.guard.service.GuardService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
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

    @ApiOperation(
            value = "대기중인 확인 요청 조회",
            notes = "나에게 온 확정 대기중 요청이 있으면 반환한다. 없으면 data가 null. 2초 간격 폴링 권장.")
    @GetMapping("/pending-request")
    public ApiResponse<PendingPairingRequestResponse> getPendingRequest(
            @ApiIgnore @AuthenticationPrincipal Long guardId
    ) {
        return ApiResponse.success(guardService.findPendingRequest(guardId));
    }

    @ApiOperation(
            value = "확인 요청 승인",
            notes = "팝업으로 상대 피보호자 정보를 확인한 뒤 호출한다. 여기서 실제 guard_senior 연동이 확정된다. "
                    + "본인에게 온 요청이 아니면 403, 이미 처리됐거나 만료됐으면 400/409.")
    @PostMapping("/request/{requestId}/confirm")
    public ApiResponse<WardPairingResponse> confirmRequest(
            @ApiIgnore @AuthenticationPrincipal Long guardId,
            @PathVariable String requestId
    ) {
        return ApiResponse.success(guardService.confirmPairingRequest(guardId, requestId));
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
