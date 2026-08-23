package com.paywith.config;

import com.paywith.common.SentryEventMasker;
import io.sentry.SentryOptions;
import io.sentry.spring.EnableSentry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Sentry 에러 모니터링.
 *
 * <p>DSN 이 비면 SDK 가 스스로 비활성화된다. 로컬·테스트는 환경변수를 두지 않으므로 아무것도
 * 전송하지 않는다. 값은 배포 서버의 deploy/.env 한 곳에만 둔다(DB·JWT 와 같은 방식).
 *
 * <p>environment 와 release 는 여기서 배선하지 않는다. SDK 가 외부 설정을 읽도록 초기화되어
 * (enableExternalConfiguration) SENTRY_ENVIRONMENT · SENTRY_RELEASE 환경변수를 직접 집어간다.
 * 프로퍼티로 한 번 더 옮겨 담으면 같은 값이 두 곳에 존재하게 된다.
 *
 * <p>예외 수집 경로는 하나다. {@code @EnableSentry} 가 등록하는 SentryExceptionResolver 는
 * order 가 1 이라 @EnableWebMvc 의 resolver 조합(order 0)보다 뒤에 서는데,
 * GlobalExceptionHandler 가 Exception 까지 전부 처리하므로 실제로는 도달하지 않는다. 즉 MVC
 * 예외는 GlobalExceptionHandler 의 log.error → SentryAppender 로만 올라간다.
 * exceptionResolverOrder 를 앞으로 당기면 두 경로가 같은 예외를 각각 보내 이슈가 중복된다.
 *
 * <p>요청 문맥(URL·헤더)은 sentry-spring 이 ServletContainerInitializer 로 자동 등록하는
 * SentrySpringFilter 가 붙인다. 우리가 필터를 등록할 필요는 없다.
 *
 * <p>트레이싱은 켜지 않았다. SentryTracingFilter 를 등록하는 @EnableSentryTracing 이 없으면
 * tracesSampleRate 를 줘도 트랜잭션이 생성되지 않는다 — 지금은 에러 수집만 한다.
 */
@Configuration
// sendDefaultPii=false 는 기본값이지만 명시한다. 켜는 순간 Authorization·Cookie 헤더와 IP 가
// 이벤트에 실린다. maxRequestBodySize=NONE 은 요청 본문(계좌번호·금액)을 수집하지 않는다는 뜻이다.
@EnableSentry(dsn = "${SENTRY_DSN:}", sendDefaultPii = false,
    maxRequestBodySize = SentryOptions.RequestSize.NONE)
public class SentryConfig {

    /**
     * SentryInitBeanPostProcessor 가 이 타입의 빈을 찾아 options.beforeSend 로 걸어 준다.
     */
    @Bean
    public SentryOptions.BeforeSendCallback sentryEventMasker() {
        return new SentryEventMasker();
    }
}
