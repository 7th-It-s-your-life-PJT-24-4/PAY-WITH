package com.paywith.guard.dto;

import java.util.List;
import lombok.Getter;

@Getter
public class SelectedWardResponse {

    private final Long wardId;
    private final String name;
    private final Long balance;
    private final List<PendingApprovalResponse> pendingApprovals;
    private final int pendingApprovalCount;
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
