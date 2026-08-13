package com.paywith.guard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.paywith.approval.domain.ApprovalRequestView;
import com.paywith.approval.mapper.ApprovalRequestMapper;
import com.paywith.guard.domain.WardSummary;
import com.paywith.guard.dto.GuardHomeResponse;
import com.paywith.guard.mapper.GuardSeniorMapper;
import com.paywith.transaction.mapper.TransactionMapper;
import com.paywith.user.domain.Role;
import com.paywith.user.domain.User;
import com.paywith.user.mapper.UserMapper;
import com.paywith.wallet.domain.Wallet;
import com.paywith.wallet.mapper.WalletMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("보호자 홈 서비스")
class GuardHomeServiceImplTest {

    private static final Long GUARD_ID = 1L;
    private static final Long WARD_ID = 2L;

    @Mock
    private GuardSeniorMapper guardSeniorMapper;
    @Mock
    private ApprovalRequestMapper approvalRequestMapper;
    @Mock
    private WalletMapper walletMapper;
    @Mock
    private TransactionMapper transactionMapper;
    @Mock
    private UserMapper userMapper;

    private GuardHomeServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new GuardHomeServiceImpl(
            guardSeniorMapper,
            approvalRequestMapper,
            walletMapper,
            transactionMapper,
            userMapper
        );
    }

    @Test
    @DisplayName("대기 거래 전체 건수는 유지하고 홈 목록은 최대 3건만 반환한다")
    void getHome_limitsPendingApprovalsToThree() {
        User guard = new User();
        guard.setRole(Role.GUARD);

        WardSummary ward = new WardSummary();
        ward.setWardId(WARD_ID);
        ward.setName("김시니어");
        ward.setAvatarId(1);

        List<ApprovalRequestView> pendingApprovals = List.of(
            pendingApproval(101L, 10_000L),
            pendingApproval(102L, 20_000L),
            pendingApproval(103L, 30_000L),
            pendingApproval(104L, 40_000L)
        );

        given(userMapper.findById(GUARD_ID)).willReturn(guard);
        given(guardSeniorMapper.findActiveWards(GUARD_ID)).willReturn(List.of(ward));
        given(approvalRequestMapper.findPendingByGuardId(GUARD_ID, null))
            .willReturn(pendingApprovals);
        given(walletMapper.findWalletByUserId(WARD_ID))
            .willReturn(Wallet.builder().userId(WARD_ID).balance(100_000L).build());
        given(approvalRequestMapper.findRuleHits(anyLong()))
            .willReturn(Collections.emptyList());
        given(transactionMapper.findRecentByWardId(WARD_ID, 3))
            .willReturn(Collections.emptyList());

        GuardHomeResponse response = service.getHome(GUARD_ID, WARD_ID);

        assertThat(response.getSelectedWard().getPendingApprovalCount()).isEqualTo(4);
        assertThat(response.getSelectedWard().getPendingApprovals())
            .extracting("transactionId")
            .containsExactly(101L, 102L, 103L);
        then(approvalRequestMapper).should(never()).findRuleHits(104L);
    }

    private ApprovalRequestView pendingApproval(Long transactionId, long amount) {
        ApprovalRequestView view = new ApprovalRequestView();
        view.setTransactionId(transactionId);
        view.setWardId(WARD_ID);
        view.setAmount(BigDecimal.valueOf(amount));
        view.setHolderName("수취인 " + transactionId);
        view.setAccountNo("110" + transactionId);
        view.setTotalScore(80);
        view.setRequestedAt(LocalDateTime.of(2026, 8, 12, 12, 0));
        return view;
    }
}
