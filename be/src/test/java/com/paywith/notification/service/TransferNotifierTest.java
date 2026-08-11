package com.paywith.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.paywith.fds.support.FdsDecisions;
import com.paywith.notification.domain.NotificationType;
import com.paywith.notification.domain.TransferNotificationInfo;
import com.paywith.notification.mapper.NotificationMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("송금 알림 문구·수신자 결정")
class TransferNotifierTest {

    private static final Long TRANSACTION_ID = 300L;
    private static final Long WARD_ID = 7L;

    @Mock
    private NotificationMapper notificationMapper;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private TransferNotifier notifier;

    private void givenTransfer() {
        TransferNotificationInfo info = new TransferNotificationInfo();
        info.setWardId(WARD_ID);
        info.setAmount(150000L);
        info.setRecipientName("김철수");
        given(notificationMapper.findTransferNotificationInfo(TRANSACTION_ID)).willReturn(info);
    }

    private String capturedGuardianBody() {
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        then(notificationService).should()
            .notifyGuardians(eq(WARD_ID), any(), anyString(), body.capture(), anyString(), anyLong());
        return body.getValue();
    }

    @Test
    @DisplayName("SAFE 판정은 조회조차 하지 않는다")
    void notifyRiskDetected_doesNothingWhenSafe() {
        notifier.notifyRiskDetected(TRANSACTION_ID, FdsDecisions.safe());

        then(notificationMapper).should(never()).findTransferNotificationInfo(any());
        then(notificationService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("보류(HELD)는 승인 요청 종류로 보호자에게 간다")
    void notifyRiskDetected_sendsApprovalRequestWhenHeld() {
        givenTransfer();

        notifier.notifyRiskDetected(TRANSACTION_ID, FdsDecisions.held());

        then(notificationService).should().notifyGuardians(eq(WARD_ID),
            eq(NotificationType.APPROVAL_REQUEST), anyString(), anyString(),
            eq("TRANSACTION"), eq(TRANSACTION_ID));
    }

    @Test
    @DisplayName("블랙리스트 차단은 차단 사실을 알린다 — 승인 요청이 아니다")
    void notifyRiskDetected_sendsBlockedNoticeWhenBlacklisted() {
        givenTransfer();

        notifier.notifyRiskDetected(TRANSACTION_ID, FdsDecisions.blocked());

        then(notificationService).should().notifyGuardians(eq(WARD_ID),
            eq(NotificationType.ANOMALY), eq("송금 차단 알림"), anyString(), anyString(), anyLong());
        assertThat(capturedGuardianBody()).contains("김철수", "150,000", "차단");
    }

    @Test
    @DisplayName("주의 문구는 완료를 단정하지 않는다 — 이 알림은 송금 실행 전에 나간다")
    void notifyRiskDetected_cautionBodyDoesNotClaimCompletion() {
        givenTransfer();

        notifier.notifyRiskDetected(TRANSACTION_ID, FdsDecisions.caution());

        String body = capturedGuardianBody();
        assertThat(body).contains("김철수", "150,000");
        assertThat(body).doesNotContain("완료");
    }

    @Test
    @DisplayName("거래를 찾지 못하면 알림을 건너뛴다")
    void notifyRiskDetected_skipsWhenTransactionMissing() {
        given(notificationMapper.findTransferNotificationInfo(TRANSACTION_ID)).willReturn(null);

        notifier.notifyRiskDetected(TRANSACTION_ID, FdsDecisions.caution());

        then(notificationService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("조회가 실패해도 예외를 내보내지 않는다 — 알림이 송금을 뒤집으면 안 된다")
    void notifyRiskDetected_doesNotThrowWhenLookupFails() {
        given(notificationMapper.findTransferNotificationInfo(TRANSACTION_ID))
            .willThrow(new RuntimeException("조회 실패"));

        assertThatCode(() -> notifier.notifyRiskDetected(TRANSACTION_ID, FdsDecisions.caution()))
            .doesNotThrowAnyException();
        then(notificationService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("거절은 피보호자 본인에게 간다")
    void notifyRejected_notifiesWardOnly() {
        givenTransfer();

        notifier.notifyRejected(TRANSACTION_ID);

        then(notificationService).should().notifyUser(eq(WARD_ID),
            eq(NotificationType.APPROVAL_RESULT), eq("송금이 거절되었습니다"), anyString(),
            eq("TRANSACTION"), eq(TRANSACTION_ID));
        then(notificationService).should(never())
            .notifyGuardians(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("승인 후 송금이 끝나면 완료를 알린다")
    void notifyTransferCompleted_notifiesWard() {
        givenTransfer();

        notifier.notifyTransferCompleted(TRANSACTION_ID);

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        then(notificationService).should().notifyUser(eq(WARD_ID), any(),
            eq("송금이 완료되었습니다"), body.capture(), anyString(), anyLong());
        assertThat(body.getValue()).contains("김철수", "150,000");
    }

    @Test
    @DisplayName("실패 알림은 응답과 같은 사유를 그대로 쓴다 — 문구가 갈리면 안 된다")
    void notifyTransferFailed_usesGivenReason() {
        givenTransfer();

        notifier.notifyTransferFailed(TRANSACTION_ID, "송금 가능한 잔액이 부족합니다.");

        then(notificationService).should().notifyUser(eq(WARD_ID), any(),
            eq("송금이 실패했습니다"), eq("송금 가능한 잔액이 부족합니다."), anyString(), anyLong());
    }

    /**
     * 이 경로는 잔액이 이미 차감된 뒤다. "실패"로 읽히면 다시 보내 이중 송금이 되므로 문구에
     * 그 단어가 들어가면 안 된다.
     */
    @Test
    @DisplayName("결과 불명은 실패로 단정하지 않고 재송금을 막는다")
    void notifyTransferUnresolved_avoidsFailureWordingAndWarnsAgainstRetry() {
        givenTransfer();

        notifier.notifyTransferUnresolved(TRANSACTION_ID);

        ArgumentCaptor<String> title = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        then(notificationService).should().notifyUser(eq(WARD_ID), eq(NotificationType.APPROVAL_RESULT),
            title.capture(), body.capture(), eq("TRANSACTION"), eq(TRANSACTION_ID));

        assertThat(title.getValue()).doesNotContain("실패");
        assertThat(body.getValue()).doesNotContain("실패");
        assertThat(body.getValue()).contains("다시 보내지 마시고");
        assertThat(body.getValue()).contains("김철수", "150,000");
    }

    @Test
    @DisplayName("결과 불명도 거래를 찾지 못하면 건너뛴다")
    void notifyTransferUnresolved_skipsWhenTransactionMissing() {
        given(notificationMapper.findTransferNotificationInfo(TRANSACTION_ID)).willReturn(null);

        notifier.notifyTransferUnresolved(TRANSACTION_ID);

        then(notificationService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("수취인이 없는 거래도 문구를 만들 수 있어야 한다")
    void notifyRiskDetected_fallsBackWhenRecipientMissing() {
        TransferNotificationInfo info = new TransferNotificationInfo();
        info.setWardId(WARD_ID);
        info.setAmount(150000L);
        info.setRecipientName(null);
        given(notificationMapper.findTransferNotificationInfo(TRANSACTION_ID)).willReturn(info);

        assertThatCode(() -> notifier.notifyRiskDetected(TRANSACTION_ID, FdsDecisions.caution()))
            .doesNotThrowAnyException();
        assertThat(capturedGuardianBody()).contains("수취인");
    }
}
