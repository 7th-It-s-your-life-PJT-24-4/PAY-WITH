package com.paywith.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.sentry.spring.SentryTaskDecorator;
import org.springframework.context.annotation.*;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@Configuration
@EnableScheduling
@EnableAsync
@ComponentScan(basePackages = "com.paywith",
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ANNOTATION,
                classes = { Controller.class, RestController.class, ControllerAdvice.class, RestControllerAdvice.class }
        ))
// encoding 미지정 시 Java 표준대로 ISO-8859-1로 읽혀 한글 값(FDS 메모 키워드 등)이 깨진다.
@PropertySource(value = "classpath:application-${spring.profiles.active:local}.properties", encoding = "UTF-8")
public class AppConfig {

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        // 끄지 않으면 LocalDateTime 이 [2026,7,30,11,47,12] 형태의 숫자 배열로 직렬화된다.
        // FE 스키마는 ISO-8601 문자열("2026-07-30T11:47:12")을 기대한다.
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    @Bean
    public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter(ObjectMapper objectMapper) {
        return new MappingJackson2HttpMessageConverter(objectMapper);
    }

    /**
     * 푸시 발송 전용 스레드풀. FCM 호출은 외부 네트워크라 결제·송금 응답 시간에 얹히면 안 되고,
     * 요청 스레드를 붙잡으면 부하가 몰릴 때 톰캣 스레드가 먼저 마른다.
     *
     * <p>큐가 차면 버리지 않고 호출 스레드에서 직접 보낸다(CallerRunsPolicy). 알림을 조용히
     * 잃는 것보다 그 요청이 느려지는 편이 낫다고 봤다.
     *
     * <p>재배포 때 진행 중인 발송은 기다렸다 끝낸다 — 커밋은 됐는데 알림만 사라지는 걸 줄인다.
     */
    @Bean
    public ThreadPoolTaskExecutor pushTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // Sentry 는 요청 문맥을 스레드에 붙여 관리한다. 데코레이터가 없으면 여기서 난 오류가
        // 어느 요청에서 비롯됐는지(URL·사용자)를 잃고 스택트레이스만 남는다.
        executor.setTaskDecorator(new SentryTaskDecorator());
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("push-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        return executor;
    }
}
