package com.paywith.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.*;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@Configuration
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
}
