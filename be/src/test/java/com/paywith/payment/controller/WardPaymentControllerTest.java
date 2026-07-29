package com.paywith.payment.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.paywith.exception.BusinessException;
import com.paywith.exception.GlobalExceptionHandler;
import com.paywith.payment.dto.PaymentCancelResponse;
import com.paywith.payment.dto.PaymentStatusResponse;
import com.paywith.payment.dto.QrCreateResponse;
import com.paywith.payment.service.PaymentService;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class WardPaymentControllerTest {

    private static final long WARD_ID = 9001L;

    private MockMvc mockMvc;

    @Mock
    private PaymentService paymentService;

    private final Authentication wardAuthentication = new UsernamePasswordAuthenticationToken(
        WARD_ID,
        null,
        Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
    );

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(new WardPaymentController(paymentService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    void createQr_성공시_201과_ApiResponse_봉투로_응답() throws Exception {
        given(paymentService.createQr(WARD_ID)).willReturn(new QrCreateResponse(
            42L, "pay_qr_a8F2kL9xQ1mNzzzz", 130000L, "2026-07-16T15:31:00+09:00", 60
        ));

        mockMvc.perform(post("/api/ward/payments").principal(wardAuthentication))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.paymentId").value(42))
            .andExpect(jsonPath("$.data.paymentToken").value("pay_qr_a8F2kL9xQ1mNzzzz"))
            .andExpect(jsonPath("$.data.availableBalance").value(130000))
            .andExpect(jsonPath("$.data.expiresAt").value("2026-07-16T15:31:00+09:00"))
            .andExpect(jsonPath("$.data.expiresInSeconds").value(60));
    }

    @Test
    void getStatus_숫자가_아닌_id는_400_PAYMENT_001() throws Exception {
        mockMvc.perform(get("/api/ward/payments/abc").principal(wardAuthentication))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("결제 요청 번호가 올바르지 않습니다."));

        verifyNoInteractions(paymentService);
    }

    @Test
    void getStatus_성공시_명세_응답_필드_반환() throws Exception {
        given(paymentService.getStatus(WARD_ID, 42L)).willReturn(new PaymentStatusResponse(
            42L, null, "PENDING", null, null, null, null, null, null, "2026-07-16T14:46:00+09:00"
        ));

        mockMvc.perform(get("/api/ward/payments/42").principal(wardAuthentication))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.paymentId").value(42))
            .andExpect(jsonPath("$.data.status").value("PENDING"))
            .andExpect(jsonPath("$.data.expiresAt").value("2026-07-16T14:46:00+09:00"));
    }

    @Test
    void getStatus_서비스의_BusinessException은_해당_상태코드로_변환() throws Exception {
        given(paymentService.getStatus(WARD_ID, 42L))
            .willThrow(new BusinessException(HttpStatus.NOT_FOUND, "결제 요청을 찾을 수 없습니다."));

        mockMvc.perform(get("/api/ward/payments/42").principal(wardAuthentication))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("결제 요청을 찾을 수 없습니다."));
    }

    @Test
    void cancel_성공시_200과_취소_응답() throws Exception {
        given(paymentService.cancel(WARD_ID, 42L)).willReturn(new PaymentCancelResponse(
            42L, "CANCELED", "2026-07-16T14:45:30+09:00"
        ));

        mockMvc.perform(post("/api/ward/payments/42/cancel").principal(wardAuthentication))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.paymentId").value(42))
            .andExpect(jsonPath("$.data.status").value("CANCELED"))
            .andExpect(jsonPath("$.data.canceledAt").value("2026-07-16T14:45:30+09:00"));
    }
}
