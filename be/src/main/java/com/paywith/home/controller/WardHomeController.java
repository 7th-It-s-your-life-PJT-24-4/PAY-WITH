package com.paywith.home.controller;

import com.paywith.common.ApiResponse;
import com.paywith.home.dto.WardHomeResponse;
import com.paywith.home.service.WardHomeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

@Api(tags = "피보호자 홈")
@RestController
@RequestMapping("/api/ward/home")
@RequiredArgsConstructor
public class WardHomeController {

    private final WardHomeService wardHomeService;

    /**
     * 홈 화면 진입 시 한 번 호출한다. 대상을 파라미터로 받지 않고 인증 주체로만 정한다.
     */
    @ApiOperation(
        value = "피보호자 홈 화면 조회",
        notes = "로그인한 피보호자의 이름, 지갑 잔액, 승인 대기 중인 송금 목록을 한 번에 반환한다. "
            + "대상을 파라미터로 받지 않고 인증 주체로만 정하므로 다른 사람의 정보는 조회할 수 없다. "
            + "검사 순서는 역할 → 지갑 → 페어링이다. 지갑은 피보호자에게만 생성되므로 보호자 계정은 "
            + "403(AUTH_004 \"피보호자만 접근할 수 있습니다.\"), 지갑이 없으면 404(WALLET_001), "
            + "ACTIVE 페어링이 없으면 403(WARD_001 \"페어링 완료 후 이용할 수 있습니다.\"). "
            + "승인 대기 목록은 PENDING 이면서 아직 만료되지 않은 건을 건수 제한 없이 만료가 임박한 순(expiredAt 오름차순)으로 "
            + "반환하며, 결제는 아직 승인 대기가 생기지 않아 송금(type=TRANSFER_OUT)만 나온다. "
            + "시각 필드는 오프셋 없는 ISO-8601(예: 2026-07-16T15:30:00).")
    @GetMapping
    public ApiResponse<WardHomeResponse> findMyHome(
        @ApiIgnore @AuthenticationPrincipal Long userId
    ) {
        return ApiResponse.success(wardHomeService.findMyHome(userId));
    }
}
