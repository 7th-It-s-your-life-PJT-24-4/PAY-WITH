package com.paywith.common;

/**
 * 화면 표시용 하이픈을 제거해 전화번호를 DB와 Redis 키에서 같은 형식으로 사용한다.
 */
public final class PhoneNumberNormalizer {

    private PhoneNumberNormalizer() {
    }

    public static String normalize(String phone) {
        if (phone == null) {
            return null;
        }
        return phone.replaceAll("\\D", "");
    }
}
