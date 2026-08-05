package com.paywith.config;

import javax.servlet.ServletContext;
import org.springframework.security.web.context.AbstractSecurityWebApplicationInitializer;
import org.springframework.web.filter.ForwardedHeaderFilter;

public class SecurityWebInitializer extends AbstractSecurityWebApplicationInitializer {

    /**
     * ForwardedHeaderFilter 는 반드시 springSecurityFilterChain 보다 먼저 돌아야 한다.
     *
     * <p>Spring 5.3 의 CorsUtils.isCorsRequest 는 Origin 헤더를 request 의 scheme/host/port 와
     * 비교해 동일 출처면 CORS 검사를 건너뛴다. 그런데 TLS 는 Nginx 가 끊으므로 필터를 거치기
     * 전의 request 는 http + 8080 으로 보인다. 그러면 https://paywith.site 에서 온 동일 출처
     * 요청조차 교차 출처로 판정되어, 허용 목록에 없는 origin 으로 403 이 된다.
     *
     * <p>WebAppInitializer.getServletFilters() 에 두면 등록 순서가 SecurityWebInitializer 와의
     * 실행 순서에 좌우된다(둘 다 order 가 없어 비결정적). 여기서 등록하면 보안 필터 체인보다
     * 먼저인 것이 보장된다.
     */
    @Override
    protected void beforeSpringSecurityFilterChain(ServletContext servletContext) {
        insertFilters(servletContext, new ForwardedHeaderFilter());
    }
}
