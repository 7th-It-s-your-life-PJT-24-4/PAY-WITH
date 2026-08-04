package com.paywith.home.dto;

import com.paywith.wallet.dto.WalletBalanceResponse;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import lombok.Getter;

/**
 * 피보호자 홈 화면이 한 번에 필요로 하는 값 묶음.
 *
 * <p>잔액은 {@link WalletBalanceResponse} 를 그대로 품는다. 필드를 펼쳐 담으면 지갑 조회
 * API 와 계약이 갈라져, 한쪽만 바뀌었을 때 FE 가 두 곳을 따로 고쳐야 한다.
 */
@ApiModel(description = "피보호자 홈 화면 정보")
@Getter
public class WardHomeResponse {

    @ApiModelProperty(value = "로그인한 피보호자 이름", example = "김시니어")
    private final String userName;

    @ApiModelProperty(value = "본인 지갑 잔액")
    private final WalletBalanceResponse wallet;

    @ApiModelProperty(value = "승인 대기 건수. 목록 길이와 같지만 화면이 요약 문구에 바로 쓴다",
        example = "2")
    private final int pendingApprovalCount;

    @ApiModelProperty(value = "승인 대기 거래 목록. 만료가 임박한 순서")
    private final List<PendingApprovalItemResponse> pendingApprovals;

    public WardHomeResponse(
        String userName,
        WalletBalanceResponse wallet,
        List<PendingApprovalItemResponse> pendingApprovals
    ) {
        this.userName = userName;
        this.wallet = wallet;
        this.pendingApprovalCount = pendingApprovals.size();
        this.pendingApprovals = pendingApprovals;
    }
}
