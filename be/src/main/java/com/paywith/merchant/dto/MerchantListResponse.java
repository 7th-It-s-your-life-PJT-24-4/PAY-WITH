package com.paywith.merchant.dto;

import com.paywith.merchant.domain.Merchant;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 스캐너용 가맹점 목록 응답. 좌표는 서버 관리 값이라 응답에 포함하지 않는다
 * (클라이언트와 좌표를 주고받지 않는다는 원칙).
 */
@Getter
public class MerchantListResponse {

    private final List<MerchantItem> merchants;

    public MerchantListResponse(List<Merchant> merchants) {
        this.merchants = merchants.stream()
            .map(MerchantItem::new)
            .collect(Collectors.toList());
    }

    @Getter
    public static class MerchantItem {

        private final Long merchantId;
        private final String name;
        private final String categoryCode;
        private final String region;

        public MerchantItem(Merchant merchant) {
            this.merchantId = merchant.getMerchantId();
            this.name = merchant.getName();
            this.categoryCode = merchant.getCategoryCode();
            this.region = merchant.getRegion();
        }
    }
}
