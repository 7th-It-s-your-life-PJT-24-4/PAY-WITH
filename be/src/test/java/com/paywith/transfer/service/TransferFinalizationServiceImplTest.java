package com.paywith.transfer.service;

import com.paywith.exception.BusinessException;
import com.paywith.external.openbanking.OpenBankingClient;
import com.paywith.external.openbanking.dto.RealNameInquiryResponse;
import com.paywith.fds.domain.RiskLevel;
import com.paywith.recipient.domain.Recipient;
import com.paywith.transaction.domain.Transaction;
import com.paywith.transaction.mapper.TransactionMapper;
import com.paywith.transfer.dto.PreparedTransfer;
import com.paywith.transfer.dto.TransferRequest;
import com.paywith.transfer.dto.TransferResponse;
import com.paywith.wallet.domain.Wallet;
import com.paywith.wallet.mapper.WalletMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransferFinalizationServiceImplTest {

    @Mock
    private WalletMapper walletMapper;

    @Mock
    private TransactionMapper transactionMapper;

    @Mock
    private OpenBankingClient openBankingClient;

    @InjectMocks
    private TransferFinalizationServiceImpl transferFinalizationService;

    private final Long userId = 1L;
    private TransferRequest request;
    private PreparedTransfer prepared;

    @BeforeEach
    void setUp() {
        request = new TransferRequest("004", "11012300006781", 50_000L, "생활비", "123456");

        Wallet wallet = Wallet.builder().walletId(10L).userId(userId).balance(100_000L).build();
        Recipient recipient = Recipient.builder().recipientId(200L).build();
        Transaction transaction = Transaction.builder().transactionId(999L).build();

        RealNameInquiryResponse inquiryResponse = new RealNameInquiryResponse();
        inquiryResponse.setBankName("KB국민은행");
        inquiryResponse.setAccountHolderName("김시니어");

        prepared = PreparedTransfer.builder()
                .wallet(wallet)
                .recipient(recipient)
                .transaction(transaction)
                .inquiryResponse(inquiryResponse)
                .build();
    }

    @Test
    void 위험등급이면_거래를_HELD로_전환하고_잔액은_건드리지_않는다() {
        given(transactionMapper.updateStatus(999L, "HELD")).willReturn(1);

        TransferResponse result = transferFinalizationService.finalize(prepared, RiskLevel.DANGER, request);

        assertThat(result.getStatus()).isEqualTo("HELD");
        assertThat(result.getTransactionId()).isEqualTo(999L);

        verify(transactionMapper).updateStatus(999L, "HELD");
        verify(walletMapper, never()).decreaseBalanceIfSufficient(any(), any());
        verify(openBankingClient, never()).deposit(any(), any(), any());
    }

    @Test
    void 잔액이_부족하면_예외를_던지고_입금이체는_하지_않는다() {
        given(transactionMapper.updateStatus(999L, "PROCESSING")).willReturn(1);
        given(walletMapper.decreaseBalanceIfSufficient(10L, 50_000L)).willReturn(0);

        assertThatThrownBy(() -> transferFinalizationService.finalize(prepared, RiskLevel.SAFE, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.UNPROCESSABLE_ENTITY)
                .hasMessageContaining("송금 가능한 잔액이 부족합니다");

        verify(transactionMapper).updateStatus(999L, "PROCESSING");
        verify(openBankingClient, never()).deposit(any(), any(), any());
        verify(transactionMapper, never()).completeTransaction(any(), any(), any(), any());
    }

    @Test
    void 정상_완료시_입금이체하고_완료_응답을_반환한다() {
        given(transactionMapper.updateStatus(999L, "PROCESSING")).willReturn(1);
        given(walletMapper.decreaseBalanceIfSufficient(10L, 50_000L)).willReturn(1);
        Wallet updatedWallet = Wallet.builder().walletId(10L).userId(userId).balance(50_000L).build();
        given(walletMapper.findWalletByUserId(userId)).willReturn(updatedWallet);
        given(transactionMapper.completeTransaction(eq(999L), eq("COMPLETED"), eq(50_000L), any(LocalDateTime.class)))
                .willReturn(1);

        TransferResponse result = transferFinalizationService.finalize(prepared, RiskLevel.SAFE, request);

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getTransactionId()).isEqualTo(999L);
        assertThat(result.getHolderName()).isEqualTo("김시니어");
        assertThat(result.getBankName()).isEqualTo("KB국민은행");
        assertThat(result.getBalanceAfter()).isEqualTo(50_000L);
        assertThat(result.getAmount()).isEqualTo(50_000L);
        assertThat(result.getMemo()).isEqualTo("생활비");

        verify(openBankingClient).deposit("004", "11012300006781", 50_000L);
        verify(transactionMapper).completeTransaction(eq(999L), eq("COMPLETED"), eq(50_000L), any(LocalDateTime.class));
    }

    @Test
    void 주의등급도_HELD로_보류되지_않고_정상적으로_송금이_진행된다() {
        given(transactionMapper.updateStatus(999L, "PROCESSING")).willReturn(1);
        given(walletMapper.decreaseBalanceIfSufficient(10L, 50_000L)).willReturn(1);
        given(walletMapper.findWalletByUserId(userId))
                .willReturn(Wallet.builder().walletId(10L).userId(userId).balance(50_000L).build());
        given(transactionMapper.completeTransaction(eq(999L), eq("COMPLETED"), eq(50_000L), any(LocalDateTime.class)))
                .willReturn(1);

        TransferResponse result = transferFinalizationService.finalize(prepared, RiskLevel.CAUTION, request);

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        verify(transactionMapper, never()).updateStatus(999L, "HELD");
    }
}
