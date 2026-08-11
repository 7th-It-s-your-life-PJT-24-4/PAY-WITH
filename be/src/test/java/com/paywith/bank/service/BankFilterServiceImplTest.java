package com.paywith.bank.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import static org.mockito.Mockito.lenient;

import com.paywith.bank.domain.Bank;
import com.paywith.bank.dto.BankCandidateResponse;
import com.paywith.bank.dto.BankFilterResponse;
import com.paywith.bank.mapper.BankFilterMapper;
import com.paywith.exception.BusinessException;
import com.paywith.user.domain.Role;
import com.paywith.user.domain.User;
import com.paywith.user.mapper.UserMapper;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BankFilterServiceImplTest {

    @Mock
    private BankFilterMapper bankMapper;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private BankFilterServiceImpl bankFilterService;

    private static final Long WARD_ID = 1L;

    /** 테스트용 활성 은행 목록(seed 기준 22개 은행) */
    private List<Bank> allActiveBanks;

    @BeforeEach
    void setUp() {
        // 활성 사용자(WARD)
        User ward = new User();
        ward.setRole(Role.WARD);
        given(userMapper.findById(WARD_ID)).willReturn(ward);
        given(bankMapper.existsActivePairing(WARD_ID)).willReturn(true);

        // 모든 은행 활성
        allActiveBanks = List.of(
            bank("002"), bank("003"), bank("004"), bank("007"),
            bank("011"), bank("020"), bank("023"), bank("027"),
            bank("031"), bank("032"), bank("034"), bank("035"),
            bank("037"), bank("039"), bank("045"), bank("048"),
            bank("071"), bank("081"), bank("088"), bank("089"),
            bank("090"), bank("092")
        );
        // InputValidation 테스트에서는 호출되지 않으므로 lenient stub으로 설정
        lenient().when(bankMapper.findAllActive()).thenReturn(allActiveBanks);
    }

    private Bank bank(String code) {
        Bank b = new Bank();
        b.setBankCode(code);
        b.setBankName(code + "_BANK");
        b.setActive(true);
        return b;
    }

    private List<String> codesOf(BankFilterResponse response) {
        return response.getBanks().stream()
            .map(BankCandidateResponse::getBankCode)
            .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────
    // 입력값 검증
    // ─────────────────────────────────────────────────

    @Nested
    @DisplayName("계좌번호 입력 검증")
    class InputValidation {

        @Test
        @DisplayName("null 계좌번호 → ACCOUNT_001 예외")
        void nullAccountNo_throwsException() {
            assertThatThrownBy(() -> bankFilterService.filterBanks(WARD_ID, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("계좌번호 형식이 올바르지 않습니다.");
        }

        @Test
        @DisplayName("비숫자 포함 계좌번호 → ACCOUNT_001 예외")
        void nonDigitAccountNo_throwsException() {
            assertThatThrownBy(() -> bankFilterService.filterBanks(WARD_ID, "123-456-789"))
                .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("빈 문자열 → ACCOUNT_001 예외")
        void emptyAccountNo_throwsException() {
            assertThatThrownBy(() -> bankFilterService.filterBanks(WARD_ID, ""))
                .isInstanceOf(BusinessException.class);
        }
    }

    // ─────────────────────────────────────────────────
    // 카카오뱅크(090) — 13자리 + prefix 조건
    // ─────────────────────────────────────────────────

    @Nested
    @DisplayName("카카오뱅크(090) 필터링")
    class KakaoBank {

        @Test
        @DisplayName("13자리 + 3333 prefix → 카카오뱅크 포함")
        void validKakao_included() {
            BankFilterResponse result = bankFilterService.filterBanks(WARD_ID, "3333012345678");
            assertThat(codesOf(result)).contains("090");
        }

        @Test
        @DisplayName("13자리 + 3355 prefix → 카카오뱅크 포함")
        void kakaoSaving_included() {
            BankFilterResponse result = bankFilterService.filterBanks(WARD_ID, "3355012345678");
            assertThat(codesOf(result)).contains("090");
        }

        @Test
        @DisplayName("13자리 + 7777 prefix → 카카오뱅크 포함")
        void kakaoMini_included() {
            BankFilterResponse result = bankFilterService.filterBanks(WARD_ID, "7777012345678");
            assertThat(codesOf(result)).contains("090");
        }

        @Test
        @DisplayName("13자리 + 9999 prefix → 카카오뱅크 제외")
        void invalidPrefix_excluded() {
            BankFilterResponse result = bankFilterService.filterBanks(WARD_ID, "9999012345678");
            assertThat(codesOf(result)).doesNotContain("090");
        }

        @Test
        @DisplayName("10자리 → 카카오뱅크 제외(길이 불일치)")
        void wrongLength_excluded() {
            BankFilterResponse result = bankFilterService.filterBanks(WARD_ID, "3333012345");
            assertThat(codesOf(result)).doesNotContain("090");
        }
    }

    // ─────────────────────────────────────────────────
    // 케이뱅크(089) — 12자리 + prefix 조건
    // ─────────────────────────────────────────────────

    @Nested
    @DisplayName("케이뱅크(089) 필터링")
    class KBank {

        @Test
        @DisplayName("12자리 + 1001 prefix → 케이뱅크 포함")
        void validKBank_included() {
            BankFilterResponse result = bankFilterService.filterBanks(WARD_ID, "100112345678");
            assertThat(codesOf(result)).contains("089");
        }

        @Test
        @DisplayName("12자리 + 1002 prefix → 케이뱅크 포함")
        void kbankSavings_included() {
            BankFilterResponse result = bankFilterService.filterBanks(WARD_ID, "100212345678");
            assertThat(codesOf(result)).contains("089");
        }

        @Test
        @DisplayName("12자리 + 9999 prefix → 케이뱅크 제외")
        void invalidPrefix_excluded() {
            BankFilterResponse result = bankFilterService.filterBanks(WARD_ID, "999912345678");
            assertThat(codesOf(result)).doesNotContain("089");
        }
    }

    // ─────────────────────────────────────────────────
    // 토스뱅크(092) — 12자리 + prefix 조건
    // ─────────────────────────────────────────────────

    @Nested
    @DisplayName("토스뱅크(092) 필터링")
    class TossBank {

        @Test
        @DisplayName("12자리 + 1000 prefix → 토스뱅크 포함")
        void validToss_included() {
            BankFilterResponse result = bankFilterService.filterBanks(WARD_ID, "100012345678");
            assertThat(codesOf(result)).contains("092");
        }

        @Test
        @DisplayName("12자리 + 2000 prefix → 토스뱅크 제외")
        void invalidPrefix_excluded() {
            BankFilterResponse result = bankFilterService.filterBanks(WARD_ID, "200012345678");
            assertThat(codesOf(result)).doesNotContain("092");
        }
    }

    // ─────────────────────────────────────────────────
    // 자릿수 범위 검사 — 하나은행(081, 11~14자리)
    // ─────────────────────────────────────────────────

    @Nested
    @DisplayName("하나은행(081) 자릿수 범위 필터링")
    class HanaBank {

        @Test
        @DisplayName("11자리 → 하나은행 포함(범위 내 최솟값)")
        void minLength_included() {
            BankFilterResponse result = bankFilterService.filterBanks(WARD_ID, "10234567890");
            assertThat(codesOf(result)).contains("081");
        }

        @Test
        @DisplayName("14자리 → 하나은행 포함(범위 내 최댓값)")
        void maxLength_included() {
            BankFilterResponse result = bankFilterService.filterBanks(WARD_ID, "10234567890123");
            assertThat(codesOf(result)).contains("081");
        }

        @Test
        @DisplayName("15자리 → 하나은행 제외(범위 초과)")
        void overMaxLength_excluded() {
            BankFilterResponse result = bankFilterService.filterBanks(WARD_ID, "102345678901234");
            assertThat(codesOf(result)).doesNotContain("081");
        }

        @Test
        @DisplayName("10자리 → 하나은행 제외(범위 미달)")
        void underMinLength_excluded() {
            BankFilterResponse result = bankFilterService.filterBanks(WARD_ID, "1023456789");
            assertThat(codesOf(result)).doesNotContain("081");
        }
    }

    // ─────────────────────────────────────────────────
    // 우리은행(020) — 정확히 13자리
    // ─────────────────────────────────────────────────

    @Nested
    @DisplayName("우리은행(020) 자릿수 필터링")
    class WooriBank {

        @Test
        @DisplayName("13자리 → 우리은행 포함")
        void exactLength_included() {
            BankFilterResponse result = bankFilterService.filterBanks(WARD_ID, "1002345678901");
            assertThat(codesOf(result)).contains("020");
        }

        @Test
        @DisplayName("12자리 → 우리은행 제외")
        void wrongLength_excluded() {
            BankFilterResponse result = bankFilterService.filterBanks(WARD_ID, "100234567890");
            assertThat(codesOf(result)).doesNotContain("020");
        }
    }
}
