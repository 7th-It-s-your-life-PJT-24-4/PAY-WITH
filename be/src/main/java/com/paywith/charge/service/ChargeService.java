package com.paywith.charge.service;

import com.paywith.charge.dto.ChargeRequest;
import com.paywith.charge.dto.ChargeResponse;

public interface ChargeService {
    // 시니어 충전용 메서드
    ChargeResponse charge(Long userId, ChargeRequest request);

    // 보호자 충전용 메서드
    ChargeResponse chargeByGuard(Long guardId, Long wardId, ChargeRequest request);
}
