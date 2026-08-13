package com.paywith.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import com.paywith.notification.domain.Notification;
import com.paywith.notification.domain.NotificationType;
import com.paywith.notification.mapper.NotificationMapper;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
@DisplayName("알림 저장")
class NotificationServiceImplTest {

    private static final Long SENIOR_ID = 10L;
    private static final Long WARD_ID = 11L;
    private static final Long TRANSACTION_ID = 200L;

    @Mock
    private NotificationMapper notificationMapper;

    @Mock
    private PushDispatcher pushDispatcher;

    @InjectMocks
    private NotificationServiceImpl service;

    @AfterEach
    void clearSynchronization() {
        // 스레드 로컬이라 정리하지 않으면 다음 테스트가 이 상태를 물려받는다.
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    /** 실제 트랜잭션 없이 afterCommit 등록/발화만 흉내 낸다. */
    private void commit() {
        List.copyOf(TransactionSynchronizationManager.getSynchronizations())
            .forEach(TransactionSynchronization::afterCommit);
    }

    @Test
    @DisplayName("ACTIVE 보호자 전원에게 같은 내용의 행을 남긴다 — 읽음 여부가 보호자별이라 인원수만큼")
    void 보호자_전원에게_행을_남긴다() {
        given(notificationMapper.findActiveGuardIds(SENIOR_ID)).willReturn(List.of(1L, 2L));

        service.notifyGuardians(SENIOR_ID, NotificationType.ANOMALY,
            "결제 차단", "위험 결제가 차단되었습니다.", "TRANSACTION", TRANSACTION_ID);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        then(notificationMapper).should(times(2)).insert(captor.capture());

        List<Notification> saved = captor.getAllValues();
        assertThat(saved).extracting(Notification::getUserId).containsExactly(1L, 2L);
        assertThat(saved).allSatisfy(notification -> {
            assertThat(notification.getType()).isEqualTo(NotificationType.ANOMALY);
            assertThat(notification.getTitle()).isEqualTo("결제 차단");
            assertThat(notification.getBody()).isEqualTo("위험 결제가 차단되었습니다.");
            assertThat(notification.getRefType()).isEqualTo("TRANSACTION");
            assertThat(notification.getRefId()).isEqualTo(TRANSACTION_ID);
        });
    }

    @Test
    @DisplayName("ACTIVE 보호자가 없으면 아무 행도 남기지 않는다 — 페어링 전 피보호자")
    void 보호자가_없으면_저장하지_않는다() {
        given(notificationMapper.findActiveGuardIds(SENIOR_ID)).willReturn(Collections.emptyList());

        service.notifyGuardians(SENIOR_ID, NotificationType.ANOMALY,
            "결제 차단", "위험 결제가 차단되었습니다.", "TRANSACTION", TRANSACTION_ID);

        then(notificationMapper).should(never()).insert(any());
    }

    @Test
    @DisplayName("행 저장이 실패해도 예외를 내보내지 않는다 — 알림이 호출 흐름(결제)을 뒤집으면 안 된다")
    void 저장_실패는_호출_흐름을_뒤집지_않는다() {
        given(notificationMapper.findActiveGuardIds(SENIOR_ID)).willReturn(List.of(1L));
        willThrow(new RuntimeException("insert 실패")).given(notificationMapper).insert(any());

        assertThatCode(() -> service.notifyGuardians(SENIOR_ID, NotificationType.ANOMALY,
            "결제 차단", "위험 결제가 차단되었습니다.", "TRANSACTION", TRANSACTION_ID))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("커밋 전에는 발송하지 않는다 — 롤백된 거래에 푸시가 나가면 되돌릴 수 없다")
    void notifyGuardians_doesNotDispatchUntilCommit() {
        TransactionSynchronizationManager.initSynchronization();
        given(notificationMapper.findActiveGuardIds(SENIOR_ID)).willReturn(List.of(1L));
        given(notificationMapper.findActiveGuardTokens(SENIOR_ID)).willReturn(List.of("TOKEN_A"));

        service.notifyGuardians(SENIOR_ID, NotificationType.ANOMALY,
            "결제 차단", "위험 결제가 차단되었습니다.", "TRANSACTION", TRANSACTION_ID);

        then(pushDispatcher).should(never()).dispatch(any(), any(), any(), any());

        commit();

        then(pushDispatcher).should()
            .dispatch(eq(List.of("TOKEN_A")), eq("결제 차단"), eq("위험 결제가 차단되었습니다."), any());
    }

    @Test
    @DisplayName("보호자 이상거래 상세를 열 수 있도록 거래와 피보호자 식별자를 함께 보낸다")
    void notifyGuardians_includesWardIdForAnomaly() {
        TransactionSynchronizationManager.initSynchronization();
        given(notificationMapper.findActiveGuardIds(SENIOR_ID)).willReturn(List.of(1L));
        given(notificationMapper.findActiveGuardTokens(SENIOR_ID)).willReturn(List.of("TOKEN_A"));

        service.notifyGuardians(SENIOR_ID, NotificationType.ANOMALY,
            "결제 차단", "위험 결제가 차단되었습니다.", "TRANSACTION", TRANSACTION_ID);
        commit();

        ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
        then(pushDispatcher).should().dispatch(any(), any(), any(), captor.capture());
        assertThat(captor.getValue())
            .containsEntry("type", "ANOMALY")
            .containsEntry("refType", "TRANSACTION")
            .containsEntry("refId", String.valueOf(TRANSACTION_ID))
            .containsEntry("wardId", String.valueOf(SENIOR_ID));
    }

    @Test
    @DisplayName("승인 요청은 승인 식별자만 보내고 피보호자 식별자를 중복하지 않는다")
    void notifyGuardians_keepsApprovalRequestPayloadFocused() {
        TransactionSynchronizationManager.initSynchronization();
        given(notificationMapper.findActiveGuardIds(SENIOR_ID)).willReturn(List.of(1L));
        given(notificationMapper.findActiveGuardTokens(SENIOR_ID)).willReturn(List.of("TOKEN_A"));

        service.notifyGuardians(SENIOR_ID, NotificationType.APPROVAL_REQUEST,
            "승인 요청", "확인이 필요한 송금이 있습니다.", "APPROVAL", TRANSACTION_ID);
        commit();

        ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
        then(pushDispatcher).should().dispatch(any(), any(), any(), captor.capture());
        assertThat(captor.getValue())
            .containsEntry("type", "APPROVAL_REQUEST")
            .containsEntry("refType", "APPROVAL")
            .containsEntry("refId", String.valueOf(TRANSACTION_ID))
            .doesNotContainKey("wardId");
    }

    @Test
    @DisplayName("토큰을 등록한 보호자가 없으면 발송을 시도하지 않는다")
    void notifyGuardians_skipsDispatchWhenNoTokens() {
        TransactionSynchronizationManager.initSynchronization();
        given(notificationMapper.findActiveGuardIds(SENIOR_ID)).willReturn(List.of(1L));
        given(notificationMapper.findActiveGuardTokens(SENIOR_ID)).willReturn(Collections.emptyList());

        service.notifyGuardians(SENIOR_ID, NotificationType.ANOMALY,
            "결제 차단", "위험 결제가 차단되었습니다.", "TRANSACTION", TRANSACTION_ID);
        commit();

        then(pushDispatcher).should(never()).dispatch(any(), any(), any(), any());
    }

    @Test
    @DisplayName("트랜잭션 없이 불리면 커밋을 기다리지 않고 바로 보낸다 — 되돌릴 것이 없다")
    void notifyUser_dispatchesImmediatelyWithoutTransaction() {
        given(notificationMapper.findFcmTokenByUserId(WARD_ID)).willReturn("WARD_TOKEN");

        service.notifyUser(WARD_ID, NotificationType.APPROVAL_RESULT,
            "송금 완료", "보호자가 승인해 송금이 완료되었습니다.", "TRANSACTION", TRANSACTION_ID);

        then(pushDispatcher).should().dispatch(eq(List.of("WARD_TOKEN")), any(), any(), any());
    }

    @Test
    @DisplayName("피보호자가 토큰을 등록하지 않았으면 행만 남기고 발송은 건너뛴다")
    void notifyUser_skipsDispatchWhenTokenMissing() {
        given(notificationMapper.findFcmTokenByUserId(WARD_ID)).willReturn(null);

        service.notifyUser(WARD_ID, NotificationType.APPROVAL_RESULT,
            "송금 완료", "보호자가 승인해 송금이 완료되었습니다.", "TRANSACTION", TRANSACTION_ID);

        then(notificationMapper).should(times(1)).insert(any());
        then(pushDispatcher).should(never()).dispatch(any(), any(), any(), any());
    }

    @Test
    @DisplayName("지정한 한 명에게만 행을 남긴다 — 승인 결과는 페어링을 거슬러 올라갈 필요가 없다")
    void notifyUser_savesSingleRowForGivenUser() {
        service.notifyUser(WARD_ID, NotificationType.APPROVAL_RESULT,
            "송금 완료", "보호자가 승인해 송금이 완료되었습니다.", "TRANSACTION", TRANSACTION_ID);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        then(notificationMapper).should(times(1)).insert(captor.capture());
        then(notificationMapper).should(never()).findActiveGuardIds(any());

        Notification saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(WARD_ID);
        assertThat(saved.getType()).isEqualTo(NotificationType.APPROVAL_RESULT);
        assertThat(saved.getTitle()).isEqualTo("송금 완료");
        assertThat(saved.getRefId()).isEqualTo(TRANSACTION_ID);
    }

    @Test
    @DisplayName("단건 저장이 실패해도 예외를 내보내지 않는다 — 승인 흐름을 뒤집으면 안 된다")
    void notifyUser_doesNotThrowWhenInsertFails() {
        willThrow(new RuntimeException("insert 실패")).given(notificationMapper).insert(any());

        assertThatCode(() -> service.notifyUser(WARD_ID, NotificationType.APPROVAL_RESULT,
            "송금 완료", "보호자가 승인해 송금이 완료되었습니다.", "TRANSACTION", TRANSACTION_ID))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("보호자 조회가 실패해도 예외를 내보내지 않는다")
    void 조회_실패도_호출_흐름을_뒤집지_않는다() {
        given(notificationMapper.findActiveGuardIds(SENIOR_ID))
            .willThrow(new RuntimeException("조회 실패"));

        assertThatCode(() -> service.notifyGuardians(SENIOR_ID, NotificationType.ANOMALY,
            "결제 차단", "위험 결제가 차단되었습니다.", "TRANSACTION", TRANSACTION_ID))
            .doesNotThrowAnyException();

        then(notificationMapper).should(never()).insert(any());
    }
}
