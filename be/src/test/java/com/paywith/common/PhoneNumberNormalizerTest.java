package com.paywith.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("전화번호 정규화")
class PhoneNumberNormalizerTest {

    @Test
    void removesNonDigitsFromDisplayFormat() {
        assertThat(PhoneNumberNormalizer.normalize("010-1234-5678")).isEqualTo("01012345678");
    }

    @Test
    void keepsNullAsNull() {
        assertThat(PhoneNumberNormalizer.normalize(null)).isNull();
    }
}
