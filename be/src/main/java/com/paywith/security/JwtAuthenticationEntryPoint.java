package com.paywith.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paywith.common.ApiResponse;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * 인증 실패를 ApiResponse(code/message) JSON으로 응답한다.
 * JwtAuthenticationFilter가 남긴 AUTH_ERROR_ATTRIBUTE로 무토큰/무효(AUTH_001)와 만료(AUTH_002)를 구분한다.
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public JwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException authException
    ) throws IOException {
        String code = (String) request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR_ATTRIBUTE);
        if (code == null) {
            code = "AUTH_001";
        }
        String message = "AUTH_002".equals(code) ? "인증이 만료되었습니다." : "인증이 필요합니다.";

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(code, message));
    }
}