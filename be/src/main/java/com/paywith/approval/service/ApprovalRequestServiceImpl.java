package com.paywith.approval.service;

import com.paywith.approval.domain.ApprovalRequest;
import com.paywith.approval.mapper.ApprovalRequestMapper;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApprovalRequestServiceImpl implements ApprovalRequestService {

    private final ApprovalRequestMapper approvalRequestMapper;
    private final int expireMinutes;

    public ApprovalRequestServiceImpl(
        ApprovalRequestMapper approvalRequestMapper,
        @Value("${fds.approval.expire-minutes}") int expireMinutes
    ) {
        this.approvalRequestMapper = approvalRequestMapper;
        this.expireMinutes = expireMinutes;
    }

    @Override
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
