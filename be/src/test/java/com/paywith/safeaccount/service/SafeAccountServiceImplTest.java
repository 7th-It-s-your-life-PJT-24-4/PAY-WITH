package com.paywith.safeaccount.service;

import com.paywith.exception.BusinessException;
import com.paywith.external.openbanking.OpenBankingClient;
import com.paywith.external.openbanking.dto.RealNameInquiryResponse;
import com.paywith.guard.service.GuardService;
import com.paywith.recipient.domain.Recipient;
import com.paywith.recipient.mapper.BankMapper;
import com.paywith.recipient.mapper.RecipientMapper;
import com.paywith.safeaccount.dto.GuardSafeAccountRegisterRequest;
import com.paywith.safeaccount.dto.SafeAccountDeleteResponse;
import com.paywith.safeaccount.dto.SafeAccountListItem;
import com.paywith.safeaccount.dto.SafeAccountListResponse;
import com.paywith.safeaccount.dto.SafeAccountRegisterRequest;
import com.paywith.safeaccount.dto.SafeAccountResponse;
import com.paywith.safeaccount.mapper.SafeAccountMapper;
import com.paywith.user.domain.Role;
import com.paywith.user.domain.User;
import com.paywith.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SafeAccountServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private RecipientMapper recipientMapper;

    @Mock
    private BankMapper bankMapper;

    @Mock
    private SafeAccountMapper safeAccountMapper;

    @Mock
    private GuardService guardService;

    @Mock
    private OpenBankingClient openBankingClient;

    @InjectMocks
    private SafeAccountServiceImpl safeAccountService;

    private final Long wardId = 1L;
    private final Long guardId = 2L;
    private final Long recipientId = 200L;
    private SafeAccountRegisterRequest request;
    private Recipient recipient;

    @BeforeEach
    void setUp() {
        request = SafeAccountRegisterRequest.builder()
                .recipientId(recipientId)
                .accountAlias("용돈용")
                .build();

        recipient = Recipient.builder()
                .recipientId(recipientId)
                .wardId(wardId)
                .bankCode("004")
                .accountNo("11012300006781")
                .holderName("김수취")
                .isRegisteredSafe(false)
                .safeRegisteredAt(null)
                .build();
    }

    private User userWithRole(Role role) {
        User user = new User();
        user.setId(wardId);
        user.setRole(role);
        return user;
    }

    @Test
    void 피보호자가_아니면_예외를_던진다() {
        given(userMapper.findById(wardId)).willReturn(userWithRole(Role.GUARD));

        assertThatThrownBy(() -> safeAccountService.registerByWard(wardId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN)
                .hasMessageContaining("피보호자만 접근할 수 있습니다.");

        verify(recipientMapper, never()).existsActivePairing(any());
    }

    @Test
    void 활성_페어링이_없으면_예외를_던진다() {
        given(userMapper.findById(wardId)).willReturn(userWithRole(Role.WARD));
        given(recipientMapper.existsActivePairing(wardId)).willReturn(false);

        assertThatThrownBy(() -> safeAccountService.registerByWard(wardId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN)
                .hasMessageContaining("페어링 완료 후 이용할 수 있습니다.");

        verify(recipientMapper, never()).findById(any());
    }

    @Test
    void 별칭이_50자를_초과하면_예외를_던진다() {
        given(userMapper.findById(wardId)).willReturn(userWithRole(Role.WARD));
        given(recipientMapper.existsActivePairing(wardId)).willReturn(true);
        request = SafeAccountRegisterRequest.builder()
                .recipientId(recipientId)
                .accountAlias("가".repeat(51))
                .build();

        assertThatThrownBy(() -> safeAccountService.registerByWard(wardId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                .hasMessageContaining("입력값이 올바르지 않습니다.");

        verify(recipientMapper, never()).findById(any());
    }

    @Test
    void recipientId가_없으면_예외를_던진다() {
        given(userMapper.findById(wardId)).willReturn(userWithRole(Role.WARD));
        given(recipientMapper.existsActivePairing(wardId)).willReturn(true);
        request = SafeAccountRegisterRequest.builder()
                .recipientId(null)
                .accountAlias("용돈용")
                .build();

        assertThatThrownBy(() -> safeAccountService.registerByWard(wardId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasMessageContaining("송금 이력이 있는 수취인을 찾을 수 없습니다.");

        verify(recipientMapper, never()).findById(any());
    }

    @Test
    void 다른_피보호자의_수취인이면_예외를_던진다() {
        given(userMapper.findById(wardId)).willReturn(userWithRole(Role.WARD));
        given(recipientMapper.existsActivePairing(wardId)).willReturn(true);
        Recipient otherWardRecipient = Recipient.builder()
                .recipientId(recipientId)
                .wardId(999L)
                .build();
        given(recipientMapper.findById(recipientId)).willReturn(otherWardRecipient);

        assertThatThrownBy(() -> safeAccountService.registerByWard(wardId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasMessageContaining("송금 이력이 있는 수취인을 찾을 수 없습니다.");

        verify(safeAccountMapper, never()).existsCompletedTransfer(any(), any());
    }

    @Test
    void 완료된_송금_이력이_없으면_예외를_던진다() {
        given(userMapper.findById(wardId)).willReturn(userWithRole(Role.WARD));
        given(recipientMapper.existsActivePairing(wardId)).willReturn(true);
        given(recipientMapper.findById(recipientId)).willReturn(recipient);
        given(safeAccountMapper.existsCompletedTransfer(wardId, recipientId)).willReturn(false);

        assertThatThrownBy(() -> safeAccountService.registerByWard(wardId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasMessageContaining("송금 이력이 있는 수취인을 찾을 수 없습니다.");

        verify(safeAccountMapper, never()).registerSafeAccount(any(), any(), any());
    }

    @Test
    void 이미_안전계좌로_등록되어_있으면_예외를_던진다() {
        given(userMapper.findById(wardId)).willReturn(userWithRole(Role.WARD));
        given(recipientMapper.existsActivePairing(wardId)).willReturn(true);
        recipient.setIsRegisteredSafe(true);
        given(recipientMapper.findById(recipientId)).willReturn(recipient);
        given(safeAccountMapper.existsCompletedTransfer(wardId, recipientId)).willReturn(true);

        assertThatThrownBy(() -> safeAccountService.registerByWard(wardId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT)
                .hasMessageContaining("이미 등록된 안전계좌입니다.");

        verify(safeAccountMapper, never()).registerSafeAccount(any(), any(), any());
    }

    @Test
    void 동시_등록_요청으로_UPDATE가_반영되지_않으면_예외를_던진다() {
        given(userMapper.findById(wardId)).willReturn(userWithRole(Role.WARD));
        given(recipientMapper.existsActivePairing(wardId)).willReturn(true);
        given(recipientMapper.findById(recipientId)).willReturn(recipient);
        given(safeAccountMapper.existsCompletedTransfer(wardId, recipientId)).willReturn(true);
        given(safeAccountMapper.registerSafeAccount(any(), any(), any())).willReturn(0);

        assertThatThrownBy(() -> safeAccountService.registerByWard(wardId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT)
                .hasMessageContaining("이미 등록된 안전계좌입니다.");
    }

    @Test
    void 신규_등록시_newlyRegistered가_true인_응답을_반환한다() {
        given(userMapper.findById(wardId)).willReturn(userWithRole(Role.WARD));
        given(recipientMapper.existsActivePairing(wardId)).willReturn(true);
        given(recipientMapper.findById(recipientId)).willReturn(recipient);
        given(safeAccountMapper.existsCompletedTransfer(wardId, recipientId)).willReturn(true);
        given(bankMapper.findBankName("004")).willReturn("KB국민은행");
        given(safeAccountMapper.registerSafeAccount(any(), any(), any())).willReturn(1);

        SafeAccountResponse response = safeAccountService.registerByWard(wardId, request);

        assertThat(response.getSafeAccountId()).isEqualTo(recipientId);
        assertThat(response.getRecipientId()).isEqualTo(recipientId);
        assertThat(response.getBankCode()).isEqualTo("004");
        assertThat(response.getBankName()).isEqualTo("KB국민은행");
        assertThat(response.getAccountNo()).isEqualTo("11012300006781");
        assertThat(response.getHolderName()).isEqualTo("김수취");
        assertThat(response.getAccountAlias()).isEqualTo("용돈용");
        assertThat(response.getIsVerified()).isTrue();
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
        assertThat(response.getNewlyRegistered()).isTrue();

        verify(safeAccountMapper).registerSafeAccount(eq(recipientId), isNull(), eq("용돈용"));
    }

    @Test
    void 복구_등록시_newlyRegistered가_false인_응답을_반환한다() {
        given(userMapper.findById(wardId)).willReturn(userWithRole(Role.WARD));
        given(recipientMapper.existsActivePairing(wardId)).willReturn(true);
        recipient.setSafeRegisteredAt(LocalDateTime.now().minusDays(10));
        given(recipientMapper.findById(recipientId)).willReturn(recipient);
        given(safeAccountMapper.existsCompletedTransfer(wardId, recipientId)).willReturn(true);
        given(bankMapper.findBankName("004")).willReturn("KB국민은행");
        given(safeAccountMapper.registerSafeAccount(any(), any(), any())).willReturn(1);

        SafeAccountResponse response = safeAccountService.registerByWard(wardId, request);

        assertThat(response.getNewlyRegistered()).isFalse();
    }

    @Test
    void 별칭이_비어있으면_null로_정규화되어_등록된다() {
        given(userMapper.findById(wardId)).willReturn(userWithRole(Role.WARD));
        given(recipientMapper.existsActivePairing(wardId)).willReturn(true);
        given(recipientMapper.findById(recipientId)).willReturn(recipient);
        given(safeAccountMapper.existsCompletedTransfer(wardId, recipientId)).willReturn(true);
        given(bankMapper.findBankName("004")).willReturn("KB국민은행");
        given(safeAccountMapper.registerSafeAccount(any(), any(), any())).willReturn(1);
        request = SafeAccountRegisterRequest.builder()
                .recipientId(recipientId)
                .accountAlias("  ")
                .build();

        SafeAccountResponse response = safeAccountService.registerByWard(wardId, request);

        assertThat(response.getAccountAlias()).isNull();
        verify(safeAccountMapper).registerSafeAccount(eq(recipientId), isNull(), isNull());
    }

    // ===== registerByGuard =====

    private GuardSafeAccountRegisterRequest guardRequest;

    @BeforeEach
    void setUpGuardRequest() {
        guardRequest = GuardSafeAccountRegisterRequest.builder()
                .bankCode("004")
                .accountNo("11012300006781")
                .accountAlias("용돈용")
                .build();
    }

    @Test
    void 보호자_등록시_페어링된_보호자가_아니면_예외를_던진다() {
        given(guardService.verifyGuardOfWard(guardId, wardId)).willReturn(false);

        assertThatThrownBy(() -> safeAccountService.registerByGuard(guardId, wardId, guardRequest))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasMessageContaining("연동된 피보호자를 찾을 수 없습니다.");

        verify(recipientMapper, never()).findRecipient(any(), any(), any());
    }

    @Test
    void 보호자_등록시_별칭이_50자를_초과하면_예외를_던진다() {
        given(guardService.verifyGuardOfWard(guardId, wardId)).willReturn(true);
        guardRequest = GuardSafeAccountRegisterRequest.builder()
                .bankCode("004")
                .accountNo("11012300006781")
                .accountAlias("가".repeat(51))
                .build();

        assertThatThrownBy(() -> safeAccountService.registerByGuard(guardId, wardId, guardRequest))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                .hasMessageContaining("입력값이 올바르지 않습니다.");

        verify(recipientMapper, never()).findRecipient(any(), any(), any());
    }

    @Test
    void 보호자_등록시_기존_계좌가_이미_안전계좌면_예외를_던진다() {
        given(guardService.verifyGuardOfWard(guardId, wardId)).willReturn(true);
        recipient.setIsRegisteredSafe(true);
        given(recipientMapper.findRecipient(wardId, "004", "11012300006781")).willReturn(recipient);

        assertThatThrownBy(() -> safeAccountService.registerByGuard(guardId, wardId, guardRequest))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT)
                .hasMessageContaining("이미 등록된 안전 계좌 입니다.");

        verify(safeAccountMapper, never()).registerSafeAccount(any(), any(), any());
    }

    @Test
    void 보호자_등록시_동시_등록_요청으로_UPDATE가_반영되지_않으면_예외를_던진다() {
        given(guardService.verifyGuardOfWard(guardId, wardId)).willReturn(true);
        given(recipientMapper.findRecipient(wardId, "004", "11012300006781")).willReturn(recipient);
        given(safeAccountMapper.registerSafeAccount(any(), any(), any())).willReturn(0);

        assertThatThrownBy(() -> safeAccountService.registerByGuard(guardId, wardId, guardRequest))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT)
                .hasMessageContaining("이미 등록된 안전 계좌 입니다.");
    }

    @Test
    void 보호자_등록시_기존_계좌_신규등록이면_newlyRegistered가_true다() {
        given(guardService.verifyGuardOfWard(guardId, wardId)).willReturn(true);
        given(recipientMapper.findRecipient(wardId, "004", "11012300006781")).willReturn(recipient);
        given(bankMapper.findBankName("004")).willReturn("KB국민은행");
        given(safeAccountMapper.registerSafeAccount(any(), any(), any())).willReturn(1);

        SafeAccountResponse response = safeAccountService.registerByGuard(guardId, wardId, guardRequest);

        assertThat(response.getSafeAccountId()).isEqualTo(recipientId);
        assertThat(response.getRecipientId()).isEqualTo(recipientId);
        assertThat(response.getBankName()).isEqualTo("KB국민은행");
        assertThat(response.getHolderName()).isEqualTo("김수취");
        assertThat(response.getNewlyRegistered()).isTrue();

        verify(safeAccountMapper).registerSafeAccount(recipientId, guardId, "용돈용");
        verify(openBankingClient, never()).inquireRealName(any(), any(), any());
    }

    @Test
    void 보호자_등록시_기존_계좌_복구등록이면_newlyRegistered가_false다() {
        given(guardService.verifyGuardOfWard(guardId, wardId)).willReturn(true);
        recipient.setSafeRegisteredAt(LocalDateTime.now().minusDays(10));
        given(recipientMapper.findRecipient(wardId, "004", "11012300006781")).willReturn(recipient);
        given(bankMapper.findBankName("004")).willReturn("KB국민은행");
        given(safeAccountMapper.registerSafeAccount(any(), any(), any())).willReturn(1);

        SafeAccountResponse response = safeAccountService.registerByGuard(guardId, wardId, guardRequest);

        assertThat(response.getNewlyRegistered()).isFalse();
    }

    @Test
    void 보호자_등록시_송금이력이_없는_신규_계좌면_실명조회_후_등록한다() {
        given(guardService.verifyGuardOfWard(guardId, wardId)).willReturn(true);
        given(recipientMapper.findRecipient(wardId, "004", "11012300006781")).willReturn(null);

        RealNameInquiryResponse inquiry = new RealNameInquiryResponse();
        inquiry.setRspCode("A0000");
        inquiry.setBankName("KB국민은행");
        inquiry.setAccountHolderName("홍길동");
        given(openBankingClient.inquireRealName("004", "11012300006781", null)).willReturn(inquiry);
        given(safeAccountMapper.insertSafeAccountByGuard(
                wardId, "004", "11012300006781", "홍길동", guardId, "용돈용"))
                .willReturn(300L);

        SafeAccountResponse response = safeAccountService.registerByGuard(guardId, wardId, guardRequest);

        assertThat(response.getSafeAccountId()).isEqualTo(300L);
        assertThat(response.getRecipientId()).isEqualTo(300L);
        assertThat(response.getBankName()).isEqualTo("KB국민은행");
        assertThat(response.getHolderName()).isEqualTo("홍길동");
        assertThat(response.getNewlyRegistered()).isTrue();

        verify(safeAccountMapper, never()).registerSafeAccount(any(), any(), any());
    }

    @Test
    void 보호자_등록시_신규_계좌_실명조회가_실패하면_예외를_던진다() {
        given(guardService.verifyGuardOfWard(guardId, wardId)).willReturn(true);
        given(recipientMapper.findRecipient(wardId, "004", "11012300006781")).willReturn(null);

        RealNameInquiryResponse inquiry = new RealNameInquiryResponse();
        inquiry.setRspCode("A0004");
        given(openBankingClient.inquireRealName("004", "11012300006781", null)).willReturn(inquiry);

        assertThatThrownBy(() -> safeAccountService.registerByGuard(guardId, wardId, guardRequest))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasMessageContaining("해당 계좌를 찾을 수 없습니다.");

        verify(safeAccountMapper, never()).insertSafeAccountByGuard(any(), any(), any(), any(), any(), any());
    }

    // ===== getSafeAccountListByWard =====

    private SafeAccountListItem listItem;

    @BeforeEach
    void setUpListItem() {
        listItem = SafeAccountListItem.builder()
                .safeAccountId(recipientId)
                .recipientId(recipientId)
                .bankCode("004")
                .bankName("KB국민은행")
                .accountNo("11012300006781")
                .holderName("김수취")
                .accountAlias("용돈용")
                .isVerified(true)
                .build();
    }

    @Test
    void 목록_조회시_피보호자가_아니면_예외를_던진다() {
        given(userMapper.findById(wardId)).willReturn(userWithRole(Role.GUARD));

        assertThatThrownBy(() -> safeAccountService.getSafeAccountListByWard(wardId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN)
                .hasMessageContaining("피보호자만 접근할 수 있습니다.");

        verify(recipientMapper, never()).existsActivePairing(any());
        verify(safeAccountMapper, never()).findSafeAccountList(any());
    }

    @Test
    void 목록_조회시_활성_페어링이_없으면_예외를_던진다() {
        given(userMapper.findById(wardId)).willReturn(userWithRole(Role.WARD));
        given(recipientMapper.existsActivePairing(wardId)).willReturn(false);

        assertThatThrownBy(() -> safeAccountService.getSafeAccountListByWard(wardId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN)
                .hasMessageContaining("페어링 완료 후 이용할 수 있습니다.");

        verify(safeAccountMapper, never()).findSafeAccountList(any());
    }

    @Test
    void 피보호자는_안전계좌_목록에_recipientId가_포함된_상태로_조회한다() {
        given(userMapper.findById(wardId)).willReturn(userWithRole(Role.WARD));
        given(recipientMapper.existsActivePairing(wardId)).willReturn(true);
        given(safeAccountMapper.findSafeAccountList(wardId)).willReturn(List.of(listItem));

        SafeAccountListResponse response = safeAccountService.getSafeAccountListByWard(wardId);

        assertThat(response.getSafeAccounts()).hasSize(1);
        assertThat(response.getSafeAccounts().get(0).getRecipientId()).isEqualTo(recipientId);
    }

    // ===== getSafeAccountListByGuard =====

    @Test
    void 보호자_목록_조회시_페어링된_보호자가_아니면_예외를_던진다() {
        given(guardService.verifyGuardOfWard(guardId, wardId)).willReturn(false);

        assertThatThrownBy(() -> safeAccountService.getSafeAccountListByGuard(guardId, wardId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN)
                .hasMessageContaining("해당 시니어의 보호자가 아닙니다.");

        verify(safeAccountMapper, never()).findSafeAccountList(any());
    }

    @Test
    void 보호자는_안전계좌_목록에_recipientId가_노출되지_않는다() {
        given(guardService.verifyGuardOfWard(guardId, wardId)).willReturn(true);
        given(safeAccountMapper.findSafeAccountList(wardId)).willReturn(List.of(listItem));

        SafeAccountListResponse response = safeAccountService.getSafeAccountListByGuard(guardId, wardId);

        assertThat(response.getSafeAccounts()).hasSize(1);
        assertThat(response.getSafeAccounts().get(0).getRecipientId()).isNull();
    }

    // ===== deactivateByWard =====

    @Test
    void 본인_삭제시_피보호자가_아니면_예외를_던진다() {
        given(userMapper.findById(wardId)).willReturn(userWithRole(Role.GUARD));

        assertThatThrownBy(() -> safeAccountService.deactivateByWard(wardId, recipientId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN)
                .hasMessageContaining("안전계좌를 찾을 수 없습니다.");

        verify(recipientMapper, never()).findById(any());
    }

    @Test
    void 본인_삭제시_안전계좌가_존재하지_않으면_예외를_던진다() {
        given(userMapper.findById(wardId)).willReturn(userWithRole(Role.WARD));
        given(recipientMapper.findById(recipientId)).willReturn(null);

        assertThatThrownBy(() -> safeAccountService.deactivateByWard(wardId, recipientId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasMessageContaining("안전계좌를 찾을 수 없습니다.");

        verify(safeAccountMapper, never()).deactivateSafeAccount(any());
    }

    @Test
    void 본인_삭제시_다른_피보호자의_안전계좌면_예외를_던진다() {
        given(userMapper.findById(wardId)).willReturn(userWithRole(Role.WARD));
        Recipient otherWardRecipient = Recipient.builder()
                .recipientId(recipientId)
                .wardId(999L)
                .safeRegisteredAt(LocalDateTime.now())
                .build();
        given(recipientMapper.findById(recipientId)).willReturn(otherWardRecipient);

        assertThatThrownBy(() -> safeAccountService.deactivateByWard(wardId, recipientId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasMessageContaining("안전계좌를 찾을 수 없습니다.");

        verify(safeAccountMapper, never()).deactivateSafeAccount(any());
    }

    @Test
    void 본인_삭제시_안전계좌로_등록된_적_없으면_예외를_던진다() {
        given(userMapper.findById(wardId)).willReturn(userWithRole(Role.WARD));
        recipient.setSafeRegisteredAt(null);
        given(recipientMapper.findById(recipientId)).willReturn(recipient);

        assertThatThrownBy(() -> safeAccountService.deactivateByWard(wardId, recipientId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasMessageContaining("안전계좌를 찾을 수 없습니다.");

        verify(safeAccountMapper, never()).deactivateSafeAccount(any());
    }

    @Test
    void 본인_삭제시_정상적으로_비활성화하고_INACTIVE_응답을_반환한다() {
        given(userMapper.findById(wardId)).willReturn(userWithRole(Role.WARD));
        recipient.setSafeRegisteredAt(LocalDateTime.now().minusDays(10));
        given(recipientMapper.findById(recipientId)).willReturn(recipient);

        SafeAccountDeleteResponse response = safeAccountService.deactivateByWard(wardId, recipientId);

        assertThat(response.getSafeAccountId()).isEqualTo(recipientId);
        assertThat(response.getStatus()).isEqualTo("INACTIVE");

        verify(safeAccountMapper).deactivateSafeAccount(recipientId);
    }

    // ===== deactivateByGuard =====

    @Test
    void 보호자_삭제시_페어링된_보호자가_아니면_예외를_던진다() {
        given(guardService.verifyGuardOfWard(guardId, wardId)).willReturn(false);

        assertThatThrownBy(() -> safeAccountService.deactivateByGuard(guardId, wardId, recipientId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasMessageContaining("연동된 피보호자를 찾을 수 없습니다.");

        verify(recipientMapper, never()).findById(any());
    }

    @Test
    void 보호자_삭제시_대상이_이_피보호자의_안전계좌가_아니면_예외를_던진다() {
        given(guardService.verifyGuardOfWard(guardId, wardId)).willReturn(true);
        Recipient otherWardRecipient = Recipient.builder()
                .recipientId(recipientId)
                .wardId(999L)
                .safeRegisteredAt(LocalDateTime.now())
                .build();
        given(recipientMapper.findById(recipientId)).willReturn(otherWardRecipient);

        assertThatThrownBy(() -> safeAccountService.deactivateByGuard(guardId, wardId, recipientId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasMessageContaining("안전계좌를 찾을 수 없습니다.");

        verify(safeAccountMapper, never()).deactivateSafeAccount(any());
    }

    @Test
    void 보호자_삭제시_정상적으로_비활성화하고_INACTIVE_응답을_반환한다() {
        given(guardService.verifyGuardOfWard(guardId, wardId)).willReturn(true);
        recipient.setSafeRegisteredAt(LocalDateTime.now().minusDays(10));
        given(recipientMapper.findById(recipientId)).willReturn(recipient);

        SafeAccountDeleteResponse response = safeAccountService.deactivateByGuard(guardId, wardId, recipientId);

        assertThat(response.getSafeAccountId()).isEqualTo(recipientId);
        assertThat(response.getStatus()).isEqualTo("INACTIVE");

        verify(safeAccountMapper).deactivateSafeAccount(recipientId);
    }
}
