package com.paywith.transfer.service;

import com.paywith.exception.BusinessException;
import com.paywith.external.openbanking.OpenBankingClient;
import com.paywith.external.openbanking.dto.RealNameInquiryResponse;
import com.paywith.recipient.domain.Recipient;
import com.paywith.recipient.mapper.RecipientMapper;
import com.paywith.transaction.domain.Transaction;
import com.paywith.transaction.mapper.TransactionMapper;
import com.paywith.transfer.dto.PreparedTransfer;
import com.paywith.transfer.dto.TransferRequest;
import com.paywith.user.domain.Role;
import com.paywith.user.domain.User;
import com.paywith.user.mapper.UserMapper;
import com.paywith.wallet.domain.Wallet;
import com.paywith.wallet.mapper.WalletMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransferPreparationServiceImplTest {

    @Mock
    private WalletMapper walletMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private OpenBankingClient openBankingClient;

    @Mock
    private RecipientMapper recipientMapper;

    @Mock
    private TransactionMapper transactionMapper;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private TransferPreparationServiceImpl transferPreparationService;

    private final Long userId = 1L;
    private TransferRequest request;
    private Wallet wallet;
    private User user;

    @BeforeEach
    void setUp() {
        request = new TransferRequest("004", "11012300006781", 50_000L, "생활비", "123456");
        wallet = Wallet.builder().walletId(10L).userId(userId).balance(100_000L).build();
        user = new User();
        user.setPin("encodedPin");
        user.setRole(Role.WARD);
    }

    @Test
    void 지갑이_없으면_예외() {
        given(walletMapper.findWalletByUserId(userId)).willReturn(null);

        assertThatThrownBy(() -> transferPreparationService.prepare(userId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                .hasMessageContaining("지갑을 찾을 수 없습니다");

        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void 피보호자가_아니면_예외() {
        given(walletMapper.findWalletByUserId(userId)).willReturn(wallet);
        user.setRole(Role.GUARD);
        given(userMapper.findById(userId)).willReturn(user);

        assertThatThrownBy(() -> transferPreparationService.prepare(userId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN)
                .hasMessageContaining("피보호자만 이용할 수 있습니다");

        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void 송금_비밀번호가_틀리면_예외() {
        given(walletMapper.findWalletByUserId(userId)).willReturn(wallet);
        given(userMapper.findById(userId)).willReturn(user);
        given(passwordEncoder.matches(request.getTransferPin(), user.getPin())).willReturn(false);

        assertThatThrownBy(() -> transferPreparationService.prepare(userId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                .hasMessageContaining("송금 비밀번호가 올바르지 않습니다");

        verify(openBankingClient, never()).inquireRealName(any(), any(), any());
    }

    @Test
    void 수취인_실명조회에_실패하면_예외() {
        given(walletMapper.findWalletByUserId(userId)).willReturn(wallet);
        given(userMapper.findById(userId)).willReturn(user);
        given(passwordEncoder.matches(request.getTransferPin(), user.getPin())).willReturn(true);

        RealNameInquiryResponse failResponse = new RealNameInquiryResponse();
        failResponse.setRspCode("A0004");
        given(openBankingClient.inquireRealName("004", "11012300006781", null))
                .willReturn(failResponse);

        assertThatThrownBy(() -> transferPreparationService.prepare(userId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasMessageContaining("해당계좌를 찾을 수 없습니다");

        verify(recipientMapper, never()).findRecipient(any(), any(), any());
    }

    @Test
    void 신규_수취인이면_등록하고_준비결과를_반환한다() {
        given(walletMapper.findWalletByUserId(userId)).willReturn(wallet);
        given(userMapper.findById(userId)).willReturn(user);
        given(passwordEncoder.matches(request.getTransferPin(), user.getPin())).willReturn(true);

        RealNameInquiryResponse inquiryResponse = new RealNameInquiryResponse();
        inquiryResponse.setRspCode("A0000");
        inquiryResponse.setBankName("KB국민은행");
        inquiryResponse.setAccountHolderName("김시니어");
        given(openBankingClient.inquireRealName("004", "11012300006781", null))
                .willReturn(inquiryResponse);

        given(recipientMapper.findRecipient(userId, "004", "11012300006781")).willReturn(null);
        doAnswer(invocation -> {
            Recipient r = invocation.getArgument(0);
            r.setRecipientId(100L);
            return null;
        }).when(recipientMapper).insertRecipient(any(Recipient.class));

        doAnswer(invocation -> {
            Transaction t = invocation.getArgument(0);
            t.setTransactionId(999L);
            return null;
        }).when(transactionMapper).insertTransaction(any(Transaction.class));

        PreparedTransfer result = transferPreparationService.prepare(userId, request);

        assertThat(result.getWallet()).isEqualTo(wallet);
        assertThat(result.getRecipient().getRecipientId()).isEqualTo(100L);
        assertThat(result.getRecipient().getHolderName()).isEqualTo("김시니어");
        assertThat(result.getTransaction().getTransactionId()).isEqualTo(999L);
        assertThat(result.getTransaction().getStatus()).isEqualTo("REQUESTED");
        assertThat(result.getTransaction().getType()).isEqualTo("TRANSFER_OUT");
        assertThat(result.getInquiryResponse()).isEqualTo(inquiryResponse);

        verify(recipientMapper).insertRecipient(any(Recipient.class));
        verify(recipientMapper, never()).updateSendInfo(any());
    }

    @Test
    void 기존_수취인이면_등록없이_송금정보만_갱신한다() {
        given(walletMapper.findWalletByUserId(userId)).willReturn(wallet);
        given(userMapper.findById(userId)).willReturn(user);
        given(passwordEncoder.matches(request.getTransferPin(), user.getPin())).willReturn(true);

        RealNameInquiryResponse inquiryResponse = new RealNameInquiryResponse();
        inquiryResponse.setRspCode("A0000");
        inquiryResponse.setBankName("KB국민은행");
        inquiryResponse.setAccountHolderName("김시니어");
        given(openBankingClient.inquireRealName("004", "11012300006781", null))
                .willReturn(inquiryResponse);

        Recipient existingRecipient = Recipient.builder()
                .recipientId(200L)
                .wardId(userId)
                .bankCode("004")
                .accountNo("11012300006781")
                .holderName("김시니어")
                .sendCount(3)
                .build();
        given(recipientMapper.findRecipient(userId, "004", "11012300006781")).willReturn(existingRecipient);

        doAnswer(invocation -> {
            Transaction t = invocation.getArgument(0);
            t.setTransactionId(999L);
            return null;
        }).when(transactionMapper).insertTransaction(any(Transaction.class));

        PreparedTransfer result = transferPreparationService.prepare(userId, request);

        assertThat(result.getRecipient()).isEqualTo(existingRecipient);
        assertThat(result.getTransaction().getRecipientId()).isEqualTo(200L);

        verify(recipientMapper).updateSendInfo(200L);
        verify(recipientMapper, never()).insertRecipient(any());
    }
}
