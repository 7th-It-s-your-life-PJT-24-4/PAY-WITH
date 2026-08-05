package com.paywith.approval.service;

import com.paywith.approval.domain.ApprovalRequest;
import com.paywith.approval.dto.ApprovalDecisionResponse;
import com.paywith.approval.dto.ApprovalDecisionResultResponse;
import com.paywith.approval.dto.ApprovalRequestDetailResponse;
import com.paywith.approval.dto.ApprovalRequestSummaryResponse;
import java.util.List;

/**
 * 보호자 승인요청 도메인 서비스.
 */
public interface ApprovalRequestService {

    /** FDS 위험(DANGER) 판정 시 승인요청을 생성한다. */
    ApprovalRequest create(Long transactionId);

    /**
     * 보호자가 담당하는 시니어들의 승인 대기 목록. 만료된 건은 제외한다.
     *
     * @param wardId null 이면 담당 전체, 값이 있으면 그 피보호자 건만
     */
    List<ApprovalRequestSummaryResponse> findPending(Long guardId, Long wardId);

    /** 보호자가 직접 승인 또는 거절한 이상거래 목록. */
    List<ApprovalRequestSummaryResponse> findDecisionHistory(
        Long guardId, Long wardId, String status);

    /** 승인 판단에 필요한 상세. 담당하지 않는 시니어의 건이면 404. */
    ApprovalRequestDetailResponse findDetail(Long approvalId, Long guardId);

    /** 승인 또는 거절이 끝난 요청의 상세와 처리 결과. */
    ApprovalDecisionResultResponse findDecisionResult(Long approvalId, Long guardId);

    /**
     * 보류된 송금을 승인한다.
     *
     * <p>approval_requests 와 transactions 의 상태를 같은 트랜잭션에서 함께 갱신한다
     * (schema.sql 의 single writer 규칙).
     */
    ApprovalDecisionResponse approve(Long approvalId, Long guardId);

    /** 보류된 송금을 거절한다. 상태 갱신 범위는 {@link #approve} 와 같다. */
    ApprovalDecisionResponse reject(Long approvalId, Long guardId);
}
