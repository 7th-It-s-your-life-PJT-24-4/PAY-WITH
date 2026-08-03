package com.paywith.merchant.security;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.paywith.config.SecurityConfig;
import com.paywith.exception.GlobalExceptionHandler;
import com.paywith.merchant.controller.MerchantController;
import com.paywith.merchant.dto.MerchantListResponse;
import com.paywith.merchant.service.MerchantService;
import com.paywith.payment.support.DevJwtTokenFactory;
import com.paywith.security.JwtAuthenticationEntryPoint;
import com.paywith.security.JwtAuthenticationFilter;
import com.paywith.security.JwtTokenProvider;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * B7(permitAll) 검증: 가맹점 스캐너는 로그인 계정이 없는 제3 액터이므로
 * GET /api/merchants는 무토큰으로도 실제 SecurityConfig 필터 체인을 통과해야 한다.
 */
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = {MerchantSecurityTest.TestConfig.class, SecurityConfig.class})
class MerchantSecurityTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private MerchantService merchantService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply(springSecurity())
            .build();
    }

    @Test
    void 무토큰_요청도_200으로_목록을_반환한다() throws Exception {
        given(merchantService.findAllPayable()).willReturn(new MerchantListResponse(List.of()));

        mockMvc.perform(get("/api/merchants"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Configuration
    @EnableWebMvc
    static class TestConfig {

        @Bean
        public MerchantService merchantService() {
            return Mockito.mock(MerchantService.class);
        }

        @Bean
        public MerchantController merchantController(MerchantService merchantService) {
            return new MerchantController(merchantService);
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

        @Bean
        public ObjectMapper objectMapper() {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            return mapper;
        }

        // #62가 SecurityConfig 생성자에 추가한 의존성 — WardPaymentSecurityTest와 동일 구성
        @Bean
        public JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
            return new JwtAuthenticationEntryPoint(objectMapper);
        }
    }
}
