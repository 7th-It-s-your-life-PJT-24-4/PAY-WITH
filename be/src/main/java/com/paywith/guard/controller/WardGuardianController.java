package com.paywith.guard.controller;

import com.paywith.common.ApiResponse;
import com.paywith.guard.dto.GuardInfoResponse;
import com.paywith.guard.service.GuardService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

@Api(tags = "피보호자 페어링")
@RestController
@RequestMapping("/api/ward/guardian")
public class WardGuardianController {

    private final GuardService guardService;

    public WardGuardianController(GuardService guardService) {
        this.guardService = guardService;
    }

    @ApiOperation(
        value = "보호자 정보 조회",
        notes = "시니어가 자신과 연동된 보호자의 이름·전화번호·아바타를 조회한다. 연동된 보호자가 없으면 "
            + "403 WARD_001. WARD가 아니면 403 AUTH_004.")
    @GetMapping
    public ApiResponse<GuardInfoResponse> getMyGuardian(@ApiIgnore @AuthenticationPrincipal Long wardId) {
        return ApiResponse.success(guardService.findMyGuardian(wardId));
    }
}