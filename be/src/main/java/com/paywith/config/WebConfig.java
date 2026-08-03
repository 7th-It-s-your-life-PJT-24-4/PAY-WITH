package com.paywith.config;

import java.util.List;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebMvc
@ComponentScan(
        basePackages = "com.paywith",
        includeFilters = @ComponentScan.Filter(
                type = FilterType.ANNOTATION,
                classes = { Controller.class, RestController.class, ControllerAdvice.class, RestControllerAdvice.class }
        ),
        useDefaultFilters = false
)
public class WebConfig implements WebMvcConfigurer {

    private final MappingJackson2HttpMessageConverter jacksonConverter;

    public WebConfig(MappingJackson2HttpMessageConverter jacksonConverter) {
        this.jacksonConverter = jacksonConverter;
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(jacksonConverter);
    }

    // CORS 는 SecurityConfig.corsConfigurationSource() 한 곳에서만 정의한다. 보안 필터가
    // MVC 보다 먼저 도는 탓에 여기서 addCorsMappings 를 겹쳐 두면 적용되지 않는다.

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // springfox 3 은 UI 를 webjar 안에 담아 /swagger-ui/index.html 로 연다.
        // 2.x 의 /swagger-ui.html 은 더 이상 존재하지 않는다.
        registry.addResourceHandler("/swagger-ui/**")
            .addResourceLocations("classpath:/META-INF/resources/webjars/springfox-swagger-ui/");
        registry.addResourceHandler("/webjars/**")
            .addResourceLocations("classpath:/META-INF/resources/webjars/");
    }
}
