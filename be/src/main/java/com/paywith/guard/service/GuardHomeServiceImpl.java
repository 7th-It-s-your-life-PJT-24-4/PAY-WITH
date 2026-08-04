package com.paywith.guard.service;

import com.paywith.approval.domain.ApprovalRequestView;
import com.paywith.approval.dto.ApprovalRuleHitResponse;
import com.paywith.approval.mapper.ApprovalRequestMapper;
import com.paywith.exception.BusinessException;
import com.paywith.guard.domain.WardSummary;
import com.paywith.guard.dto.GuardHomeResponse;
import com.paywith.guard.dto.PendingApprovalResponse;
import com.paywith.guard.dto.RecentTransactionResponse;
import com.paywith.guard.dto.SelectedWardResponse;
import com.paywith.guard.dto.WardTabResponse;
import com.paywith.guard.mapper.GuardSeniorMapper;
import com.paywith.transaction.mapper.TransactionMapper;
import com.paywith.user.domain.Role;
import com.paywith.user.domain.User;
import com.paywith.user.mapper.UserMapper;
import com.paywith.wallet.domain.Wallet;
import com.paywith.wallet.mapper.WalletMapper;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class GuardHomeServiceImpl implements GuardHomeService {

    private static final int RECENT_TRANSACTION_LIMIT = 3;

    private final GuardSeniorMapper guardSeniorMapper;
    private final ApprovalRequestMapper approvalRequestMapper;
    private final WalletMapper walletMapper;
    private final TransactionMapper transactionMapper;
    private final UserMapper userMapper;

    public GuardHomeServiceImpl(
        GuardSeniorMapper guardSeniorMapper,
        ApprovalRequestMapper approvalRequestMapper,
        WalletMapper walletMapper,
        TransactionMapper transactionMapper,
        UserMapper userMapper
    ) {
        this.guardSeniorMapper = guardSeniorMapper;
        this.approvalRequestMapper = approvalRequestMapper;
        this.walletMapper = walletMapper;
        this.transactionMapper = transactionMapper;
        this.userMapper = userMapper;
    }

    @Override
    public GuardHomeResponse getHome(Long guardId, Long wardIdParam) {
        requireGuardRole(guardId);

        List<WardSummary> activeWards = guardSeniorMapper.findActiveWards(guardId);
        List<ApprovalRequestView> allPending = approvalRequestMapper.findPendingByGuardId(guardId, null);
        Set<Long> pendingWardIds = allPending.stream()
            .map(ApprovalRequestView::getWardId)
            .collect(Collectors.toSet());

        List<WardTabResponse> wards = activeWards.stream()
            .map(ward -> new WardTabResponse(ward.getWardId(), ward.getName(), pendingWardIds.contains(ward.getWardId())))
            .collect(Collectors.toList());

        WardSummary selected = resolveSelectedWard(activeWards, wardIdParam);
        SelectedWardResponse selectedWard = selected == null
            ? null
            : buildSelectedWard(selected, allPending);

        return new GuardHomeResponse(wards, selectedWard);
    }

    private WardSummary resolveSelectedWard(List<WardSummary> activeWards, Long wardIdParam) {
        if (wardIdParam == null) {
            return activeWards.isEmpty() ? null : activeWards.get(0);
        }
        return activeWards.stream()
            .filter(ward -> ward.getWardId().equals(wardIdParam))
            .findFirst()
            .orElseThrow(() -> new BusinessException(
                HttpStatus.NOT_FOUND, "LINK_001", "연동된 피보호자를 찾을 수 없습니다."
            ));
    }

    private SelectedWardResponse buildSelectedWard(WardSummary ward, List<ApprovalRequestView> allPending) {
        Wallet wallet = walletMapper.findWalletByUserId(ward.getWardId());
        Long balance = wallet != null ? wallet.getBalance() : 0L;

        PendingApprovalResponse pendingApproval = allPending.stream()
            .filter(view -> view.getWardId().equals(ward.getWardId()))
            .findFirst()
            .map(this::toPendingApproval)
            .orElse(null);

        List<RecentTransactionResponse> recentTransactions =
            transactionMapper.findRecentByWardId(ward.getWardId(), RECENT_TRANSACTION_LIMIT);

        return new SelectedWardResponse(ward.getWardId(), ward.getName(), balance, pendingApproval, recentTransactions);
    }

    private PendingApprovalResponse toPendingApproval(ApprovalRequestView view) {
        List<ApprovalRuleHitResponse> ruleHits = approvalRequestMapper.findRuleHits(view.getTransactionId());
        String riskReason = ruleHits.isEmpty() ? null : ruleHits.get(0).getDescription();

        return new PendingApprovalResponse(
            view.getTransactionId(),
            view.getAmount() != null ? view.getAmount().longValue() : null,
            view.getHolderName(),
            view.getAccountNo(),
            view.getTotalScore(),
            riskReason,
            view.getRequestedAt()
        );
    }

    private void requireGuardRole(Long guardId) {
        User user = userMapper.findById(guardId);
        if (user == null || user.getRole() != Role.GUARD) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "AUTH_004", "접근 권한이 없습니다.");
        }
    }
}