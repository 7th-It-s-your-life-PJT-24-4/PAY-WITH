package com.paywith.wallet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.paywith.exception.BusinessException;
import com.paywith.user.domain.Role;
import com.paywith.user.domain.User;
import com.paywith.user.mapper.UserMapper;
import com.paywith.wallet.domain.Wallet;
import com.paywith.wallet.dto.WalletBalanceResponse;
import com.paywith.wallet.mapper.WalletMapper;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
@DisplayName("피보호자 지갑 잔액 조회")
class WalletServiceImplTest {

    private static final Long USER_ID = 9207L;

    @Mock
    private WalletMapper walletMapper;

    @Mock
    private UserMapper userMapper;

    private WalletService service;

    @BeforeEach
    void setUp() {
        service = new WalletServiceImpl(walletMapper, userMapper);
    }

    private User user(Role role) {
        User user = new User();
        user.setId(USER_ID);
        user.setRole(role);
        return user;
    }

    @Test
    void findMyBalance_returnsBalanceOfAuthenticatedWard() {
        LocalDateTime updatedAt = LocalDateTime.of(2026, 8, 3, 15, 55, 51);
        given(userMapper.findById(USER_ID)).willReturn(user(Role.WARD));
        given(walletMapper.findWalletByUserId(USER_ID)).willReturn(
            Wallet.builder()
                .walletId(100L)
                .userId(USER_ID)
                .balance(50000L)
                .status("ACTIVE")
                .updatedAt(updatedAt)
                .build());

        WalletBalanceResponse response = service.findMyBalance(USER_ID);

        assertThat(response.getWalletId()).isEqualTo(100L);
        assertThat(response.getBalance()).isEqualTo(50000L);
        assertThat(response.getUpdatedAt()).isEqualTo(updatedAt);
    }

    // 지갑은 WARD 에게만 생성되므로 보호자는 자연히 "지갑 없음"이 된다. 그 상태로 404 를 주면
    // 지갑 생성이 누락된 장애와 구분되지 않아, 역할을 먼저 확인해 403 으로 나눈다.
    @Test
    void findMyBalance_rejectsGuardBeforeLookingUpWallet() {
        given(userMapper.findById(USER_ID)).willReturn(user(Role.GUARD));

        assertThatThrownBy(() -> service.findMyBalance(USER_ID))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);

        then(walletMapper).should(never()).findWalletByUserId(USER_ID);
    }

    @Test
    void findMyBalance_throwsNotFoundWhenWalletIsMissing() {
        given(userMapper.findById(USER_ID)).willReturn(user(Role.WARD));
        given(walletMapper.findWalletByUserId(USER_ID)).willReturn(null);

        assertThatThrownBy(() -> service.findMyBalance(USER_ID))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
    }

    @Test
    void findMyBalance_throwsNotFoundWhenUserIsMissing() {
        given(userMapper.findById(USER_ID)).willReturn(null);

        assertThatThrownBy(() -> service.findMyBalance(USER_ID))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);

        then(walletMapper).should(never()).findWalletByUserId(USER_ID);
    }
}
