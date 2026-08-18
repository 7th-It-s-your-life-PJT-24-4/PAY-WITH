package com.paywith.fds.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

import com.paywith.exception.BusinessException;
import com.paywith.external.fraudcheck.FraudAccountClient;
import com.paywith.fds.domain.RecipientRiskInfo;
import com.paywith.fds.dto.FdsEvaluationRequest;
import com.paywith.fds.mapper.FdsHistoryMapper;
import com.paywith.fds.service.rule.RuleContext;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
@DisplayName("송금 FDS 판정 입력 수집")
class RuleContextCollectorServiceImplTest {

    private static final long TRANSACTION_ID = 100L;
    private static final long WALLET_ID = 200L;
    private static final long RECIPIENT_ID = 300L;

    // 두 윈도우를 일부러 다른 값으로 둔다. 같으면 둘을 뒤바꿔 넘기는 버그를 잡을 수 없다.
    private static final int REPEATED_WINDOW_MINUTES = 10;
    private static final int DIVISION_WINDOW_MINUTES = 30;

    private static final String[] MEMO_KEYWORDS = {"검찰", "대출"};

    private static final String BANK_CODE = "004";
    private static final String ACCOUNT_NO = "1234567890";

    @Mock
    private FdsHistoryMapper fdsHistoryMapper;
    @Mock
    private FraudAccountClient fraudAccountClient;

    private RuleContextCollectorServiceImpl collector;

    @BeforeEach
    void setUp() {
        collector = new RuleContextCollectorServiceImpl(
            fdsHistoryMapper,
            fraudAccountClient,
            REPEATED_WINDOW_MINUTES,
            DIVISION_WINDOW_MINUTES,
            MEMO_KEYWORDS);
    }

    private FdsEvaluationRequest request(String amount, String memo) {
        return new FdsEvaluationRequest(
            TRANSACTION_ID, WALLET_ID, RECIPIENT_ID, new BigDecimal(amount), memo);
    }

    private RecipientRiskInfo recipient(int sendCount, boolean registeredSafe) {
        RecipientRiskInfo info = new RecipientRiskInfo();
        info.setBankCode(BANK_CODE);
        info.setAccountNo(ACCOUNT_NO);
        info.setSendCount(sendCount);
        info.setRegisteredSafe(registeredSafe);
        return info;
    }

    // 나머지 조회는 stub 하지 않으면 Mockito 기본값(int 0, boolean false)이 나온다.
    // 그 값에 관심 없는 테스트는 이 한 줄이면 collect() 가 끝까지 돈다.
    private void givenRecipient(RecipientRiskInfo info) {
        given(fdsHistoryMapper.findRecipientRiskInfo(RECIPIENT_ID)).willReturn(info);
    }

    @Test
    void collect_throwsNotFoundWhenRecipientMissing() {
        givenRecipient(null);

        assertThatThrownBy(() -> collector.collect(request("30000", null)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasMessageContaining("수취인 정보를 찾을 수 없습니다.");
    }

    @Test
    void collect_mapsRequestAndRecipientFieldsIntoContext() {
        givenRecipient(recipient(5, true));

        RuleContext context = collector.collect(request("30000", "생활비"));

        assertThat(context.getAmount()).isEqualByComparingTo("30000");
        assertThat(context.getMemo()).isEqualTo("생활비");
        assertThat(context.getRecipientSendCount()).isEqualTo(5);
        assertThat(context.isRecipientRegisteredSafe()).isTrue();
    }

    @Test
    void collect_treatsFraudLookupFailureAsNotReported() {
        givenRecipient(recipient(5, false));
        willThrow(new RuntimeException("외부 조회 실패"))
                .given(fraudAccountClient).isReportedAsFraud(BANK_CODE, ACCOUNT_NO);

        RuleContext context = collector.collect(request("30000", null));

        assertThat(context.isRecipientReportedAsFraud()).isFalse();
    }

    @Test
    void collect_appliesRepeatedWindowToRecentTransferCutoff() {
        givenRecipient(recipient(5, false));
        LocalDateTime beforeCall = LocalDateTime.now();

        collector.collect(request("30000", null));

        ArgumentCaptor<LocalDateTime> since = ArgumentCaptor.forClass(LocalDateTime.class);
        then(fdsHistoryMapper).should()
                .countRecentTransfers(eq(WALLET_ID), since.capture(), anyLong());
        assertThat(since.getValue()).isBetween(
                beforeCall.minusMinutes(REPEATED_WINDOW_MINUTES),
                LocalDateTime.now().minusMinutes(REPEATED_WINDOW_MINUTES));
    }

    @Test
    void collect_appliesDivisionWindowToDistinctRecipientCutoff() {
        givenRecipient(recipient(5, false));
        LocalDateTime beforeCall = LocalDateTime.now();

        collector.collect(request("30000", null));

        ArgumentCaptor<LocalDateTime> since = ArgumentCaptor.forClass(LocalDateTime.class);
        then(fdsHistoryMapper).should()
                .countRecentDistinctRecipients(eq(WALLET_ID), since.capture(), anyLong());
        assertThat(since.getValue()).isBetween(
                beforeCall.minusMinutes(DIVISION_WINDOW_MINUTES),
                LocalDateTime.now().minusMinutes(DIVISION_WINDOW_MINUTES));
    }

    @Test
    void collect_excludesCurrentTransactionFromRecentCounts() {
        givenRecipient(recipient(5, false));

        collector.collect(request("30000", null));

        then(fdsHistoryMapper).should()
                .countRecentTransfers(eq(WALLET_ID), any(LocalDateTime.class), eq(TRANSACTION_ID));
        then(fdsHistoryMapper).should()
                .countRecentDistinctRecipients(eq(WALLET_ID), any(LocalDateTime.class), eq(TRANSACTION_ID));
    }

    @Test
    void collect_queriesApprovalHistoryWithMatchingIds() {
        givenRecipient(recipient(5, false));
        given(fdsHistoryMapper.existsRejectedApproval(RECIPIENT_ID)).willReturn(true);
        given(fdsHistoryMapper.existsPendingApproval(WALLET_ID)).willReturn(true);

        RuleContext context = collector.collect(request("30000", null));

        assertThat(context.isRecipientRejectedBefore()).isTrue();
        assertThat(context.isPendingApprovalExists()).isTrue();
    }
}
