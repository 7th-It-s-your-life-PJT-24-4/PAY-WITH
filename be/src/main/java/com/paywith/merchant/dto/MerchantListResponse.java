package com.paywith.merchant.dto;

import com.paywith.merchant.domain.Merchant;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 스캐너용 가맹점 목록 응답. 좌표는 서버 관리 값이라 응답에 포함하지 않는다
 * (클라이언트와 좌표를 주고받지 않는다는 원칙).
 */
@ApiModel(description = "결제 가능 가맹점 목록(가맹점 스캐너용). 좌표가 등록된 가맹점만 merchant_id 오름차순으로 담으며 좌표 자체는 내려주지 않는다")
@Getter
public class MerchantListResponse {

    @ApiModelProperty(value = "결제 가능(좌표 등록) 가맹점 목록, merchant_id 오름차순. 해당 가맹점이 없으면 빈 배열(null 아님)")
    private final List<MerchantItem> merchants;

    public MerchantListResponse(List<Merchant> merchants) {
        this.merchants = merchants.stream()
            .map(MerchantItem::new)
            .collect(Collectors.toList());
    }

    @ApiModel(description = "결제 가능 가맹점 한 건")
    @Getter
    public static class MerchantItem {

        @ApiModelProperty(value = "가맹점 번호(merchants.merchant_id). 결제 실행 요청의 merchantId로 보낸다", example = "3")
        private final Long merchantId;

        @ApiModelProperty(value = "가맹점명(merchants.name)", example = "한마음경로식당")
        private final String name;

        @ApiModelProperty(value = "업종 코드(nullable 자유 문자열, 최대 30자 — enum 아님). 현재 시드 값은 "
            + "MART·PHARMACY·RESTAURANT·CVS·HOSPITAL·JEWELRY·ELECTRONICS",
            example = "RESTAURANT")
        private final String categoryCode;

        @ApiModelProperty(value = "지역명(nullable 자유 문자열, 최대 100자)", example = "서울 종로구")
        private final String region;

        public MerchantItem(Merchant merchant) {
            this.merchantId = merchant.getMerchantId();
            this.name = merchant.getName();
            this.categoryCode = merchant.getCategoryCode();
            this.region = merchant.getRegion();
        }
    }
}
