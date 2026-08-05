package com.paywith.config;

import javax.servlet.Filter;
import org.springframework.web.filter.ForwardedHeaderFilter;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

public class WebAppInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {

    @Override
    protected Class<?>[] getRootConfigClasses() {
        return new Class<?>[] {AppConfig.class, MyBatisConfig.class, SecurityConfig.class, RedisConfig.class};
    }

    // SwaggerConfig 는 서블릿 컨텍스트에 둔다. springfox 가 /v2/api-docs 등을 핸들러로 등록하려면
    // DispatcherServlet 의 RequestMappingHandlerMapping 과 같은 컨텍스트에 있어야 한다.
    @Override
    protected Class<?>[] getServletConfigClasses() {
        return new Class<?>[] {WebConfig.class, SwaggerConfig.class};
    }

    @Override
    protected String[] getServletMappings() {
        return new String[] {"/"};
    }

    /**
     * TLS 는 Nginx 가 끊고 앱에는 평문 HTTP 로 들어온다. 이 필터가 없으면 request.getScheme()
     * 이 http 라, springfox 가 만든 스펙의 schemes 도 http 가 되어 HTTPS 로 열린 Swagger UI 의
     * Try it out 이 mixed content 로 차단된다. 앱이 만드는 절대 URL 전반에 영향을 준다.
     *
     * <p>X-Forwarded-* 를 신뢰하는 필터이므로, 그 헤더를 항상 덮어써 주는 리버스 프록시 뒤에서만
     * 써야 한다. Nginx 설정의 proxy_set_header 가 그 역할을 한다.
     */
    @Override
    protected Filter[] getServletFilters() {
        return new Filter[] {new ForwardedHeaderFilter()};
    }
}
