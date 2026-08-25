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
            + "GUARD 역할이 아니면 403(AUTH_004 \"접근 권한이 없습니다.\"). "
            + "wardId 를 생략하면 가장 먼저 연동된 피보호자(connected_at 오름차순)가 기본 선택되고, "
            + "연동된 피보호자가 없으면 wards 는 빈 배열, selectedWard 는 null. "
            + "연동되지 않은 wardId 를 넘기면 404(LINK_001). 비숫자 wardId 는 500. "
            + "selectedWard.pendingApprovals 는 만료 임박순 최대 3건, pendingApprovalCount 는 제한 전 전체 건수. "
            + "recentTransactions 는 종결 상태(COMPLETED·REJECTED·CANCELED)만 최신순 3건이며 보호자 대리 충전도 포함된다. "
            + "지갑이 없으면 balance 0. 시각 필드는 오프셋 없는 ISO-8601.")
    @GetMapping
    public ApiResponse<GuardHomeResponse> getHome(
        @ApiIgnore @AuthenticationPrincipal Long guardId,
        @ApiParam(value = "조회할 피보호자 id. 생략하면 첫 번째 연동 피보호자", example = "1")
        @RequestParam(required = false) Long wardId
    ) {
        return ApiResponse.success(guardHomeService.getHome(guardId, wardId));
    }
}