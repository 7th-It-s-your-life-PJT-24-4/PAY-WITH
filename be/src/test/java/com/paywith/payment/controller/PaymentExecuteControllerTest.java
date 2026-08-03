package com.paywith.payment.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.paywith.exception.BusinessException;
import com.paywith.exception.GlobalExceptionHandler;
import com.paywith.payment.dto.ExecuteRequest;
import com.paywith.payment.dto.ExecuteResponse;
import com.paywith.payment.service.PaymentExecuteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PaymentExecuteControllerTest {

    private static final String VALID_BODY =
        "{\"qrToken\": \"pay_qr_a8F2kL9xQ1mNzzzz\", \"merchantId\": 1, \"amount\": 4500}";

    private MockMvc mockMvc;

    @Mock
    private PaymentExecuteService paymentExecuteService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(new PaymentExecuteController(paymentExecuteService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    void execute_성공시_200과_명세_5필드_반환() throws Exception {
        given(paymentExecuteService.execute(ArgumentMatchers.any(ExecuteRequest.class)))
            .willReturn(new ExecuteResponse(
                1024L, "COMPLETED", 4500L, "GS25 강남역점", "2026-07-16T15:30:00+09:00"
            ));

        mockMvc.perform(post("/api/payments/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.transactionId").value(1024))
            .andExpect(jsonPath("$.data.status").value("COMPLETED"))
            .andExpect(jsonPath("$.data.amount").value(4500))
            .andExpect(jsonPath("$.data.merchantName").value("GS25 강남역점"))
            .andExpect(jsonPath("$.data.createdAt").value("2026-07-16T15:30:00+09:00"));
    }

    @Test
    void execute_qrToken_누락이면_400_서비스_미호출() throws Exception {
        mockMvc.perform(post("/api/payments/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"merchantId\": 1, \"amount\": 4500}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));

        verifyNoInteractions(paymentExecuteService);
    }

    @Test
    void execute_amount_0이하면_400_서비스_미호출() throws Exception {
        mockMvc.perform(post("/api/payments/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"qrToken\": \"pay_qr_a8F2kL9xQ1mNzzzz\", \"merchantId\": 1, \"amount\": 0}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));

        verifyNoInteractions(paymentExecuteService);
    }

    @Test
    void execute_무효토큰_BusinessException은_400과_명세_메시지로_변환() throws Exception {
        given(paymentExecuteService.execute(ArgumentMatchers.any(ExecuteRequest.class)))
            .willThrow(new BusinessException(
                HttpStatus.BAD_REQUEST, "유효하지 않은 QR입니다. 새 QR로 다시 시도해주세요."));

        mockMvc.perform(post("/api/payments/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("유효하지 않은 QR입니다. 새 QR로 다시 시도해주세요."));
    }

    @Test
    void execute_가맹점_미존재_BusinessException은_404로_변환() throws Exception {
        given(paymentExecuteService.execute(ArgumentMatchers.any(ExecuteRequest.class)))
            .willThrow(new BusinessException(HttpStatus.NOT_FOUND, "가맹점을 찾을 수 없습니다."));

        mockMvc.perform(post("/api/payments/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("가맹점을 찾을 수 없습니다."));
    }
}
