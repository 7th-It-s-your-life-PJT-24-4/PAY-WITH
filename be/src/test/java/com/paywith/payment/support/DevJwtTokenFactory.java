package com.paywith.payment.support;

import com.paywith.security.JwtTokenProvider;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;

/**
 * 로컬 개발용 JWT 발급 도구 (B6).
 *
 * 로그인 "발급 과정"만 우회한다 — 발급된 토큰은 실제 Authorization: Bearer 헤더로 전달해
 * 기존 JwtAuthenticationFilter 검증 경로를 그대로 태운다. src/test 전용이라 WAR에 포함되지 않는다.
 * 시크릿은 application-local.properties의 값(환경변수 오버라이드 지원)을 재사용하므로
 * 새 비밀키·고정 토큰을 커밋하지 않는다.
 *
 * 대상 시드 사용자: src/test/resources/db/dev-payment-seed.sql (WARD 9001 / GUARDIAN 9002)
 */
public final class DevJwtTokenFactory {

    public static final long WARD_USER_ID = 9001L;
    public static final long GUARDIAN_USER_ID = 9002L;

    private DevJwtTokenFactory() {
    }

    public static String wardAccessToken() {
        return jwtTokenProvider().createAccessToken(WARD_USER_ID, "dev-ward@paywith.local");
    }

    public static String guardianAccessToken() {
        return jwtTokenProvider().createAccessToken(GUARDIAN_USER_ID, "dev-guardian@paywith.local");
    }

    /** 로컬 설정값으로 구성한 실제 JwtTokenProvider (테스트 컨텍스트에서도 재사용) */
    public static JwtTokenProvider jwtTokenProvider() {
        Properties properties = loadLocalProperties();
        return new JwtTokenProvider(
            resolve(properties.getProperty("jwt.secret")),
            Long.parseLong(resolve(properties.getProperty("jwt.access-token-validity-ms"))),
            Long.parseLong(resolve(properties.getProperty("jwt.refresh-token-validity-ms")))
        );
    }

    /** curl 테스트용 토큰 출력: ./gradlew test 없이 IDE에서 main 실행 */
    public static void main(String[] args) {
        System.out.println("WARD(9001)     Bearer " + wardAccessToken());
        System.out.println("GUARDIAN(9002) Bearer " + guardianAccessToken());
    }

    private static Properties loadLocalProperties() {
        try (InputStream inputStream =
                 DevJwtTokenFactory.class.getClassLoader().getResourceAsStream("application-local.properties")) {
            if (inputStream == null) {
                throw new IllegalStateException("application-local.properties를 클래스패스에서 찾을 수 없습니다.");
            }
            Properties properties = new Properties();
            properties.load(inputStream);
            return properties;
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    /** "${ENV_NAME:default}" 형식의 값을 환경변수 우선으로 풀어준다 */
    private static String resolve(String value) {
        if (value == null || !value.startsWith("${") || !value.endsWith("}")) {
            return value;
        }
        String body = value.substring(2, value.length() - 1);
        int separatorIndex = body.indexOf(':');
        String envName = separatorIndex < 0 ? body : body.substring(0, separatorIndex);
        String defaultValue = separatorIndex < 0 ? null : body.substring(separatorIndex + 1);
        String envValue = System.getenv(envName);
        return envValue != null ? envValue : defaultValue;
    }
}
