package com.paywith.common;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 외부(Sentry)로 나가는 문자열에서 금융·개인정보로 보이는 토막을 가린다.
 *
 * <p>로그와 예외 메시지는 우리가 형식을 정하지만, 값까지 통제하지는 못한다. 특히 MyBatis 가
 * 던지는 예외 메시지에는 바인딩된 파라미터가 그대로 실려 나온다 — 계좌번호 유니크 제약이나
 * fcm_token 중복이 걸리면 그 값이 예외 메시지에 남는다. Sentry 는 헤더의 Authorization·Cookie 는
 * 알아서 걸러 주지만(sendDefaultPii=false), 메시지 본문은 손대지 않으므로 여기서 한 겹 더 막는다.
 *
 * <p>스택트레이스의 클래스·메서드·줄번호는 건드리지 않는다. 원인 추적에 필요하고, 값이 실리는
 * 자리가 아니다.
 */
public final class SensitiveDataMasker {

    public static final String MASK = "[Filtered]";

    /** JWT — 우리 액세스·리프레시 토큰 형식이다. */
    private static final Pattern JWT =
        Pattern.compile("eyJ[A-Za-z0-9_-]{8,}\\.[A-Za-z0-9_-]{8,}\\.[A-Za-z0-9_-]{8,}");

    /**
     * FCM 등록 토큰처럼 의미 없이 긴 영숫자 덩어리. 40자는 UUID(36자)를 지나치도록 잡은 값이다 —
     * 거래 추적에 쓰는 UUID 까지 가리면 로그를 읽을 수 없다.
     */
    private static final Pattern OPAQUE_TOKEN = Pattern.compile("[A-Za-z0-9_-]{40,}");

    /**
     * 숫자와 하이픈으로만 이어진 덩어리. 자릿수 판정은 {@link #masksAsNumber} 가 한다.
     *
     * <p>앞뒤로 영숫자·하이픈이 붙어 있으면 잡지 않는다. UUID(550e8400-e29b-41d4-a716-446655440000)
     * 의 뒤쪽은 하이픈 낀 15자리 숫자처럼 보여서, 이 조건이 없으면 UUID 가 반토막 난다.
     * 대신 acct110234567890 처럼 식별자에 계좌번호를 이어 붙인 문자열은 놓친다 — 우리 로그에
     * 그런 형식이 없고, 넓히면 이번엔 정상 식별자를 가리게 된다.
     */
    private static final Pattern DIGIT_GROUP =
        Pattern.compile("(?<![0-9A-Za-z-])[0-9][0-9-]*[0-9](?![0-9A-Za-z-])");

    /** 계좌번호(10~14) · 휴대폰번호(11) · 카드번호(16)를 덮는 구간. */
    private static final int MIN_MASKED_DIGITS = 10;
    private static final int MAX_MASKED_DIGITS = 16;

    private SensitiveDataMasker() {
    }

    public static String mask(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String masked = JWT.matcher(text).replaceAll(Matcher.quoteReplacement(MASK));
        masked = OPAQUE_TOKEN.matcher(masked).replaceAll(Matcher.quoteReplacement(MASK));
        return maskNumbers(masked);
    }

    private static String maskNumbers(String text) {
        Matcher matcher = DIGIT_GROUP.matcher(text);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String group = matcher.group();
            matcher.appendReplacement(result,
                Matcher.quoteReplacement(masksAsNumber(group) ? MASK : group));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * 하이픈을 뺀 자릿수로 판정한다. 010-1234-5678 과 01012345678 을 같게 보기 위해서다.
     *
     * <p>10~16자리 숫자면 무엇이든 가린다. epoch 밀리초(13자리)처럼 무해한 값도 함께 가려지는데,
     * 이 앱의 식별자는 전부 짧은 auto increment 라 실제로 잃는 정보가 거의 없다. 반대로 자릿수를
     * 더 좁히면 은행마다 다른 계좌번호 길이를 놓친다.
     */
    private static boolean masksAsNumber(String group) {
        int digits = 0;
        for (int i = 0; i < group.length(); i++) {
            if (Character.isDigit(group.charAt(i))) {
                digits++;
            }
        }
        return digits >= MIN_MASKED_DIGITS && digits <= MAX_MASKED_DIGITS;
    }
}
