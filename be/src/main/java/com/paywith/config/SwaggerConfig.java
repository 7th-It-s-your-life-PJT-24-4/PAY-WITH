package com.paywith.config;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
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
            .apiInfo(apiInfo())
            // springfox 가 전 응답에 붙이는 기본 200/201/401/403/404 행은 실제 계약과 무관해 끈다.
            .useDefaultResponseMessages(false)
            .securitySchemes(List.of(apiKey()))
            .securityContexts(List.of(securityContext()))
            .select()
            .apis(RequestHandlerSelectors.basePackage("com.paywith"))
            .paths(PathSelectors.ant("/api/**"))
            .build();
    }

    /** 첫 화면에서 반복 질문 두 가지(응답 래퍼 구조, Authorize 입력 형식)가 해소되도록 적어 둔다. */
    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
            .title("PayWith API")
            .description("시니어 안심 전자지갑 API. 역할은 WARD(피보호자)/GUARD(보호자)로 나뉜다. "
                + "모든 응답은 {success, data, message(, code)} 래퍼로 감싸진다. "
                + "인증이 필요한 API는 우측 상단 Authorize 에 \"Bearer {accessToken}\" 형식으로 입력한다.\n\n"
                + "공통 규칙 (개별 API 설명에 반복된 내용의 근거)\n"
                + "1. 인증: /api/auth/**, POST /api/users, GET /api/merchants, POST /api/payments/execute, "
                + "GET /api/health 를 제외한 모든 API 는 Bearer 토큰이 필요하다. 토큰이 없거나 무효하면 "
                + "401 AUTH_001 \"인증이 필요합니다.\", 만료됐으면 401 AUTH_002 \"인증이 만료되었습니다.\". "
                + "역할(WARD/GUARD)·소유권 검사는 시큐리티가 아니라 각 API 가 수행하므로 403 코드는 API 별 설명을 따른다.\n"
                + "2. 검증 오류: 요청 본문의 필수값·형식 검증(@Valid)에 실패하면 400 REQUEST_001 이고, "
                + "message 는 \"<필드명>: <검증 메시지>\" 형식으로 첫 번째 오류 하나만 담는다.\n"
                + "3. 500: JSON 타입 불일치·파싱 실패, 숫자가 아닌 경로/쿼리 값, 필수 헤더 누락, 그 밖의 "
                + "처리되지 않은 예외는 모두 500 이며 code 없이 message \"서버 오류가 발생했습니다.\" 만 온다.\n"
                + "4. code 없는 오류: 일부 오류 응답은 code 키 자체가 생략된다(래퍼가 null 필드를 생략). "
                + "이 경우 message 로 구분한다.\n"
                + "5. 시각 형식: LocalDateTime 필드는 오프셋 없는 ISO-8601(예: 2026-07-16T15:30:00)이며, "
                + "서버 처리 시각을 그대로 담는 필드는 마이크로초가 붙을 수 있다(예: 2026-07-16T15:30:00.123456). "
                + "결제 API 의 시각 필드만 +09:00 오프셋 문자열이다.")
            .version("1.0")
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
