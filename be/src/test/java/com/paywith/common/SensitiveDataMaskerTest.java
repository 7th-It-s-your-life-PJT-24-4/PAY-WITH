package com.paywith.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Sentry 전송 문자열 마스킹")
class SensitiveDataMaskerTest {

    @Test
    void mask_hidesAccountNumber() {
        String masked = SensitiveDataMasker.mask("수취인 계좌 110234567890 조회 실패");

        assertThat(masked).doesNotContain("110234567890");
        assertThat(masked).isEqualTo("수취인 계좌 [Filtered] 조회 실패");
    }

    @Test
    void mask_hidesPhoneNumberWithAndWithoutHyphens() {
        assertThat(SensitiveDataMasker.mask("010-1234-5678")).isEqualTo("[Filtered]");
        assertThat(SensitiveDataMasker.mask("01012345678")).isEqualTo("[Filtered]");
    }

    @Test
    void mask_hidesJwt() {
        String jwt = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.dBjftJeZ4CVPmB92K27uhbUJU1p1r";

        assertThat(SensitiveDataMasker.mask("토큰 검증 실패: " + jwt))
            .isEqualTo("토큰 검증 실패: [Filtered]");
    }

    @Test
    void mask_hidesOpaqueTokenLikeFcmRegistrationToken() {
        // MyBatis 가 users.fcm_token 유니크 제약 위반을 던지면 이 값이 예외 메시지에 실려 온다.
        String fcmToken = "fGhI7kLmNoPqRsTuVwXyZ0123456789abcdefGHIJKLMNOPQRSTUVWXYZ_-abcdef";

        assertThat(SensitiveDataMasker.mask("Duplicate entry '" + fcmToken + "' for key"))
            .isEqualTo("Duplicate entry '[Filtered]' for key");
    }

    @Test
    void mask_keepsShortIdentifiers() {
        // 로그의 transactionId·walletId 까지 가리면 원인 추적이 불가능해진다.
        String message = "송금 확정 실패. transactionId=1024, walletId=77";

        assertThat(SensitiveDataMasker.mask(message)).isEqualTo(message);
    }

    @Test
    void mask_keepsUuid() {
        // 36자라 OPAQUE_TOKEN(40자) 경계 아래에 있어야 한다.
        String message = "요청 550e8400-e29b-41d4-a716-446655440000 처리 중";

        assertThat(SensitiveDataMasker.mask(message)).isEqualTo(message);
    }

    @Test
    void mask_handlesMultipleOccurrencesInOneMessage() {
        String masked = SensitiveDataMasker.mask("from 110234567890 to 3333012345678");

        assertThat(masked).isEqualTo("from [Filtered] to [Filtered]");
    }

    @Test
    void mask_returnsInputWhenNullOrEmpty() {
        assertThat(SensitiveDataMasker.mask(null)).isNull();
        assertThat(SensitiveDataMasker.mask("")).isEmpty();
    }
}
