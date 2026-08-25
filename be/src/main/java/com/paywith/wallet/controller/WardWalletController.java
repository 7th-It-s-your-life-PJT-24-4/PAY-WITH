package com.paywith.wallet.controller;

import com.paywith.common.ApiResponse;
import com.paywith.wallet.dto.WalletBalanceResponse;
import com.paywith.wallet.service.WalletService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

@Api(tags = "피보호자 지갑")
@RestController
@RequestMapping("/api/ward/wallet")
@RequiredArgsConstructor
public class WardWalletController {

    private final WalletService walletService;

    /**
     * 본인 지갑 잔액 조회. 대상 지갑을 경로나 파라미터로 받지 않고 인증 주체로만 정한다.
     */
    @ApiOperation(
        value = "내 지갑 잔액 조회",
        notes = "로그인한 피보호자 본인의 지갑 잔액을 반환한다. 대상 지갑을 파라미터로 받지 않고 "
            + "인증 주체로만 정하므로 다른 사람의 지갑은 조회할 수 없다. "
            + "지갑은 피보호자에게만 생성되므로 보호자 계정은 403 AUTH_004, 지갑 행이 없으면 404 WALLET_001, "
            + "토큰은 유효하나 사용자 행이 없으면 404(code 없음) \"사용자를 찾을 수 없습니다.\".")
    @GetMapping
    public ApiResponse<WalletBalanceResponse> getMyBalance(
        @ApiIgnore @AuthenticationPrincipal Long userId
    ) {
        return ApiResponse.success(walletService.findMyBalance(userId));
    }
}
