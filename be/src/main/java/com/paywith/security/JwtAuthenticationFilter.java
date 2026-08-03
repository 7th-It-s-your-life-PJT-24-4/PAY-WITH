package com.paywith.security;

import java.io.IOException;
import java.util.Collections;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** JwtAuthenticationEntryPoint 가 읽어 AUTH_001/AUTH_002 응답 코드를 결정하는 request attribute. */
    public static final String AUTH_ERROR_ATTRIBUTE = "com.paywith.security.AUTH_ERROR_CODE";

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String token = resolveToken(request);

        if (token == null) {
            request.setAttribute(AUTH_ERROR_ATTRIBUTE, "AUTH_001");
            filterChain.doFilter(request, response);
            return;
        }

        JwtTokenProvider.TokenStatus status = jwtTokenProvider.resolveStatus(token);
        if (status == JwtTokenProvider.TokenStatus.VALID) {
            Long userId = jwtTokenProvider.getUserId(token);
            String phone = jwtTokenProvider.getPhone(token);
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userId,
                phone,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } else {
            request.setAttribute(
                AUTH_ERROR_ATTRIBUTE,
                status == JwtTokenProvider.TokenStatus.EXPIRED ? "AUTH_002" : "AUTH_001"
            );
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
            return authorization.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
