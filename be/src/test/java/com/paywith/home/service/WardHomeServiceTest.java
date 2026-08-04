package com.paywith.home.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.paywith.approval.domain.ApprovalRequestView;
import com.paywith.approval.mapper.ApprovalRequestMapper;
import com.paywith.exception.BusinessException;
import com.paywith.home.dto.WardHomeResponse;
import com.paywith.user.domain.User;
import com.paywith.user.mapper.UserMapper;
import com.paywith.wallet.domain.Wallet;
import com.paywith.wallet.dto.WalletBalanceResponse;
import com.paywith.wallet.service.WalletService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
@DisplayName("피보호자 홈 화면 조회")
class WardHomeServiceTest {

    private static final Long WARD_ID = 42L;

    @Mock
    private UserMapper userMapper;

    @Mock
    private WalletService walletService;

    @Mock
    private ApprovalRequestMapper approvalRequestMapper;

    private WardHomeService service;

    @BeforeEach
    void setUp() {
        service = new WardHomeServiceImpl(userMapper, walletService, approvalRequestMapper);
    }

    private User user() {
        User user = new User();
        user.setId(WARD_ID);
        user.setName("김시니어");
        return user;
    }

    private WalletBalanceResponse wallet() {
        return new WalletBalanceResponse(Wallet.builder()
            .walletId(9207L)
            .userId(WARD_ID)
            .balance(50000L)
            .updatedAt(LocalDateTime.now())
            .build());
    }

    private ApprovalRequestView view(Long approvalId, String amount) {
        ApprovalRequestView view = new ApprovalRequestView();
        view.setApprovalId(approvalId);
        view.setTransactionId(approvalId + 400);
        view.setWardId(WARD_ID);
        view.setWardName("김시니어");
        view.setType("TRANSFER_OUT");
        view.setAmount(new BigDecimal(amount));
        view.setMemo("생활비");
        view.setHolderName("박수취");
        view.setBankName("신한은행");
        view.setAccountNo("110234567890");
        view.setRiskLevel("DANGER");
        view.setTotalScore(64);
        view.setRequestedAt(LocalDateTime.now());
        view.setExpiredAt(LocalDateTime.now().plusMinutes(30));
        return view;
    }

    @Test
    void findMyHome_composesNameWalletAndApprovals() {
        given(userMapper.findById(WARD_ID)).willReturn(user());
        given(walletService.findMyBalance(WARD_ID)).willReturn(wallet());
        given(approvalRequestMapper.findPendingByWardId(WARD_ID))
            .willReturn(List.of(view(100L, "2000000")));

        WardHomeResponse result = service.findMyHome(WARD_ID);

        assertThat(result.getUserName()).isEqualTo("김시니어");
        assertThat(result.getWallet().getWalletId()).isEqualTo(9207L);
        assertThat(result.getWallet().getBalance()).isEqualTo(50000L);
        assertThat(result.getPendingApprovals()).hasSize(1);
        assertThat(result.getPendingApprovals().get(0).getApprovalId()).isEqualTo(100L);
        assertThat(result.getPendingApprovals().get(0).getType()).isEqualTo("TRANSFER_OUT");
        assertThat(result.getPendingApprovals().get(0).getRiskLevel()).isEqualTo("DANGER");
    }

    // 화면이 요약 문구에 바로 쓰는 값이라 목록 길이와 어긋나면 안 된다
    @Test
    void findMyHome_pendingApprovalCountMatchesListSize() {
        given(userMapper.findById(WARD_ID)).willReturn(user());
        given(walletService.findMyBalance(WARD_ID)).willReturn(wallet());
        given(approvalRequestMapper.findPendingByWardId(WARD_ID))
            .willReturn(List.of(view(100L, "2000000"), view(101L, "3500000")));

        WardHomeResponse result = service.findMyHome(WARD_ID);

        assertThat(result.getPendingApprovalCount()).isEqualTo(2);
        assertThat(result.getPendingApprovalCount()).isEqualTo(result.getPendingApprovals().size());
    }

    @Test
    void findMyHome_returnsZeroCountWhenNoPendingApprovals() {
        given(userMapper.findById(WARD_ID)).willReturn(user());
        given(walletService.findMyBalance(WARD_ID)).willReturn(wallet());
        given(approvalRequestMapper.findPendingByWardId(WARD_ID)).willReturn(List.of());

        WardHomeResponse result = service.findMyHome(WARD_ID);

        assertThat(result.getPendingApprovalCount()).isZero();
        assertThat(result.getPendingApprovals()).isEmpty();
    }

    @Test
    void findMyHome_failsWhenUserNotFound() {
        given(userMapper.findById(WARD_ID)).willReturn(null);

        assertThatThrownBy(() -> service.findMyHome(WARD_ID))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);

        then(walletService).should(never()).findMyBalance(anyLong());
        then(approvalRequestMapper).should(never()).findPendingByWardId(anyLong());
    }

    // 역할 검증은 지갑 도메인에 남긴다. 홈이 규칙을 복제하지 않고 그대로 전파하는지 확인한다
    @Test
    void findMyHome_propagatesRoleRejectionFromWalletService() {
        given(userMapper.findById(WARD_ID)).willReturn(user());
        given(walletService.findMyBalance(WARD_ID)).willThrow(
            new BusinessException(HttpStatus.FORBIDDEN, "AUTH_004", "피보호자만 접근할 수 있습니다."));

        assertThatThrownBy(() -> service.findMyHome(WARD_ID))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);

        then(approvalRequestMapper).should(never()).findPendingByWardId(anyLong());
    }

    // 담당 보호자를 거치지 않는 본인 조회다. 보호자용 목록 조회로 새면 안 된다
    @Test
    void findMyHome_queriesByWardIdNotGuardId() {
        given(userMapper.findById(WARD_ID)).willReturn(user());
        given(walletService.findMyBalance(WARD_ID)).willReturn(wallet());
        given(approvalRequestMapper.findPendingByWardId(WARD_ID)).willReturn(List.of());

        service.findMyHome(WARD_ID);

        then(approvalRequestMapper).should().findPendingByWardId(WARD_ID);
        then(approvalRequestMapper).should(never()).findPendingByGuardId(anyLong(), anyLong());
    }
}
