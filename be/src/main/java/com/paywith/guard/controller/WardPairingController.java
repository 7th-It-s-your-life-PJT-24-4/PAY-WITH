package com.paywith.guard.controller;

import com.paywith.common.ApiResponse;
import com.paywith.guard.dto.PairingRequestResponse;
import com.paywith.guard.dto.PairingStatusResponse;
import com.paywith.guard.dto.WardPairingRequest;
import com.paywith.guard.dto.WardPairingResponse;
import com.paywith.guard.service.GuardService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

@Api(tags = "피보호자 페어링")
@RestController
@RequestMapping("/api/ward/pairing")
public class WardPairingController {

    private final GuardService guardService;

    public WardPairingController(GuardService guardService) {
        this.guardService = guardService;
    }

    @ApiOperation(
        value = "페어링 코드로 연결",
        notes = "피보호자가 보호자로부터 받은 5자리 코드를 입력해 연결한다. 코드가 형식에 안 맞으면 400, "
            + "만료·미발급이면 400, 이미 연결돼 있으면 409, 5회 이상 실패하면 429. WARD가 아니면 403.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<WardPairingResponse> pair(
        @ApiIgnore @AuthenticationPrincipal Long wardId,
        @Valid @RequestBody WardPairingRequest request
    ) {
        return ApiResponse.success(guardService.pairWithCode(wardId, request.getPairingCode()));
    }

    @PostMapping("/request")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PairingRequestResponse> requestPairing(
            @ApiIgnore @AuthenticationPrincipal Long wardId,
            @Valid @RequestBody WardPairingRequest request
    ) {
        return ApiResponse.success(guardService.createPairingRequest(wardId, request.getPairingCode()));
    }

    @GetMapping("/request/{requestId}/status")
    public ApiResponse<PairingStatusResponse> checkStatus(
            @ApiIgnore @AuthenticationPrincipal Long wardId,
            @PathVariable String requestId
    ) {
        return ApiResponse.success(guardService.checkPairingStatus(wardId, requestId));
    }
}