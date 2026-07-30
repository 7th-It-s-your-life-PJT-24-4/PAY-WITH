package com.paywith.practice;

import com.paywith.exception.BusinessException;
import com.paywith.external.openbanking.OpenBankingClient;
import com.paywith.external.openbanking.dto.RealNameInquiryResponse;
import com.paywith.recipient.domain.Recipient;
import com.paywith.recipient.mapper.RecipientMapper;
import com.paywith.transaction.domain.Transaction;
import com.paywith.transaction.mapper.TransactionMapper;
import com.paywith.transfer.dto.RecipientInquiryRequest;
import com.paywith.transfer.dto.RecipientInquiryResponse;
import com.paywith.transfer.dto.TransferRequest;
import com.paywith.transfer.dto.TransferResponse;
import com.paywith.transfer.service.TransferServiceImpl;
import com.paywith.user.domain.User;
import com.paywith.user.mapper.UserMapper;
import com.paywith.wallet.domain.Wallet;
import com.paywith.wallet.mapper.WalletMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceImplTest {

    @Mock
    private WalletMapper walletMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RecipientMapper recipientMapper;

    @Mock
    private TransactionMapper transactionMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private OpenBankingClient openBankingClient;
    @InjectMocks
    private TransferServiceImpl transferService;

    @Test
    void 수취인_조회_성공() {
        // given
        RealNameInquiryResponse mockResponse = new RealNameInquiryResponse();
        mockResponse.setRspCode("A0000");
        mockResponse.setBankName("KB국민은행");
        mockResponse.setAccountHolderName("홍길동");

        when(openBankingClient.inquireRealName("004", "11012300006781", null))
                .thenReturn(mockResponse);

        RecipientInquiryRequest request = new RecipientInquiryRequest("004", "11012300006781");

        // when
        RecipientInquiryResponse result = transferService.inquireRecipient(request);

        // then
        assertThat(result.getBankName()).isEqualTo("KB국민은행");
        assertThat(result.getRecipientName()).isEqualTo("홍길동");
    }

    @Test
    void 수취인_조회_실패시_예외() {
        // given
        RealNameInquiryResponse mockResponse = new RealNameInquiryResponse();
        mockResponse.setRspCode("A0004");

        when(openBankingClient.inquireRealName("004", "9999999999", null))
                .thenReturn(mockResponse);

        RecipientInquiryRequest request = new RecipientInquiryRequest("004", "9999999999");

        // when & then
        assertThatThrownBy(() -> transferService.inquireRecipient(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("해당 계좌를 찾을 수 없습니다");
    }

    @Test
    void 정상_송금_신규_수취인() {
        // given
        Long userId = 1L;

        Wallet wallet = Wallet.builder()
                .walletId(10L)
                .userId(userId)
                .balance(100_000L)
                .pin("encodedPin")
                .build();

        TransferRequest request = new TransferRequest(
                "004", "11012300006781", 50_000L, "생활비", "123456"
        );

        RealNameInquiryResponse inquiryResponse = new RealNameInquiryResponse();
        inquiryResponse.setRspCode("A0000");
        inquiryResponse.setBankName("KB국민은행");
        inquiryResponse.setAccountHolderName("김시니어");

        when(walletMapper.findWalletByUserId(userId)).thenReturn(wallet);
        when(userMapper.findById(userId)).thenReturn(new User());
        when(passwordEncoder.matches("123456", "encodedPin")).thenReturn(true);
        when(openBankingClient.inquireRealName("004", "11012300006781", null))
                .thenReturn(inquiryResponse);

        // 신규 수취인 → findRecipient는 null 반환
        when(recipientMapper.findRecipient(userId, "004", "11012300006781"))
                .thenReturn(null);

        // insertRecipient 호출 시, 실제 DB의 <selectKey>가 recipientId를 채워주는 것처럼 흉내내기
        doAnswer(invocation -> {
            Recipient r = invocation.getArgument(0);
            r.setRecipientId(100L);
            return null;
        }).when(recipientMapper).insertRecipient(any(Recipient.class));

        when(walletMapper.decreaseBalanceIfSufficient(10L, 50_000L)).thenReturn(1);

        // insertTransaction도 마찬가지로 transactionId를 채워주는 것처럼 흉내내기
        doAnswer(invocation -> {
            Transaction t = invocation.getArgument(0);
            t.setTransactionId(999L);
            return null;
        }).when(transactionMapper).insertTransaction(any(Transaction.class));

        // when
        TransferResponse result = transferService.transfer(userId, request);

        // then
        assertThat(result.getTransactionId()).isEqualTo(999L);
        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getHolderName()).isEqualTo("김시니어");
        assertThat(result.getBalanceAfter()).isEqualTo(50_000L);

        verify(recipientMapper).insertRecipient(any(Recipient.class));
        verify(recipientMapper, never()).updateSendInfo(any());
        verify(openBankingClient).deposit("004", "11012300006781", 50_000L);
    }
    @Test
    void 잔액_부족시_예외() {
        // given
        Long userId = 1L;

        Wallet wallet = Wallet.builder()
                .walletId(10L)
                .userId(userId)
                .balance(10_000L)   // 요청 금액보다 적은 잔액
                .pin("encodedPin")
                .build();

        TransferRequest request = new TransferRequest(
                "004", "11012300006781", 50_000L, "생활비", "123456"
        );

        RealNameInquiryResponse inquiryResponse = new RealNameInquiryResponse();
        inquiryResponse.setRspCode("A0000");
        inquiryResponse.setBankName("KB국민은행");
        inquiryResponse.setAccountHolderName("김시니어");

        when(walletMapper.findWalletByUserId(userId)).thenReturn(wallet);
        when(userMapper.findById(userId)).thenReturn(new User());
        when(passwordEncoder.matches("123456", "encodedPin")).thenReturn(true);
        when(openBankingClient.inquireRealName("004", "11012300006781", null))
                .thenReturn(inquiryResponse);
        when(recipientMapper.findRecipient(userId, "004", "11012300006781"))
                .thenReturn(null);
        doAnswer(invocation -> {
            Recipient r = invocation.getArgument(0);
            r.setRecipientId(100L);
            return null;
        }).when(recipientMapper).insertRecipient(any(Recipient.class));

        // 핵심: 잔액 부족 → 0행 반영
        when(walletMapper.decreaseBalanceIfSufficient(10L, 50_000L)).thenReturn(0);

        // when & then
        assertThatThrownBy(() -> transferService.transfer(userId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("잔액이 부족합니다");

        // 잔액 부족 이후 단계는 절대 실행되면 안 됨
        verify(openBankingClient, never()).deposit(any(), any(), any());
        verify(transactionMapper, never()).insertTransaction(any(Transaction.class));
    }

    @Test
    void PIN_불일치시_예외() {
        // given
        Long userId = 1L;

        Wallet wallet = Wallet.builder()
                .walletId(10L)
                .userId(userId)
                .balance(100_000L)
                .pin("encodedPin")
                .build();

        TransferRequest request = new TransferRequest(
                "004", "11012300006781", 50_000L, "생활비", "999999"  // 틀린 PIN
        );

        when(walletMapper.findWalletByUserId(userId)).thenReturn(wallet);
        when(userMapper.findById(userId)).thenReturn(new User());
        when(passwordEncoder.matches("999999", "encodedPin")).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> transferService.transfer(userId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("송금 비밀번호가 올바르지 않습니다");

        // PIN 검증 이후 단계는 전부 실행되면 안 됨
        verify(openBankingClient, never()).inquireRealName(any(), any(), any());
        verify(recipientMapper, never()).findRecipient(any(), any(), any());
        verify(walletMapper, never()).decreaseBalanceIfSufficient(any(), any());
        verify(openBankingClient, never()).deposit(any(), any(), any());
        verify(transactionMapper, never()).insertTransaction(any(Transaction.class));
    }

    @Test
    void 정상_송금_기존_수취인() {
        // given
        Long userId = 1L;

        Wallet wallet = Wallet.builder()
                .walletId(10L)
                .userId(userId)
                .balance(100_000L)
                .pin("encodedPin")
                .build();

        TransferRequest request = new TransferRequest(
                "004", "11012300006781", 50_000L, "생활비", "123456"
        );

        RealNameInquiryResponse inquiryResponse = new RealNameInquiryResponse();
        inquiryResponse.setRspCode("A0000");
        inquiryResponse.setBankName("KB국민은행");
        inquiryResponse.setAccountHolderName("김시니어");

        // 이미 여러 번 송금해본 상대 (기존 수취인)
        Recipient existingRecipient = Recipient.builder()
                .recipientId(200L)
                .wardId(userId)
                .bankCode("004")
                .accountNo("11012300006781")
                .holderName("김시니어")
                .sendCount(3)
                .build();

        when(walletMapper.findWalletByUserId(userId)).thenReturn(wallet);
        when(userMapper.findById(userId)).thenReturn(new User());
        when(passwordEncoder.matches("123456", "encodedPin")).thenReturn(true);
        when(openBankingClient.inquireRealName("004", "11012300006781", null))
                .thenReturn(inquiryResponse);

        // 핵심: 기존 수취인 반환
        when(recipientMapper.findRecipient(userId, "004", "11012300006781"))
                .thenReturn(existingRecipient);

        when(walletMapper.decreaseBalanceIfSufficient(10L, 50_000L)).thenReturn(1);

        doAnswer(invocation -> {
            Transaction t = invocation.getArgument(0);
            t.setTransactionId(999L);
            return null;
        }).when(transactionMapper).insertTransaction(any(Transaction.class));

        // when
        TransferResponse result = transferService.transfer(userId, request);

        // then
        assertThat(result.getTransactionId()).isEqualTo(999L);
        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getHolderName()).isEqualTo("김시니어");

        // 기존 수취인이므로 updateSendInfo만 불리고 insertRecipient는 불리면 안 됨
        verify(recipientMapper).updateSendInfo(200L);
        verify(recipientMapper, never()).insertRecipient(any(Recipient.class));
    }


}