package com.paywith.approval.service;

import com.paywith.approval.domain.ApprovalRequest;

/**
 * 보호자 승인요청 도메인 서비스.
 */
public interface ApprovalRequestService {

    /** FDS 위험(DANGER) 판정 시 승인요청을 생성한다. */
    ApprovalRequest create(Long transactionId);
}
