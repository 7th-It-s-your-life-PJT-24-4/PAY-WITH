package com.paywith.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.paywith.common.SentryEventMasker;
import io.sentry.Sentry;
import io.sentry.SentryOptions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

/**
 * Sentry 배선이 실제로 조립되는지 본다. 이 조립이 틀리면 예외가 나는 게 아니라 "이슈가 안 올라온다"
 * 또는 "가려야 할 값이 그대로 나간다"로 나타나서, 한참 뒤에나 알게 된다.
 */
@DisplayName("Sentry 설정 조립")
class SentryConfigTest {

    @BeforeAll
    static void requireNoDsnInEnvironment() {
        String dsn = System.getenv("SENTRY_DSN");
        Assumptions.assumeTrue(dsn == null || dsn.isEmpty(),
            "SENTRY_DSN 이 설정된 환경 — 비활성 여부를 검증할 수 없어 스킵한다");
    }

    @Test
    void sentryOptions_disableSdkWhenDsnIsAbsent() {
        try (AnnotationConfigApplicationContext context = context()) {
            // ${SENTRY_DSN:} 이 빈 문자열로 풀려야 한다. 치환에 실패하면 리터럴이 DSN 으로 들어가
            // 초기화가 깨진다.
            assertThat(context.getBean(SentryOptions.class).getDsn()).isEmpty();
            assertThat(Sentry.isEnabled()).isFalse();
        }
    }

    @Test
    void sentryOptions_keepRequestPayloadAndPiiOut() {
        try (AnnotationConfigApplicationContext context = context()) {
            SentryOptions options = context.getBean(SentryOptions.class);

            assertThat(options.isSendDefaultPii()).isFalse();
            assertThat(options.getMaxRequestBodySize()).isEqualTo(SentryOptions.RequestSize.NONE);
        }
    }

    @Test
    void sentryOptions_useEventMaskerAsBeforeSend() {
        try (AnnotationConfigApplicationContext context = context()) {
            assertThat(context.getBean(SentryOptions.class).getBeforeSend())
                .isInstanceOf(SentryEventMasker.class);
        }
    }

    private AnnotationConfigApplicationContext context() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        // AppConfig 가 등록하는 것과 같은 치환기. 이게 없으면 애노테이션의 ${...} 가 그대로 남는다.
        context.registerBean(PropertySourcesPlaceholderConfigurer.class,
            PropertySourcesPlaceholderConfigurer::new);
        context.register(SentryConfig.class);
        context.refresh();
        return context;
    }
}
