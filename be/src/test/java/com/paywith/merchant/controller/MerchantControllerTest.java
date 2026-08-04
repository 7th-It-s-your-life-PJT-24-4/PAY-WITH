package com.paywith.merchant.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.paywith.merchant.domain.Merchant;
import com.paywith.merchant.dto.MerchantListResponse;
import com.paywith.merchant.service.MerchantService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class MerchantControllerTest {

    private MockMvc mockMvc;

    @Mock
    private MerchantService merchantService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(new MerchantController(merchantService)).build();
    }

    @Test
    void 목록_응답은_명세_4필드이고_좌표는_노출하지_않는다() throws Exception {
        given(merchantService.findAllPayable()).willReturn(listResponse());

        mockMvc.perform(get("/api/merchants"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.merchants[0].merchantId").value(1))
            .andExpect(jsonPath("$.data.merchants[0].name").value("행복마트 종로점"))
            .andExpect(jsonPath("$.data.merchants[0].categoryCode").value("MART"))
            .andExpect(jsonPath("$.data.merchants[0].region").value("서울 종로구"))
            // 좌표는 서버 관리 값 — 응답 계약에 포함되면 안 된다
            .andExpect(jsonPath("$.data.merchants[0].latitude").doesNotExist())
            .andExpect(jsonPath("$.data.merchants[0].longitude").doesNotExist());
    }

    @Test
    void 결제_가능_가맹점이_없으면_빈_배열을_반환한다() throws Exception {
        given(merchantService.findAllPayable()).willReturn(new MerchantListResponse(List.of()));

        mockMvc.perform(get("/api/merchants"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.merchants").isEmpty());
    }

    private MerchantListResponse listResponse() {
        Merchant merchant = new Merchant();
        merchant.setMerchantId(1L);
        merchant.setName("행복마트 종로점");
        merchant.setCategoryCode("MART");
        merchant.setRegion("서울 종로구");
        merchant.setLatitude(new BigDecimal("37.5729000"));
        merchant.setLongitude(new BigDecimal("126.9793000"));
        return new MerchantListResponse(List.of(merchant));
    }
}
