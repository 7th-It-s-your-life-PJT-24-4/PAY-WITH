package com.paywith.config;

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
}
