package com.paywith.guard.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import lombok.Getter;

@ApiModel(description = "보호자 홈에서 선택된 피보호자의 잔액·승인대기·최근거래 요약")
@Getter
public class SelectedWardResponse {

    @ApiModelProperty(value = "선택된 피보호자 회원 ID", example = "1")
    private final Long wardId;

    @ApiModelProperty(value = "선택된 피보호자 이름", example = "김시니어")
    private final String name;

    @ApiModelProperty(value = "피보호자 지갑 잔액(원). 지갑이 없으면 0", example = "150000")
    private final Long balance;

    @ApiModelProperty(value = "승인 대기(PENDING·미만료) 거래 목록. 만료 시각(expired_at) 오름차순으로 최대 3건")
    private final List<PendingApprovalResponse> pendingApprovals;

    @ApiModelProperty(value = "승인 대기 전체 건수. pendingApprovals 는 3건까지만 담지만 이 값은 제한 전 전체 건수", example = "5")
    private final int pendingApprovalCount;

    @ApiModelProperty(value = "최근 거래 목록. 종결 상태(COMPLETED·REJECTED·CANCELED)만 생성 시각 내림차순 최대 3건. "
        + "보호자 대리 충전 건도 포함된다")
    private final List<RecentTransactionResponse> recentTransactions;

    public SelectedWardResponse(
        Long wardId,
        String name,
        Long balance,
        List<PendingApprovalResponse> pendingApprovals,
        int pendingApprovalCount,
        List<RecentTransactionResponse> recentTransactions
    ) {
        this.wardId = wardId;
        this.name = name;
        this.balance = balance;
        this.pendingApprovals = pendingApprovals;
        this.pendingApprovalCount = pendingApprovalCount;
        this.recentTransactions = recentTransactions;
    }
}
