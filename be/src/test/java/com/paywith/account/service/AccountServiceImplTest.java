package com.paywith.account.service;

import com.paywith.account.domain.Account;
import com.paywith.account.dto.AccountCreateRequest;
import com.paywith.account.dto.AccountResponse;
import com.paywith.account.mapper.AccountMapper;
import com.paywith.exception.BusinessException;
import com.paywith.external.openbanking.OpenBankingClient;
import com.paywith.external.openbanking.dto.RealNameInquiryResponse;
import com.paywith.user.domain.User;
import com.paywith.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

    @Mock
    private AccountMapper accountMapper;

    @Mock
    private OpenBankingClient openBankingClient;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private AccountServiceImpl accountService;

    private final Long userId = 1L;
    private AccountCreateRequest request;
    private User user;

    @BeforeEach
    void setUp() {
        request = new AccountCreateRequest("004", "11012300006781", "1234");
        user = new User();
        user.setId(userId);
        user.setBirthDate(LocalDate.of(1990, 1, 1));
    }

    // ===== registerAccount =====

    @Test
    void 이미_등록된_계좌면_예외를_던진다() {
        given(accountMapper.existsByUserIdAndAccount(userId, "004", "11012300006781")).willReturn(true);

        assertThatThrownBy(() -> accountService.registerAccount(userId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT)
                .hasMessageContaining("이미 등록된 계좌입니다.");

        verify(userMapper, never()).findById(any());
        verify(accountMapper, never()).insertAccount(any());
    }

    @Test
    void 실명조회에_실패하면_예외를_던진다() {
        given(accountMapper.existsByUserIdAndAccount(userId, "004", "11012300006781")).willReturn(false);
        given(userMapper.findById(userId)).willReturn(user);

        RealNameInquiryResponse failResponse = new RealNameInquiryResponse();
        failResponse.setRspCode("A0004");
        failResponse.setRspMessage("계좌 정보 불일치");
        given(openBankingClient.inquireRealName("004", "11012300006781", "1990-01-01"))
                .willReturn(failResponse);

        assertThatThrownBy(() -> accountService.registerAccount(userId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                .hasMessageContaining("계좌 실명조회에 실패했습니다.")
                .hasMessageContaining("계좌 정보 불일치");

        verify(accountMapper, never()).insertAccount(any());
    }

    @Test
    void 정상_등록시_계좌_정보를_저장하고_응답을_반환한다() {
        given(accountMapper.existsByUserIdAndAccount(userId, "004", "11012300006781")).willReturn(false);
        given(userMapper.findById(userId)).willReturn(user);

        RealNameInquiryResponse inquiryResponse = new RealNameInquiryResponse();
        inquiryResponse.setRspCode("A0000");
        inquiryResponse.setBankName("KB국민은행");
        inquiryResponse.setAccountHolderName("홍길동");
        given(openBankingClient.inquireRealName("004", "11012300006781", "1990-01-01"))
                .willReturn(inquiryResponse);

        doAnswer(invocation -> {
            Account account = invocation.getArgument(0);
            account.setAccountId(100L);
            return null;
        }).when(accountMapper).insertAccount(any(Account.class));

        AccountResponse response = accountService.registerAccount(userId, request);

        assertThat(response.getAccountId()).isEqualTo(100L);
        assertThat(response.getBankCode()).isEqualTo("004");
        assertThat(response.getBankName()).isEqualTo("KB국민은행");
        assertThat(response.getAccountNo()).isEqualTo("11012300006781");

        verify(accountMapper).insertAccount(any(Account.class));
    }

    // ===== getAccounts =====

    @Test
    void 계좌_목록을_그대로_반환한다() {
        AccountResponse account = AccountResponse.builder()
                .accountId(1L)
                .bankCode("004")
                .bankName("KB국민은행")
                .accountNo("11012300006781")
                .build();
        given(accountMapper.findAccountsByUserId(userId)).willReturn(List.of(account));

        List<AccountResponse> result = accountService.getAccounts(userId);

        assertThat(result).containsExactly(account);
    }

    // ===== verifyOwnership =====

    @Test
    void 본인_소유_계좌면_true를_반환한다() {
        given(accountMapper.existsByUserIdAndAccountId(userId, 1L)).willReturn(true);

        assertThat(accountService.verifyOwnership(userId, 1L)).isTrue();
    }

    @Test
    void 본인_소유_계좌가_아니면_false를_반환한다() {
        given(accountMapper.existsByUserIdAndAccountId(userId, 1L)).willReturn(false);

        assertThat(accountService.verifyOwnership(userId, 1L)).isFalse();
    }

    // ===== getAccountDetail =====

    @Test
    void 계좌_상세_정보를_그대로_반환한다() {
        AccountResponse account = AccountResponse.builder()
                .accountId(1L)
                .bankCode("004")
                .bankName("KB국민은행")
                .accountNo("11012300006781")
                .build();
        given(accountMapper.findAccountById(1L)).willReturn(account);

        AccountResponse result = accountService.getAccountDetail(1L);

        assertThat(result).isEqualTo(account);
    }
}
