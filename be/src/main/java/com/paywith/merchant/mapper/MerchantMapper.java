package com.paywith.merchant.mapper;

import com.paywith.merchant.domain.Merchant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MerchantMapper {

    /** 결제 실행 시 가맹점 검증·좌표 조회용 (미존재 404 판정은 서비스 담당) */
    Merchant findById(@Param("merchantId") Long merchantId);

    /**
     * 스캐너용 결제 가능 가맹점 목록. 좌표 미등록 가맹점은 결제가 거부되므로
     * (A1 — 거래에 서버 조회 좌표를 복사) 목록에서도 제외한다.
     * merchants.is_active는 미도입 상태라 좌표 유무가 유일한 결제 가능 기준이다.
     */
    List<Merchant> findAllPayable();
}
