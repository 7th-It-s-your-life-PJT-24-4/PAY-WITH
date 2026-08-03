package com.paywith.approval.service;

/**
 * 응답 시한이 지난 승인요청의 종결 처리.
 *
 * <p>보호자가 직접 일으키는 승인/거절({@link ApprovalRequestService})과 달리 시스템이 시간에
 * 따라 수행하는 작업이라 서비스를 나눴다.
 */
public interface ApprovalExpiryService {

    /**
     * 시한이 지난 승인 대기 건을 EXPIRED 로, 그 거래를 CANCELED 로 종결한다.
     *
     * @return 만료 처리한 승인요청 수
     */
    int expireOverdue();
}
