package com.paywith.guard.controller;

import com.paywith.common.ApiResponse;
import com.paywith.guard.dto.GuardHomeResponse;
import com.paywith.guard.service.GuardHomeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

@Api(tags = "보호자 홈")
@RestController
@RequestMapping("/api/guard")
public class GuardHomeController {

    private final GuardHomeService guardHomeService;

    public GuardHomeController(GuardHomeService guardHomeService) {
        this.guardHomeService = guardHomeService;
    }

    @ApiOperation(
        value = "보호자 홈 조회",
        notes = "연결된 피보호자 탭 목록과, 선택된 피보호자의 잔액/승인대기건/최근거래를 함께 조회한다. "
            + "wardId를 생략하면 가장 먼저 연동된 피보호자가 기본 선택된다. 연동되지 않은 wardId를 넘기면 403.")
    @GetMapping
    public ApiResponse<GuardHomeResponse> getHome(
        @ApiIgnore @AuthenticationPrincipal Long guardId,
        @ApiParam(value = "조회할 피보호자 id. 생략하면 첫 번째 연동 피보호자", example = "1")
        @RequestParam(required = false) Long wardId
    ) {
        return ApiResponse.success(guardHomeService.getHome(guardId, wardId));
    }
}