package com.paywith.guard.controller;

import com.paywith.common.ApiResponse;
import com.paywith.guard.dto.GuardPairingCodeResponse;
import com.paywith.guard.dto.PendingPairingRequestResponse;
import com.paywith.guard.dto.WardPairingResponse;
import com.paywith.guard.service.GuardService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
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
        notes = "보호자가 5자리 인증코드와 초대링크({invite-base-url}/{code})를 발급받는다. 발급 후 5분간 유효하며"
            + "(expiresAt은 초 단위), 재발급하면 이전 코드는 즉시 무효화된다. GUARD가 아니면 403 AUTH_004.")
    @PostMapping
    public ApiResponse<GuardPairingCodeResponse> issueCode(@ApiIgnore @AuthenticationPrincipal Long guardId) {
        return ApiResponse.success(guardService.issuePairingCode(guardId));
    }

    @ApiOperation(
            value = "대기중인 확인 요청 조회",
            notes = "나에게 온 확정 대기중(PENDING) 요청이 있으면 반환한다. 요청이 없거나, 생성 후 2분이 지나 만료됐거나, "
                    + "이미 승인돼 PENDING이 아니면 에러 없이 data가 null. GUARD가 아니면 403 AUTH_004. 2초 간격 폴링 권장.")
    @GetMapping("/pending-request")
    public ApiResponse<PendingPairingRequestResponse> getPendingRequest(
            @ApiIgnore @AuthenticationPrincipal Long guardId
    ) {
        return ApiResponse.success(guardService.findPendingRequest(guardId));
    }

    @ApiOperation(
            value = "확인 요청 승인",
            notes = "팝업으로 상대 피보호자 정보를 확인한 뒤 호출한다. 여기서 실제 guard_senior 연동이 ACTIVE로 확정되며, "
                    + "응답 구조는 피보호자 즉시 연결과 같다. 생성 후 2분이 지나 만료됐거나 없는 요청이면 400 PAIRING_007, "
                    + "본인에게 온 요청이 아니면 403 PAIRING_008, 이미 처리된 요청이면 409 PAIRING_009"
                    + "(승인 후 30초 동안만 CONFIRMED로 남고 그 뒤에는 400 PAIRING_007). GUARD가 아니면 403 AUTH_004.")
    @PostMapping("/request/{requestId}/confirm")
    public ApiResponse<WardPairingResponse> confirmRequest(
            @ApiIgnore @AuthenticationPrincipal Long guardId,
            @ApiParam(value = "대기중인 확인 요청 조회로 받은 requestId(UUID)", required = true,
                    example = "3f29c1e0-8b7a-4c2d-9e1f-5a6b7c8d9e0f")
            @PathVariable String requestId
    ) {
        return ApiResponse.success(guardService.confirmPairingRequest(guardId, requestId));
    }

    @ApiOperation(
        value = "페어링 해제",
        notes = "보호자가 피보호자와의 연결을 해제한다(REVOKED 소프트 삭제, 재페어링하면 ACTIVE로 재활성화). "
            + "ACTIVE 연결이 없으면(연동된 적 없거나 이미 해제) 404 PAIRING_005 — 같은 요청을 두 번 보내면 두 번째는 404(멱등 아님). "
            + "GUARD가 아니면 403 AUTH_004. 비숫자 wardId는 500.")
    @DeleteMapping("/{wardId}")
    public ApiResponse<Void> unpairWard(
        @ApiIgnore @AuthenticationPrincipal Long guardId,
        @ApiParam(value = "연결을 해제할 피보호자 ID. 1 이상 여부는 서버 검증 없음(0·음수도 ACTIVE 연결이 없어 404 PAIRING_005)",
            required = true, example = "1")
        @PathVariable Long wardId
    ) {
        guardService.unpairWard(guardId, wardId);
        return ApiResponse.success(null);
    }
}
