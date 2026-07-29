package com.paywith.fds.service;

import com.paywith.exception.BusinessException;
import com.paywith.fds.domain.RecipientRiskInfo;
import com.paywith.fds.dto.FdsEvaluationRequest;
import com.paywith.fds.mapper.FdsHistoryMapper;
import com.paywith.fds.service.rule.RuleContext;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * 세 테이블(recipients/transactions/approval_requests)에 걸쳐 있어 단일 쿼리로 합치지 않는다.
 * 억지로 조인하면 실행 계획이 나빠질 뿐 조회 횟수를 줄이는 이점이 상쇄된다.
 */
@Service
public class RuleContextCollectorServiceImpl implements RuleContextCollectorService {

    private final FdsHistoryMapper fdsHistoryMapper;
    private final int repeatedWindowMinutes;
    private final int divisionWindowMinutes;
    private final List<String> memoKeywords;

    public RuleContextCollectorServiceImpl(
        FdsHistoryMapper fdsHistoryMapper,
        @Value("${fds.repeated.window-minutes}") int repeatedWindowMinutes,
        @Value("${fds.division.window-minutes}") int divisionWindowMinutes,
        @Value("${fds.memo-keywords}") String[] memoKeywords
    ) {
        this.fdsHistoryMapper = fdsHistoryMapper;
        this.repeatedWindowMinutes = repeatedWindowMinutes;
        this.divisionWindowMinutes = divisionWindowMinutes;
        this.memoKeywords = Arrays.asList(memoKeywords);
    }

    @Override
    public RuleContext collect(FdsEvaluationRequest request) {
        RecipientRiskInfo recipientRiskInfo =
            fdsHistoryMapper.findRecipientRiskInfo(request.getRecipientId());
        if (recipientRiskInfo == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "수취인 정보를 찾을 수 없습니다.");
        }

        LocalDateTime now = LocalDateTime.now();
        Long walletId = request.getWalletId();
        Long currentTransactionId = request.getTransactionId();

        return RuleContext.builder()
            .amount(request.getAmount())
            .memo(request.getMemo())
            .requestedAt(now)
            .recipientSendCount(recipientRiskInfo.getSendCount())
            .recipientRegisteredSafe(recipientRiskInfo.isRegisteredSafe())
            .recentTransferCount(fdsHistoryMapper.countRecentTransfers(
                walletId, now.minusMinutes(repeatedWindowMinutes), currentTransactionId))
            .recentDistinctRecipientCount(fdsHistoryMapper.countRecentDistinctRecipients(
                walletId, now.minusMinutes(divisionWindowMinutes), currentTransactionId))
            .recipientRejectedBefore(
                fdsHistoryMapper.existsRejectedApproval(request.getRecipientId()))
            .pendingApprovalExists(fdsHistoryMapper.existsPendingApproval(walletId))
            .memoKeywords(memoKeywords)
            .build();
    }
}
