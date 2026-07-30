package com.paywith.config;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiKey;
import springfox.documentation.service.AuthorizationScope;
import springfox.documentation.service.SecurityReference;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
public class SwaggerConfig {

    /**
     * Authorize 창에 넣은 값이 그대로 실릴 헤더명. JwtAuthenticationFilter 가 읽는 헤더와 같다.
     * 스킴 이름은 securitySchemes 와 securityReferences 양쪽이 일치해야 자물쇠가 연결된다.
     */
    private static final String SCHEME_NAME = "JWT (Bearer 접두어 포함)";
    private static final String AUTHORIZATION_HEADER = "Authorization";

    @Bean
    public Docket api() {
        // 컨트롤러가 도메인별 패키지(com.paywith.<domain>.controller)에 흩어져 있어 루트로 잡는다.
        return new Docket(DocumentationType.SWAGGER_2)
            .securitySchemes(List.of(apiKey()))
            .securityContexts(List.of(securityContext()))
            .select()
            .apis(RequestHandlerSelectors.basePackage("com.paywith"))
            .paths(PathSelectors.ant("/api/**"))
            .build();
    }

    /**
     * Swagger 2 스펙의 securityDefinitions 는 basic/apiKey/oauth2 만 지원한다. 접두어를 UI 가
     * 붙여주는 http+bearer 스킴은 OpenAPI 3 기능이라, 입력값을 헤더에 그대로 싣는 apiKey 를 쓴다.
     *
     * <p>그래서 Authorize 창에는 토큰만이 아니라 "Bearer eyJ..." 형태로 입력해야 한다
     * (JwtAuthenticationFilter 가 접두어를 요구한다). 스킴 이름에 그 점을 적어 둔다.
     */
    private ApiKey apiKey() {
        return new ApiKey(SCHEME_NAME, AUTHORIZATION_HEADER, "header");
    }

    /** /api/auth/** 는 SecurityConfig 에서 permitAll 이라 자물쇠를 붙이지 않는다. */
    private SecurityContext securityContext() {
        AuthorizationScope[] scopes = {new AuthorizationScope("global", "accessEverything")};
        return SecurityContext.builder()
            .securityReferences(List.of(new SecurityReference(SCHEME_NAME, scopes)))
            .operationSelector(context -> !context.requestMappingPattern().startsWith("/api/auth"))
            .build();
    }
}
