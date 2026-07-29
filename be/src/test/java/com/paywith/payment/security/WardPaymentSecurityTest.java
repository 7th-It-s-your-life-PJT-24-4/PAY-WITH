package com.paywith.payment.security;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.paywith.config.SecurityConfig;
import com.paywith.exception.BusinessException;
import com.paywith.exception.GlobalExceptionHandler;
import com.paywith.payment.controller.WardPaymentController;
import com.paywith.payment.dto.QrCreateResponse;
import com.paywith.payment.service.PaymentService;
import com.paywith.payment.support.DevJwtTokenFactory;
import com.paywith.security.JwtAuthenticationFilter;
import com.paywith.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * 인증 3종 테스트 (B6·B9): 무토큰 거부 / GUARDIAN 토큰 403 / 정상 WARD 성공.
 * DevJwtTokenFactory로 발급한 토큰을 실제 JwtAuthenticationFilter + SecurityConfig
 * 필터 체인에 통과시켜 "발급만 우회하고 검증은 실제 경로" 원칙(B6)을 지킨다.
 */
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = {WardPaymentSecurityTest.TestConfig.class, SecurityConfig.class})
class WardPaymentSecurityTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private PaymentService paymentService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply(springSecurity())
            .build();
    }

    @Test
    void 무토큰_요청은_거부된다() throws Exception {
        // 명세는 401(AUTH_001)이나, 현재 SecurityConfig에 AuthenticationEntryPoint가 없어
        // 기본 Http403ForbiddenEntryPoint가 403을 반환한다. 401 통일은 인증 파트 확정 대상.
        mockMvc.perform(post("/api/ward/payments"))
            .andExpect(status().isForbidden());
    }

    @Test
    void GUARDIAN_토큰이면_403() throws Exception {
        given(paymentService.createQr(DevJwtTokenFactory.GUARDIAN_USER_ID, "123456"))
            .willThrow(new BusinessException(HttpStatus.FORBIDDEN, "피보호자만 접근할 수 있습니다."));

        mockMvc.perform(post("/api/ward/payments")
                .header("Authorization", "Bearer " + DevJwtTokenFactory.guardianAccessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"pin\": \"123456\"}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("피보호자만 접근할 수 있습니다."));
    }

    @Test
    void 정상_WARD_토큰이면_201() throws Exception {
        given(paymentService.createQr(DevJwtTokenFactory.WARD_USER_ID, "123456"))
            .willReturn(new QrCreateResponse(42L, "pay_qr_a8F2kL9xQ1mNzzzz", 130000L,
                "2026-07-16T15:31:00+09:00", 60));

        mockMvc.perform(post("/api/ward/payments")
                .header("Authorization", "Bearer " + DevJwtTokenFactory.wardAccessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"pin\": \"123456\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.paymentToken").value("pay_qr_a8F2kL9xQ1mNzzzz"));
    }

    @Configuration
    @EnableWebMvc
    static class TestConfig {

        @Bean
        public PaymentService paymentService() {
            return Mockito.mock(PaymentService.class);
        }

        @Bean
        public WardPaymentController wardPaymentController(PaymentService paymentService) {
            return new WardPaymentController(paymentService);
        }

        @Bean
        public GlobalExceptionHandler globalExceptionHandler() {
            return new GlobalExceptionHandler();
        }

        @Bean
        public JwtTokenProvider jwtTokenProvider() {
            // 로컬 설정값 기반 실제 JwtTokenProvider — 테스트용 시크릿을 코드에 새로 두지 않는다(B6)
            return DevJwtTokenFactory.jwtTokenProvider();
        }

        @Bean
        public JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
            return new JwtAuthenticationFilter(jwtTokenProvider);
        }
    }
}
