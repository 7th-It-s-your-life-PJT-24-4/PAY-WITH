package com.paywith.charge.service;

import com.paywith.charge.dto.ChargeDetailResponse;
import com.paywith.charge.dto.ChargeHistoryListResponse;
import com.paywith.charge.dto.ChargeRequest;
import com.paywith.charge.dto.ChargeResponse;

public interface ChargeService {
    // 시니어 충전용 메서드
    ChargeResponse charge(Long userId, ChargeRequest request);

    // 보호자 충전용 메서드
    ChargeResponse chargeByGuard(Long guardId, Long wardId, ChargeRequest request);

    // 보호자 충전 조회 메서드
    ChargeHistoryListResponse getChargeHistories(Long guardId);

    // 보호자 충전 상세 조회 메서드
    ChargeDetailResponse getChargeDetail(Long guardId, Long transactionId);
}
