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
import com.paywith.external.openbanking.OpenBankingClient;
import com.paywith.external.openbanking.dto.WithdrawResponse;
import com.paywith.guard.service.GuardService;
import com.paywith.transaction.domain.Transaction;
import com.paywith.transaction.mapper.TransactionMapper;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
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

    @BeforeEach
    void setUp() {
        request = new ChargeRequest(accountId, 30_000L);
        account = AccountResponse.builder()
                .accountId(accountId)
                .bankCode("004")
                .bankName("KB국민은행")
                .accountNo("11012300006781")
                .build();
        wallet = Wallet.builder().walletId(20L).userId(userId).balance(50_000L).build();
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
        given(openBankingClient.withdraw("004", "11012300006781", 30_000L))
                .willReturn(new WithdrawResponse());
        given(walletMapper.findWalletByUserIdForUpdate(userId)).willReturn(null);

        assertThatThrownBy(() -> chargeService.charge(userId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasMessageContaining("지갑을 찾을 수 없습니다.");

        verify(walletMapper, never()).increaseBalance(any(), any());
        verify(transactionMapper, never()).insertTransaction(any());
    }

    @Test
    void 정상_충전시_잔액을_증가시키고_완료_응답을_반환한다() {
        given(accountService.verifyOwnership(userId, accountId)).willReturn(true);
        given(accountService.getAccountDetail(accountId)).willReturn(account);
        given(openBankingClient.withdraw("004", "11012300006781", 30_000L))
                .willReturn(new WithdrawResponse());
        given(walletMapper.findWalletByUserIdForUpdate(userId)).willReturn(wallet);
        doAnswer(invocation -> {
            Transaction transaction = invocation.getArgument(0);
            transaction.setTransactionId(999L);
            return null;
        }).when(transactionMapper).insertTransaction(any(Transaction.class));

        ChargeResponse response = chargeService.charge(userId, request);

        assertThat(response.getTransactionId()).isEqualTo(999L);
        assertThat(response.getChargeAmount()).isEqualTo(30_000L);
        assertThat(response.getBalanceAfter()).isEqualTo(80_000L);
        assertThat(response.getBankName()).isEqualTo("KB국민은행");
        assertThat(response.getAccountNo()).isEqualTo("11012300006781");
        assertThat(response.getWardId()).isNull();

        verify(walletMapper).increaseBalance(20L, 30_000L);
        verify(transactionMapper).insertTransaction(any(Transaction.class));
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
    void 보호자_충전시_피보호자_지갑에_충전하고_피보호자_정보를_채운다() {
        given(guardService.verifyGuardOfWard(guardId, wardId)).willReturn(true);
        given(accountService.verifyOwnership(guardId, accountId)).willReturn(true);
        given(accountService.getAccountDetail(accountId)).willReturn(account);
        given(openBankingClient.withdraw("004", "11012300006781", 30_000L))
                .willReturn(new WithdrawResponse());
        given(walletMapper.findWalletByUserIdForUpdate(wardId)).willReturn(wallet);
        given(userNameMapper.findUserName(wardId)).willReturn("김시니어");
        doAnswer(invocation -> {
            Transaction transaction = invocation.getArgument(0);
            transaction.setTransactionId(999L);
            return null;
        }).when(transactionMapper).insertTransaction(any(Transaction.class));

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
