package com.paywith.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("보호자 알림 저장")
class NotificationServiceImplTest {

    private static final Long SENIOR_ID = 10L;
    private static final Long TRANSACTION_ID = 200L;

    @Mock
    private NotificationMapper notificationMapper;

    @InjectMocks
    private NotificationServiceImpl service;

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
