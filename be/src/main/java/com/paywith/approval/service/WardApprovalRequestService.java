package com.paywith.approval.service;

import com.paywith.approval.dto.WardApprovalCancelResponse;
import com.paywith.approval.dto.WardApprovalDetailResponse;

/**
 * 피보호자가 자기 승인요청을 보는 경로.
 *
 * <p>{@link ApprovalRequestService} 와 나눈 이유는 그쪽 메서드가 모두 두 번째 인자로 guardId 를
 * 받는 계약이기 때문이다. 같은 자리에 wardId 를 받는 메서드를 섞으면 인자의 의미가 호출부마다
 * 달라져, 인가 조건을 잘못 넘겨도 컴파일러가 잡아주지 못한다.
 *
 * <p>승인/거절은 여기 두지 않는다. 본인이 자기 보류 거래를 승인하면 FDS 자체가 무의미해진다.
 */
public interface WardApprovalRequestService {

    /**
     * 피보호자 본인의 승인 대기 건 상세.
     *
     * @param approvalId 홈 화면 목록에서 받은 승인요청 ID
     * @param wardId 인증된 사용자 ID. 대상은 항상 본인이다
     * @return 보류 사유(발동 룰)를 뺀 상세
     * @throws com.paywith.exception.BusinessException 본인 건이 아니거나 이미 처리·만료된 경우 404
     */
    WardApprovalDetailResponse findDetailByWard(Long approvalId, Long wardId);

    /** 피보호자가 본인의 미만료 승인 대기 송금을 취소한다. */
    WardApprovalCancelResponse cancel(Long approvalId, Long wardId);
}
