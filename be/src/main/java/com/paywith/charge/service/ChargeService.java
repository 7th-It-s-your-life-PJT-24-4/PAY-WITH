package com.paywith.charge.service;

import com.paywith.charge.dto.ChargeRequest;
import com.paywith.charge.dto.ChargeResponse;

public interface ChargeService {
    ChargeResponse charge(Long userId, ChargeRequest request);
}
