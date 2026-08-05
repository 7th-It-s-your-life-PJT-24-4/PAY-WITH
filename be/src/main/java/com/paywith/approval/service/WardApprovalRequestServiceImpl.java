package com.paywith.approval.service;

import com.paywith.approval.domain.ApprovalRequestView;
import com.paywith.approval.dto.WardApprovalCancelResponse;
import com.paywith.approval.dto.WardApprovalDetailResponse;
import com.paywith.approval.mapper.ApprovalRequestMapper;
import com.paywith.approval.mapper.TransactionApprovalMapper;
import com.paywith.exception.BusinessException;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WardApprovalRequestServiceImpl implements WardApprovalRequestService {

    private final ApprovalRequestMapper approvalRequestMapper;
    private final TransactionApprovalMapper transactionApprovalMapper;

    /**
     * 본인 건인지의 판정을 쿼리에 맡긴다. 조회 결과가 null 인 경우는 (1) 없는 ID, (2) 남의 건,
     * (3) 이미 처리·만료된 건 셋인데, 모두 404 로 합쳐 응답한다. 남의 승인요청에 대해 403 과 404 를
     * 구분해주면 존재 여부 자체가 새어나가기 때문이다.
     *
     * <p>보호자용 {@code findDetail} 과 달리 findRuleHits 를 부르지 않아 조회가 하나뿐이지만,
     * 조회 전용임을 계약으로 남기려고 {@code readOnly} 는 그대로 건다.
     */
    @Override
    @Transactional(readOnly = true)
    public WardApprovalDetailResponse findDetailByWard(Long approvalId, Long wardId) {
        ApprovalRequestView view = approvalRequestMapper.findByIdAndWardId(approvalId, wardId);
        if (view == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "승인요청을 찾을 수 없습니다.");
        }
        return new WardApprovalDetailResponse(view);
    }

    @Override
    @Transactional
    public WardApprovalCancelResponse cancel(Long approvalId, Long wardId) {
        Long transactionId =
            approvalRequestMapper.findTransactionIdByIdAndWardId(approvalId, wardId);
        if (transactionId == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "승인요청을 찾을 수 없습니다.");
        }

        LocalDateTime canceledAt = LocalDateTime.now();
        if (approvalRequestMapper.cancelByWard(approvalId, wardId, canceledAt) == 0) {
            throw new BusinessException(
                HttpStatus.CONFLICT, "이미 처리되었거나 만료된 승인요청입니다.");
        }
        if (transactionApprovalMapper.cancelHeldByWard(transactionId) == 0) {
            throw new BusinessException(
                HttpStatus.CONFLICT, "취소할 수 없는 상태의 거래입니다.");
        }

        return new WardApprovalCancelResponse(approvalId, transactionId, canceledAt);
    }
}
