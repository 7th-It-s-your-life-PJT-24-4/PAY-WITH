package com.paywith.approval.service;

import com.paywith.approval.domain.ApprovalRequest;
import com.paywith.approval.domain.ApprovalRequestView;
import com.paywith.approval.dto.ApprovalDecisionResponse;
import com.paywith.approval.dto.ApprovalHistoryResultResponse;
import com.paywith.approval.dto.ApprovalRequestDetailResponse;
import com.paywith.approval.dto.ApprovalRequestSummaryResponse;
import com.paywith.approval.mapper.ApprovalRequestMapper;
import com.paywith.approval.mapper.TransactionApprovalMapper;
import com.paywith.exception.BusinessException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApprovalRequestServiceImpl implements ApprovalRequestService {

    private static final Set<String> HISTORY_STATUSES =
        Set.of("APPROVED", "REJECTED", "CANCELED", "EXPIRED");

    private final ApprovalRequestMapper approvalRequestMapper;
    private final TransactionApprovalMapper transactionApprovalMapper;
    private final int expireMinutes;

    public ApprovalRequestServiceImpl(
        ApprovalRequestMapper approvalRequestMapper,
        TransactionApprovalMapper transactionApprovalMapper,
        @Value("${fds.approval.expire-minutes}") int expireMinutes
    ) {
        this.approvalRequestMapper = approvalRequestMapper;
        this.transactionApprovalMapper = transactionApprovalMapper;
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

    @Override
    public List<ApprovalRequestSummaryResponse> findPending(Long guardId, Long wardId) {
        return approvalRequestMapper.findPendingByGuardId(guardId, wardId).stream()
            .map(ApprovalRequestSummaryResponse::new)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApprovalRequestSummaryResponse> findHistory(
        Long guardId, Long wardId, String status) {
        String normalizedStatus = normalizeHistoryStatus(status);
        return approvalRequestMapper
            .findHistoryByGuardId(guardId, wardId, normalizedStatus)
            .stream()
            .map(ApprovalRequestSummaryResponse::new)
            .collect(Collectors.toList());
    }

    @Override
    public ApprovalRequestDetailResponse findDetail(Long approvalId, Long guardId) {
        ApprovalRequestView view = approvalRequestMapper.findByIdAndGuardId(approvalId, guardId);
        if (view == null) {
            throw notFound();
        }
        return new ApprovalRequestDetailResponse(
            view, approvalRequestMapper.findRuleHits(view.getTransactionId()));
    }

    @Override
    @Transactional(readOnly = true)
    public ApprovalHistoryResultResponse findHistoryResult(Long approvalId, Long guardId) {
        ApprovalRequestView view =
            approvalRequestMapper.findHistoryResultByIdAndGuardId(approvalId, guardId);
        if (view == null) {
            throw notFound();
        }
        return new ApprovalHistoryResultResponse(
            view, approvalRequestMapper.findRuleHits(view.getTransactionId()));
    }

    @Override
    @Transactional
    public ApprovalDecisionResponse approve(Long approvalId, Long guardId) {
        return decide(approvalId, guardId, "APPROVED");
    }

    @Override
    @Transactional
    public ApprovalDecisionResponse reject(Long approvalId, Long guardId) {
        return decide(approvalId, guardId, "REJECTED");
    }

    /**
     * 승인과 거절은 기록하는 상태값만 다르고 절차가 같다.
     *
     * <p>담당 확인(404) → 조건부 UPDATE(409) → transactions 반영 순서다. 상태 검사를 조회로
     * 미리 하지 않고 UPDATE 의 WHERE 에 맡기는 이유는, 검사와 갱신 사이에 만료나 다른 보호자의
     * 처리가 끼어들 틈을 없애기 위해서다.
     */
    private ApprovalDecisionResponse decide(Long approvalId, Long guardId, String status) {
        Long transactionId =
            approvalRequestMapper.findTransactionIdByIdAndGuardId(approvalId, guardId);
        if (transactionId == null) {
            throw notFound();
        }

        LocalDateTime respondedAt = LocalDateTime.now();
        int updated = approvalRequestMapper.updateDecision(approvalId, guardId, status, respondedAt);
        if (updated == 0) {
            throw new BusinessException(
                HttpStatus.CONFLICT, "이미 처리되었거나 만료된 승인요청입니다.");
        }

        // 같은 트랜잭션에서 비정규화 복사본을 맞춘다(schema.sql 의 single writer 규칙).
        transactionApprovalMapper.updateStatus(transactionId, status);

        return new ApprovalDecisionResponse(approvalId, transactionId, status, respondedAt);
    }

    /** 담당이 아닌 보호자에게 존재 여부를 알려주지 않기 위해 권한 부족도 404 로 처리한다. */
    private BusinessException notFound() {
        return new BusinessException(HttpStatus.NOT_FOUND, "승인요청을 찾을 수 없습니다.");
    }

    private String normalizeHistoryStatus(String status) {
        String normalized = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        if (!HISTORY_STATUSES.contains(normalized)) {
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "REQUEST_001",
                "이력 상태는 APPROVED, REJECTED, CANCELED, EXPIRED만 조회할 수 있습니다.");
        }
        return normalized;
    }
}
