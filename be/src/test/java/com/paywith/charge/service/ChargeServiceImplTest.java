package com.paywith.charge.service;

import com.paywith.account.dto.AccountResponse;
import com.paywith.account.service.AccountService;
import com.paywith.charge.dto.ChargeDetailResponse;
import com.paywith.charge.dto.ChargeHistoryItem;
import com.paywith.charge.dto.ChargeHistoryListResponse;
import com.paywith.charge.dto.ChargeRequest;
import com.paywith.charge.dto.ChargeResponse;
import com.paywith.charge.mapper.UserNameMapper;
import com.paywith.exception.BusinessException;
import com.paywith.exception.ChargeIrrecoverableException;
import com.paywith.external.openbanking.OpenBankingClient;
import com.paywith.external.openbanking.dto.WithdrawResponse;
import com.paywith.guard.service.GuardService;
import com.paywith.transaction.domain.Transaction;
import com.paywith.transaction.mapper.TransactionMapper;
import com.paywith.user.domain.User;
import com.paywith.user.mapper.UserMapper;
import com.paywith.wallet.domain.Wallet;
import com.paywith.wallet.mapper.WalletMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChargeServiceImplTest {

    @Mock
    private AccountService accountService;

    @Mock
    private WalletMapper walletMapper;

    @Mock
    private TransactionMapper transactionMapper;

    @Mock
    private OpenBankingClient openBankingClient;

    @Mock
    private GuardService guardService;

    @Mock
    private UserNameMapper userNameMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private ChargeServiceImpl chargeService;

    @Captor
    private ArgumentCaptor<Transaction> transactionCaptor;

    private final Long userId = 1L;
    private final Long guardId = 2L;
    private final Long wardId = 1L;
    private final Long accountId = 10L;
    private ChargeRequest request;
    private AccountResponse account;
    private Wallet wallet;
    private User guard;

    @BeforeEach
    void setUp() {
        request = new ChargeRequest(accountId, 30_000L, "123456");
        account = AccountResponse.builder()
                .accountId(accountId)
                .bankCode("004")
                .bankName("KB국민은행")
                .accountNo("11012300006781")
                .build();
        wallet = Wallet.builder().walletId(20L).userId(userId).balance(50_000L).build();
        guard = new User();
        guard.setId(guardId);
        guard.setPin("encoded-pin");

        // TransactionTemplate 내부 콜백을 그대로 실행시켜주는 스텁 (사용하지 않는 테스트에서는 unnecessary stubbing 취급되지 않도록 lenient 처리)
        lenient().when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
    }

    // ===== charge (본인 충전) =====

    @Test
    void 본인_소유_계좌가_아니면_예외를_던진다() {
        given(accountService.verifyOwnership(userId, accountId)).willReturn(false);

        assertThatThrownBy(() -> chargeService.charge(userId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasMessageContaining("등록된 계좌를 찾을 수 없습니다.");

        verify(accountService, never()).getAccountDetail(any());
        verify(walletMapper, never()).findWalletByUserIdForUpdate(any());
    }

    @Test
    void 지갑이_없으면_예외를_던진다() {
        given(accountService.verifyOwnership(userId, accountId)).willReturn(true);
        given(accountService.getAccountDetail(accountId)).willReturn(account);
        given(walletMapper.findWalletByUserIdForUpdate(userId)).willReturn(null);

        assertThatThrownBy(() -> chargeService.charge(userId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasMessageContaining("지갑을 찾을 수 없습니다.");

        verify(openBankingClient, never()).withdraw(any(), any(), any());
        verify(walletMapper, never()).increaseBalance(any(), any());
        verify(transactionMapper, never()).insertTransaction(any());
    }

    @Test
    void 정상_충전시_잔액을_증가시키고_완료_응답을_반환한다() {
        Wallet updatedWallet = Wallet.builder().walletId(20L).userId(userId).balance(80_000L).build();

        given(accountService.verifyOwnership(userId, accountId)).willReturn(true);
        given(accountService.getAccountDetail(accountId)).willReturn(account);
        given(walletMapper.findWalletByUserIdForUpdate(userId)).willReturn(wallet);
        doAnswer(invocation -> {
            Transaction transaction = invocation.getArgument(0);
            transaction.setTransactionId(999L);
            return null;
        }).when(transactionMapper).insertTransaction(any(Transaction.class));
        given(openBankingClient.withdraw("004", "11012300006781", 30_000L))
                .willReturn(new WithdrawResponse());
        given(walletMapper.findWalletByUserId(userId)).willReturn(updatedWallet);
        given(transactionMapper.completeTransaction(eq(999L), eq("COMPLETED"), eq(80_000L), any()))
                .willReturn(1);

        ChargeResponse response = chargeService.charge(userId, request);

        assertThat(response.getTransactionId()).isEqualTo(999L);
        assertThat(response.getChargeAmount()).isEqualTo(30_000L);
        assertThat(response.getBalanceAfter()).isEqualTo(80_000L);
        assertThat(response.getBankName()).isEqualTo("KB국민은행");
        assertThat(response.getAccountNo()).isEqualTo("11012300006781");
        assertThat(response.getWardId()).isNull();

        verify(walletMapper).increaseBalance(20L, 30_000L);
        verify(transactionMapper).insertTransaction(any(Transaction.class));
        verify(transactionMapper).completeTransaction(eq(999L), eq("COMPLETED"), eq(80_000L), any());
    }

    @Test
    void 출금_이체가_실패하면_거래를_실패처리하고_충전불가_예외를_던진다() {
        given(accountService.verifyOwnership(userId, accountId)).willReturn(true);
        given(accountService.getAccountDetail(accountId)).willReturn(account);
        given(walletMapper.findWalletByUserIdForUpdate(userId)).willReturn(wallet);
        doAnswer(invocation -> {
            Transaction transaction = invocation.getArgument(0);
            transaction.setTransactionId(999L);
            return null;
        }).when(transactionMapper).insertTransaction(any(Transaction.class));
        given(openBankingClient.withdraw("004", "11012300006781", 30_000L))
                .willThrow(new RuntimeException("open banking down"));

        assertThatThrownBy(() -> chargeService.charge(userId, request))
                .isInstanceOf(ChargeIrrecoverableException.class)
                .hasMessageContaining("transactionId=999");

        verify(transactionMapper).markFailedIfRequested(999L);
        verify(walletMapper, never()).increaseBalance(any(), any());
    }

    // ===== chargeByGuard (보호자 충전) =====

    @Test
    void 보호자_충전시_페어링된_보호자가_아니면_예외를_던진다() {
        given(guardService.verifyGuardOfWard(guardId, wardId)).willReturn(false);

        assertThatThrownBy(() -> chargeService.chargeByGuard(guardId, wardId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasMessageContaining("연동된 피보호자를 찾을 수 없습니다.");

        verify(accountService, never()).verifyOwnership(any(), any());
    }

    @Test
    void 보호자_충전시_비밀번호가_일치하지_않으면_예외를_던진다() {
        given(guardService.verifyGuardOfWard(guardId, wardId)).willReturn(true);
        given(userMapper.findById(guardId)).willReturn(guard);
        given(passwordEncoder.matches(request.getPin(), guard.getPin())).willReturn(false);

        assertThatThrownBy(() -> chargeService.chargeByGuard(guardId, wardId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                .hasMessageContaining("충전 비밀번호가 올바르지 않습니다.");

        verify(accountService, never()).verifyOwnership(any(), any());
    }

    @Test
    void 보호자_충전시_피보호자_지갑에_충전하고_피보호자_정보를_채운다() {
        Wallet updatedWallet = Wallet.builder().walletId(20L).userId(wardId).balance(80_000L).build();

        given(guardService.verifyGuardOfWard(guardId, wardId)).willReturn(true);
        given(userMapper.findById(guardId)).willReturn(guard);
        given(passwordEncoder.matches(request.getPin(), guard.getPin())).willReturn(true);
        given(accountService.verifyOwnership(guardId, accountId)).willReturn(true);
        given(accountService.getAccountDetail(accountId)).willReturn(account);
        given(walletMapper.findWalletByUserIdForUpdate(wardId)).willReturn(wallet);
        given(userNameMapper.findUserName(wardId)).willReturn("김시니어");
        doAnswer(invocation -> {
            Transaction transaction = invocation.getArgument(0);
            transaction.setTransactionId(999L);
            return null;
        }).when(transactionMapper).insertTransaction(any(Transaction.class));
        given(openBankingClient.withdraw("004", "11012300006781", 30_000L))
                .willReturn(new WithdrawResponse());
        given(walletMapper.findWalletByUserId(wardId)).willReturn(updatedWallet);
        given(transactionMapper.completeTransaction(eq(999L), eq("COMPLETED"), eq(80_000L), any()))
                .willReturn(1);

        ChargeResponse response = chargeService.chargeByGuard(guardId, wardId, request);

        assertThat(response.getWardId()).isEqualTo(wardId);
        assertThat(response.getWardName()).isEqualTo("김시니어");
        assertThat(response.getBalanceAfter()).isEqualTo(80_000L);

        verify(walletMapper).findWalletByUserIdForUpdate(wardId);
        verify(transactionMapper).insertTransaction(transactionCaptor.capture());
        assertThat(transactionCaptor.getValue().getInitiatedBy()).isEqualTo(guardId);
    }

    // ===== getChargeHistories =====

    @Test
    void 충전_내역_목록을_그대로_응답한다() {
        ChargeHistoryItem item = ChargeHistoryItem.builder()
                .transactionId(999L)
                .wardId(wardId)
                .wardName("김시니어")
                .amount(30_000L)
                .build();
        given(transactionMapper.findChargeHistoriesByGuardId(guardId)).willReturn(List.of(item));

        ChargeHistoryListResponse response = chargeService.getChargeHistories(guardId);

        assertThat(response.getCharges()).containsExactly(item);
    }

    // ===== getChargeDetail =====

    @Test
    void 충전_상세_내역이_없으면_예외를_던진다() {
        given(transactionMapper.findChargeDetailByGuardId(999L, guardId)).willReturn(null);

        assertThatThrownBy(() -> chargeService.getChargeDetail(guardId, 999L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasMessageContaining("충전 내역을 찾을 수 없습니다.");
    }

    @Test
    void 충전_상세_내역을_그대로_응답한다() {
        ChargeDetailResponse detail = ChargeDetailResponse.builder()
                .transactionId(999L)
                .wardId(wardId)
                .wardName("김시니어")
                .amount(30_000L)
                .build();
        given(transactionMapper.findChargeDetailByGuardId(999L, guardId)).willReturn(detail);

        ChargeDetailResponse response = chargeService.getChargeDetail(guardId, 999L);

        assertThat(response).isEqualTo(detail);
    }
}
