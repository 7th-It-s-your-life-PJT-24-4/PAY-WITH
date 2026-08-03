package com.paywith.merchant.service;

import com.paywith.merchant.dto.MerchantListResponse;
import com.paywith.merchant.mapper.MerchantMapper;
import org.springframework.stereotype.Service;

@Service
public class MerchantService {

    private final MerchantMapper merchantMapper;

    public MerchantService(MerchantMapper merchantMapper) {
        this.merchantMapper = merchantMapper;
    }

    public MerchantListResponse findAllPayable() {
        return new MerchantListResponse(merchantMapper.findAllPayable());
    }
}
