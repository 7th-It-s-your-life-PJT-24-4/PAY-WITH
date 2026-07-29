package com.paywith.fds.service;

import com.paywith.exception.BusinessException;
import com.paywith.external.fraudcheck.FraudAccountClient;
import com.paywith.fds.domain.RecipientRiskInfo;
import com.paywith.fds.dto.FdsEvaluationRequest;
import com.paywith.fds.mapper.FdsHistoryMapper;
import com.paywith.fds.service.rule.RuleContext;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * 세 테이블에 걸쳐 있어 단일 쿼리로 합치지 않는다. 억지로 조인하면 실행 계획만 나빠진다.
 * 외부 조회도 여기서 하는데, 이 단계는 트랜잭션 밖이라 응답을 기다려도 DB 커넥션을 잡지 않는다.
 */
@Service
public class RuleContextCollectorServiceImpl implements RuleContextCollectorService {

    private static final Logger log = LoggerFactory.getLogger(RuleContextCollectorServiceImpl.class);

    private final FdsHistoryMapper fdsHistoryMapper;
    private final FraudAccountClient fraudAccountClient;
    private final int repeatedWindowMinutes;
    private final int divisionWindowMinutes;
    private final List<String> memoKeywords;

    public RuleContextCollectorServiceImpl(
        FdsHistoryMapper fdsHistoryMapper,
        FraudAccountClient fraudAccountClient,
        @Value("${fds.repeated.window-minutes}") int repeatedWindowMinutes,
        @Value("${fds.division.window-minutes}") int divisionWindowMinutes,
        @Value("${fds.memo-keywords}") String[] memoKeywords
    ) {
        this.fdsHistoryMapper = fdsHistoryMapper;
        this.fraudAccountClient = fraudAccountClient;
        this.repeatedWindowMinutes = repeatedWindowMinutes;
        this.divisionWindowMinutes = divisionWindowMinutes;
        this.memoKeywords = Arrays.asList(memoKeywords);
    }

    /**
     * 실패하면 신고되지 않은 것으로 본다. fail-closed 로 두면 외부 장애가 곧 송금 중단이 되고,
     * 실패해도 나머지 룰 판정은 그대로 돌아 위험 거래가 무방비로 통과하지는 않는다.
     */
    private boolean lookupFraudAccount(RecipientRiskInfo recipientRiskInfo) {
        try {
            return fraudAccountClient.isReportedAsFraud(
                recipientRiskInfo.getBankCode(), recipientRiskInfo.getAccountNo());
        } catch (RuntimeException e) {
            log.warn("사기계좌 조회 실패. 신고되지 않은 것으로 처리한다. bankCode={}",
                recipientRiskInfo.getBankCode(), e);
            return false;
        }
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
            .recipientReportedAsFraud(lookupFraudAccount(recipientRiskInfo))
            .pendingApprovalExists(fdsHistoryMapper.existsPendingApproval(walletId))
            .memoKeywords(memoKeywords)
            .build();
    }
}
