package com.paywith.config;

import com.paywith.security.JwtAuthenticationEntryPoint;
import com.paywith.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final List<String> allowedOrigins;

    /**
     * 콤마 문자열을 그대로 받아 직접 쪼갠다. List 로 바로 주입받으면 ConversionService 가
     * 등록된 컨텍스트에서만 동작해, 슬라이스 테스트처럼 그렇지 않은 곳에서는 콤마째 하나의
     * origin 이 되어 조용히 전부 차단된다.
     */
    public SecurityConfig(
        JwtAuthenticationFilter jwtAuthenticationFilter,
        JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
        @Value("${cors.allowed-origins}") String allowedOrigins
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
            .map(String::trim)
            .filter(origin -> !origin.isEmpty())
            .collect(Collectors.toList());
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .cors()
            .and()
            .csrf().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .exceptionHandling().authenticationEntryPoint(jwtAuthenticationEntryPoint)
            .and()
            .authorizeRequests()
            .antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
            .antMatchers("/api/auth/**", "/swagger-ui/**", "/swagger-resources/**", "/v2/api-docs", "/webjars/**").permitAll()
            .antMatchers(HttpMethod.POST, "/api/users").permitAll()
            // 가맹점 스캐너는 로그인 계정이 없는 제3 액터라 목록 조회를 개방한다
            .antMatchers(HttpMethod.GET, "/api/merchants").permitAll()
            // 결제 실행도 스캐너가 호출한다 — 60초 1회용 QR 토큰이 사실상 자격증명 역할을 한다
            .antMatchers(HttpMethod.POST, "/api/payments/execute").permitAll()
            .antMatchers(HttpMethod.GET, "/api/users/**").authenticated()
            .anyRequest().authenticated()
            .and()
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * CORS 설정은 이 빈 하나로만 관리한다. {@code http.cors()} 가 이 빈을 쓰고, 보안 필터가
     * MVC 보다 먼저 돌기 때문에 {@code WebMvcConfigurer.addCorsMappings} 를 따로 두면 그쪽이
     * 무시되면서 설정만 보고는 원인을 찾기 어려워진다.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // 허용 origin 은 프로파일별 설정값으로 둔다. 로컬은 Vite 개발 서버(localhost 와
        // 127.0.0.1 둘 다 열 수 있어 양쪽), 운영은 배포 도메인이다. 와일드카드는 credential
        // 허용과 함께 쓸 수 없어 명시적으로 나열한다.
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        // Idempotency-Key 는 송금(POST /api/ward/transfers)이 요구하는 커스텀 헤더다. 커스텀
        // 헤더가 붙으면 브라우저가 preflight 를 보내므로, 여기에 없으면 요청이 서버에 닿지도
        // 못하고 차단된다.
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Idempotency-Key"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
