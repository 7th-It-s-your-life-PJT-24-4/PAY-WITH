package com.paywith.transfer.service;

import com.paywith.exception.BusinessException;
import com.paywith.exception.TransferIrrecoverableException;
import com.paywith.external.openbanking.OpenBankingClient;
import com.paywith.external.openbanking.dto.RealNameInquiryResponse;
import com.paywith.fds.domain.RiskLevel;
import com.paywith.recipient.domain.Recipient;
import com.paywith.transaction.domain.Transaction;
import com.paywith.transaction.mapper.TransactionMapper;
import com.paywith.transfer.dto.PreparedTransfer;
import com.paywith.transfer.dto.TransferExecutionContext;
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
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransferFinalizationServiceImplTest {

    @Mock
    private WalletMapper walletMapper;

    @Mock
    private TransactionMapper transactionMapper;

    @Mock
    private OpenBankingClient openBankingClient;

    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private TransferFinalizationServiceImpl transferFinalizationService;

    private final Long userId = 1L;
    private TransferRequest request;
    private PreparedTransfer prepared;

    @BeforeEach
    void setUp() {
        // finalize() 내부에서 transactionTemplate.execute(...)로 7~9번을 감싸므로,
        // 콜백을 그대로 실행해주는 스텁이 필요하다 (HELD 분기 테스트는 이 스텁을 안 써서 lenient 처리)
        lenient().when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(new SimpleTransactionStatus());
        });

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

    // ===== "돌아올 수 없는 지점"(deposit 호출) 이후 실패 경로 =====

    @Test
    void 입금_호출이_실패하면_FAILED로_기록하고_TransferIrrecoverableException을_던진다() {
        given(transactionMapper.updateStatus(999L, "PROCESSING")).willReturn(1);
        given(walletMapper.decreaseBalanceIfSufficient(10L, 50_000L)).willReturn(1);
        given(walletMapper.findWalletByUserId(userId))
                .willReturn(Wallet.builder().walletId(10L).userId(userId).balance(50_000L).build());
        given(openBankingClient.deposit("004", "11012300006781", 50_000L))
                .willThrow(new RuntimeException("네트워크 오류"));
        given(transactionMapper.updateStatus(999L, "FAILED")).willReturn(1);

        assertThatThrownBy(() -> transferFinalizationService.finalize(prepared, RiskLevel.SAFE, request))
                .isInstanceOf(TransferIrrecoverableException.class)
                .hasMessageContaining("transactionId=999");

        verify(transactionMapper).updateStatus(999L, "FAILED");
        verify(transactionMapper, never()).completeTransaction(any(), any(), any(), any());
    }

    @Test
    void 완료_처리가_반복_실패하면_짧게_재시도한_뒤_FAILED로_기록한다() {
        given(transactionMapper.updateStatus(999L, "PROCESSING")).willReturn(1);
        given(walletMapper.decreaseBalanceIfSufficient(10L, 50_000L)).willReturn(1);
        given(walletMapper.findWalletByUserId(userId))
                .willReturn(Wallet.builder().walletId(10L).userId(userId).balance(50_000L).build());
        // 입금은 이미 성공, 완료 처리(completeTransaction)만 계속 0행(실패)
        given(transactionMapper.completeTransaction(eq(999L), eq("COMPLETED"), eq(50_000L), any(LocalDateTime.class)))
                .willReturn(0);
        given(transactionMapper.updateStatus(999L, "FAILED")).willReturn(1);

        assertThatThrownBy(() -> transferFinalizationService.finalize(prepared, RiskLevel.SAFE, request))
                .isInstanceOf(TransferIrrecoverableException.class);

        // 입금은 재시도 없이 딱 1번만 호출돼야 한다 (이미 성공했으므로 다시 부르면 이중 입금)
        verify(openBankingClient, times(1)).deposit("004", "11012300006781", 50_000L);
        verify(transactionMapper, times(3))
                .completeTransaction(eq(999L), eq("COMPLETED"), eq(50_000L), any(LocalDateTime.class));
        verify(transactionMapper).updateStatus(999L, "FAILED");
    }

    @Test
    void 완료_처리가_처음엔_실패했다가_재시도로_성공하면_정상_완료된다() {
        given(transactionMapper.updateStatus(999L, "PROCESSING")).willReturn(1);
        given(walletMapper.decreaseBalanceIfSufficient(10L, 50_000L)).willReturn(1);
        given(walletMapper.findWalletByUserId(userId))
                .willReturn(Wallet.builder().walletId(10L).userId(userId).balance(50_000L).build());
        given(transactionMapper.completeTransaction(eq(999L), eq("COMPLETED"), eq(50_000L), any(LocalDateTime.class)))
                .willReturn(0, 1); // 1차 실패, 2차 성공

        TransferResponse result = transferFinalizationService.finalize(prepared, RiskLevel.SAFE, request);

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        verify(transactionMapper, times(2))
                .completeTransaction(eq(999L), eq("COMPLETED"), eq(50_000L), any(LocalDateTime.class));
        verify(transactionMapper, never()).updateStatus(999L, "FAILED");
    }

    // ===== finalizeApprovedTransfer(보호자 승인 이후 재개 경로) =====

    @Test
    void 승인후_재개시_거래정보가_없으면_404_예외를_던진다() {
        given(transactionMapper.findExecutionContextByTransactionId(999L)).willReturn(null);

        assertThatThrownBy(() -> transferFinalizationService.finalizeApprovedTransfer(999L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasMessageContaining("transactionId=999");

        verify(transactionMapper, never()).updateStatus(any(), any());
        verify(walletMapper, never()).decreaseBalanceIfSufficient(any(), any());
        verify(openBankingClient, never()).deposit(any(), any(), any());
    }

    @Test
    void 승인후_재개시_거래정보가_있으면_조회한_컨텍스트로_완료처리까지_진행한다() {
        TransferExecutionContext context = TransferExecutionContext.builder()
                .transactionId(999L)
                .walletId(10L)
                .userId(userId)
                .bankCode("004")
                .bankName("KB국민은행")
                .accountNo("11012300006781")
                .holderName("김시니어")
                .amount(50_000L)
                .memo("생활비")
                .build();
        given(transactionMapper.findExecutionContextByTransactionId(999L)).willReturn(context);
        given(transactionMapper.updateStatus(999L, "PROCESSING")).willReturn(1);
        given(walletMapper.decreaseBalanceIfSufficient(10L, 50_000L)).willReturn(1);
        given(walletMapper.findWalletByUserId(userId))
                .willReturn(Wallet.builder().walletId(10L).userId(userId).balance(50_000L).build());
        given(transactionMapper.completeTransaction(eq(999L), eq("COMPLETED"), eq(50_000L), any(LocalDateTime.class)))
                .willReturn(1);

        TransferResponse result = transferFinalizationService.finalizeApprovedTransfer(999L);

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getTransactionId()).isEqualTo(999L);
        assertThat(result.getHolderName()).isEqualTo("김시니어");
        assertThat(result.getBankName()).isEqualTo("KB국민은행");
        assertThat(result.getBalanceAfter()).isEqualTo(50_000L);

        verify(openBankingClient).deposit("004", "11012300006781", 50_000L);
        verify(transactionMapper).completeTransaction(eq(999L), eq("COMPLETED"), eq(50_000L), any(LocalDateTime.class));
    }

    @Test
    void 승인후_재개시_입금이_실패하면_FAILED로_기록하고_TransferIrrecoverableException을_던진다() {
        TransferExecutionContext context = TransferExecutionContext.builder()
                .transactionId(999L)
                .walletId(10L)
                .userId(userId)
                .bankCode("004")
                .bankName("KB국민은행")
                .accountNo("11012300006781")
                .holderName("김시니어")
                .amount(50_000L)
                .memo("생활비")
                .build();
        given(transactionMapper.findExecutionContextByTransactionId(999L)).willReturn(context);
        given(transactionMapper.updateStatus(999L, "PROCESSING")).willReturn(1);
        given(walletMapper.decreaseBalanceIfSufficient(10L, 50_000L)).willReturn(1);
        given(walletMapper.findWalletByUserId(userId))
                .willReturn(Wallet.builder().walletId(10L).userId(userId).balance(50_000L).build());
        given(openBankingClient.deposit("004", "11012300006781", 50_000L))
                .willThrow(new RuntimeException("네트워크 오류"));
        given(transactionMapper.updateStatus(999L, "FAILED")).willReturn(1);

        assertThatThrownBy(() -> transferFinalizationService.finalizeApprovedTransfer(999L))
                .isInstanceOf(TransferIrrecoverableException.class)
                .hasMessageContaining("transactionId=999");

        verify(transactionMapper).updateStatus(999L, "FAILED");
        verify(transactionMapper, never()).completeTransaction(any(), any(), any(), any());
    }
}
