package com.paywith.approval.service;

import com.paywith.approval.domain.ApprovalRequest;
import com.paywith.approval.mapper.ApprovalRequestMapper;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 보호자 승인요청 도메인 서비스. FDS 보류(HELD) 판정 시 승인요청을 생성한다.
 */
@Service
public class ApprovalRequestService {

    private final ApprovalRequestMapper approvalRequestMapper;
    private final int expireMinutes;

    public ApprovalRequestService(
        ApprovalRequestMapper approvalRequestMapper,
        @Value("${fds.approval.expire-minutes}") int expireMinutes
    ) {
        this.approvalRequestMapper = approvalRequestMapper;
        this.expireMinutes = expireMinutes;
    }

    @Transactional
    public ApprovalRequest create(Long transactionId) {
        ApprovalRequest approvalRequest = new ApprovalRequest();
        approvalRequest.setTransactionId(transactionId);
        approvalRequest.setStatus("PENDING");
        approvalRequest.setExpiredAt(LocalDateTime.now().plusMinutes(expireMinutes));
        approvalRequestMapper.insert(approvalRequest);
        return approvalRequest;
    }
}
