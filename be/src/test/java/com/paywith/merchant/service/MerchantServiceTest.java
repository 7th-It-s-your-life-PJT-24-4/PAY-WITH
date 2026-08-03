package com.paywith.merchant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.paywith.merchant.domain.Merchant;
import com.paywith.merchant.dto.MerchantListResponse;
import com.paywith.merchant.mapper.MerchantMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MerchantServiceTest {

    @Mock
    private MerchantMapper merchantMapper;

    @InjectMocks
    private MerchantService merchantService;

    @Test
    void 목록은_도메인_4필드만_복사해_응답한다() {
        given(merchantMapper.findAllPayable()).willReturn(List.of(
            merchant(1L, "행복마트 종로점", "MART", "서울 종로구"),
            merchant(2L, "정든약국", "PHARMACY", "서울 종로구")
        ));

        MerchantListResponse response = merchantService.findAllPayable();

        assertThat(response.getMerchants()).hasSize(2);
        MerchantListResponse.MerchantItem first = response.getMerchants().get(0);
        assertThat(first.getMerchantId()).isEqualTo(1L);
        assertThat(first.getName()).isEqualTo("행복마트 종로점");
        assertThat(first.getCategoryCode()).isEqualTo("MART");
        assertThat(first.getRegion()).isEqualTo("서울 종로구");
    }

    @Test
    void 결제_가능_가맹점이_없으면_빈_목록을_반환한다() {
        given(merchantMapper.findAllPayable()).willReturn(List.of());

        assertThat(merchantService.findAllPayable().getMerchants()).isEmpty();
    }

    private Merchant merchant(Long merchantId, String name, String categoryCode, String region) {
        Merchant merchant = new Merchant();
        merchant.setMerchantId(merchantId);
        merchant.setName(name);
        merchant.setCategoryCode(categoryCode);
        merchant.setRegion(region);
        merchant.setLatitude(new BigDecimal("37.5729000"));
        merchant.setLongitude(new BigDecimal("126.9793000"));
        return merchant;
    }
}
