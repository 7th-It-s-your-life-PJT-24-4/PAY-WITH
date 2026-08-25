package com.paywith.guard.controller;

import com.paywith.common.ApiResponse;
import com.paywith.guard.dto.PairingRequestResponse;
import com.paywith.guard.dto.PairingStatusResponse;
import com.paywith.guard.dto.WardPairingRequest;
import com.paywith.guard.dto.WardPairingResponse;
import com.paywith.guard.service.GuardService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
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
        notes = "피보호자가 보호자로부터 받은 5자리 코드를 입력해 즉시 ACTIVE로 연결한다(코드는 1회용으로 소진). "
            + "검사 순서: WARD가 아니면 403 AUTH_004 → 코드가 숫자 5자리 형식이 아니거나 누락이면 400 PAIRING_001 → "
            + "실패 5회 누적이면 429 PAIRING_004(카운터는 마지막 실패로부터 5분 뒤 초기화, 초과 상태에서는 올바른 코드도 429) → "
            + "코드가 없거나 발급 5분 초과·이미 사용됐으면 400 PAIRING_002(실패 횟수 +1, 발급자 유효성은 별도 검사하지 않음) → "
            + "이미 연결돼 있으면 409 PAIRING_003. 본문 없음·JSON 파싱 실패는 500.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<WardPairingResponse> pair(
        @ApiIgnore @AuthenticationPrincipal Long wardId,
        @Valid @RequestBody WardPairingRequest request
    ) {
        return ApiResponse.success(guardService.pairWithCode(wardId, request.getPairingCode()));
    }

    @ApiOperation(
            value = "페어링 확인 요청 생성",
            notes = "피보호자가 5자리 코드를 제출해 보호자 승인 대기 요청을 만든다. 성공하면 코드는 소모되고 "
                    + "requestId(UUID)가 발급되며, 요청은 2분간 유효하다(연동은 보호자가 승인해야 확정). "
                    + "검사 순서와 에러는 즉시 연결과 같다: WARD가 아니면 403 AUTH_004, 형식 불일치·누락 400 PAIRING_001, "
                    + "실패 5회 누적 429 PAIRING_004, 코드 없음·발급 5분 초과·이미 사용됨 400 PAIRING_002(실패 횟수 +1), "
                    + "이미 연결돼 있으면 409 PAIRING_003. 본문 없음·JSON 파싱 실패는 500.")
    @PostMapping("/request")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PairingRequestResponse> requestPairing(
            @ApiIgnore @AuthenticationPrincipal Long wardId,
            @Valid @RequestBody WardPairingRequest request
    ) {
        return ApiResponse.success(guardService.createPairingRequest(wardId, request.getPairingCode()));
    }

    @ApiOperation(
            value = "페어링 요청 상태 조회",
            notes = "보호자가 승인했는지 확인한다. status는 PENDING/CONFIRMED/EXPIRED. 생성 후 2분이 지나 만료됐거나 없는 요청은 "
                    + "에러가 아니라 200 + EXPIRED로 응답한다. CONFIRMED는 승인 후 30초 동안만 유지되고 그 뒤에는 EXPIRED. "
                    + "본인 요청이 아니면 403 PAIRING_006. 역할 검사는 없다(GUARD가 호출해도 AUTH_004 아님). 2초 간격 폴링 권장.")
    @GetMapping("/request/{requestId}/status")
    public ApiResponse<PairingStatusResponse> checkStatus(
            @ApiIgnore @AuthenticationPrincipal Long wardId,
            @ApiParam(value = "페어링 확인 요청 생성으로 받은 requestId(UUID)", required = true,
                    example = "3f29c1e0-8b7a-4c2d-9e1f-5a6b7c8d9e0f")
            @PathVariable String requestId
    ) {
        return ApiResponse.success(guardService.checkPairingStatus(wardId, requestId));
    }
}