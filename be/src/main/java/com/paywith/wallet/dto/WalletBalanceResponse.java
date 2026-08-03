package com.paywith.wallet.dto;

import com.paywith.wallet.domain.Wallet;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.time.LocalDateTime;
import lombok.Getter;

/**
 * 지갑 잔액 조회 응답.
 *
 * <p>{@code userId} 는 내보내지 않는다. 본인 지갑만 조회할 수 있어 호출자가 이미 아는 값이고,
 * 굳이 실어 보내면 다른 사람의 지갑을 조회할 수 있다는 오해를 준다.
 *
 * <p>{@code wallets.status} 도 내보내지 않는다. 컬럼 자체가 없어질 수 있어서, 미리 계약에
 * 넣어두면 FE 가 쓰기 시작한 뒤에 걷어내야 한다.
 */
@ApiModel(description = "피보호자 본인 지갑 잔액")
@Getter
public class WalletBalanceResponse {

    @ApiModelProperty(value = "지갑 ID", example = "9207")
    private final Long walletId;

    @ApiModelProperty(value = "현재 잔액(원)", example = "50000")
    private final Long balance;

    @ApiModelProperty(value = "잔액이 마지막으로 변한 시각. 화면에서 기준 시점 표시에 쓴다")
    private final LocalDateTime updatedAt;

    public WalletBalanceResponse(Wallet wallet) {
        this.walletId = wallet.getWalletId();
        this.balance = wallet.getBalance();
        this.updatedAt = wallet.getUpdatedAt();
    }
}
