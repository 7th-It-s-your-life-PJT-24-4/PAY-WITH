package com.paywith.payment.security;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.paywith.config.SecurityConfig;
import com.paywith.exception.GlobalExceptionHandler;
import com.paywith.payment.controller.PaymentExecuteController;
import com.paywith.payment.dto.ExecuteRequest;
import com.paywith.payment.dto.ExecuteResponse;
import com.paywith.payment.service.PaymentExecuteService;
import com.paywith.payment.support.DevJwtTokenFactory;
import com.paywith.security.JwtAuthenticationEntryPoint;
import com.paywith.security.JwtAuthenticationFilter;
import com.paywith.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * permitAll 검증: 가맹점 스캐너는 로그인 계정이 없는 제3 액터이므로
 * POST /api/payments/execute는 무토큰으로도 실제 SecurityConfig 필터 체인을 통과해야 한다.
 */
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = {PaymentExecuteSecurityTest.TestConfig.class, SecurityConfig.class})
class PaymentExecuteSecurityTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private PaymentExecuteService paymentExecuteService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply(springSecurity())
            .build();
    }

    @Test
    void 무토큰_요청도_필터를_통과해_200으로_응답한다() throws Exception {
        given(paymentExecuteService.execute(ArgumentMatchers.any(ExecuteRequest.class)))
            .willReturn(new ExecuteResponse(
                1024L, "COMPLETED", 4500L, "GS25 강남역점", "2026-07-16T15:30:00+09:00"
            ));

        mockMvc.perform(post("/api/payments/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"qrToken\": \"pay_qr_a8F2kL9xQ1mNzzzz\", \"merchantId\": 1, \"amount\": 4500}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.transactionId").value(1024));
    }

    @Configuration
    @EnableWebMvc
    static class TestConfig {

        @Bean
        public PaymentExecuteService paymentExecuteService() {
            return Mockito.mock(PaymentExecuteService.class);
        }

        @Bean
        public PaymentExecuteController paymentExecuteController(PaymentExecuteService paymentExecuteService) {
            return new PaymentExecuteController(paymentExecuteService);
        }

        @Bean
        public GlobalExceptionHandler globalExceptionHandler() {
            return new GlobalExceptionHandler();
        }

        @Bean
        public JwtTokenProvider jwtTokenProvider() {
            // 로컬 설정값 기반 실제 JwtTokenProvider — 테스트용 시크릿을 코드에 새로 두지 않는다
            return DevJwtTokenFactory.jwtTokenProvider();
        }

        @Bean
        public JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
            return new JwtAuthenticationFilter(jwtTokenProvider);
        }

        @Bean
        public ObjectMapper objectMapper() {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            return mapper;
        }

        @Bean
        public JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
            return new JwtAuthenticationEntryPoint(objectMapper);
        }
    }
}
