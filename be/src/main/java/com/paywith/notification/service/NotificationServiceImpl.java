package com.paywith.notification.service;

import com.paywith.notification.domain.Notification;
import com.paywith.notification.domain.NotificationType;
import com.paywith.notification.mapper.NotificationMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationMapper notificationMapper;
    private final PushDispatcher pushDispatcher;

    public NotificationServiceImpl(NotificationMapper notificationMapper,
        PushDispatcher pushDispatcher) {
        this.notificationMapper = notificationMapper;
        this.pushDispatcher = pushDispatcher;
    }

    /**
     * 알림 행은 호출자의 트랜잭션에 참여시킨다 — 호출 흐름(결제 등)이 롤백되면 알림도 함께
     * 사라져야 하기 때문이다. 반대로 저장 실패는 호출 흐름을 뒤집지 않고 로그만 남기며,
     * 호출자가 아니라 여기서 잡는 이유는 PaymentFdsResultServiceImpl 과 같다: 예외가
     * @Transactional 경계를 넘는 순간 진행 중인 트랜잭션이 rollback-only 로 표시된다.
     */
    @Override
    @Transactional
    public void notifyGuardians(Long seniorId, NotificationType type, String title, String body,
        String refType, Long refId) {
        try {
            List<Long> guardIds = notificationMapper.findActiveGuardIds(seniorId);
            for (Long guardId : guardIds) {
                insert(guardId, type, title, body, refType, refId);
            }
            sendAfterCommit(notificationMapper.findActiveGuardTokens(seniorId),
                type, title, body, refType, refId, seniorId);
        } catch (RuntimeException e) {
            log.error("보호자 알림 저장 실패 — 호출 흐름은 유지. seniorId={}, type={}", seniorId, type, e);
        }
    }

    /** 실패를 삼키는 이유는 {@link #notifyGuardians} 와 같다. */
    @Override
    @Transactional
    public void notifyUser(Long userId, NotificationType type, String title, String body,
        String refType, Long refId) {
        try {
            insert(userId, type, title, body, refType, refId);

            String token = notificationMapper.findFcmTokenByUserId(userId);
            sendAfterCommit(token == null ? List.of() : List.of(token),
                type, title, body, refType, refId, null);
        } catch (RuntimeException e) {
            log.error("알림 저장 실패 — 호출 흐름은 유지. userId={}, type={}", userId, type, e);
        }
    }

    /**
     * 발송은 반드시 커밋 이후여야 한다. 트랜잭션 안에서 보내면 롤백된 거래에도 푸시가 나가고,
     * 사용자는 일어나지 않은 일을 알림으로 받는다. 되돌릴 방법도 없다.
     *
     * <p>{@code afterCommit} 안에서는 발송을 스레드풀에 넘기기만 한다 — 이 콜백은 아직 요청
     * 스레드 위에서 돌기 때문에, 여기서 FCM 응답을 기다리면 트랜잭션만 짧아지고 응답 시간은
     * 그대로다.
     */
    private void sendAfterCommit(List<String> tokens, NotificationType type, String title,
        String body, String refType, Long refId, Long seniorId) {
        if (tokens.isEmpty()) {
            return;
        }
        Map<String, String> data = buildData(type, refType, refId, seniorId);

        // 트랜잭션 없이 불린 경우(스케줄러 등 호출 경로가 바뀌었을 때) 그대로 보낸다. 되돌릴
        // 트랜잭션이 없으니 커밋을 기다릴 것도 없다.
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            pushDispatcher.dispatch(tokens, title, body, data);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                pushDispatcher.dispatch(tokens, title, body, data);
            }
        });
    }

    /** 앱이 알림을 눌렀을 때 어디로 보낼지 정하는 값. 값이 없는 키는 넣지 않는다. */
    private Map<String, String> buildData(NotificationType type, String refType, Long refId,
        Long seniorId) {
        Map<String, String> data = new HashMap<>();
        data.put("type", type.name());
        if (refType != null) {
            data.put("refType", refType);
        }
        if (refId != null) {
            data.put("refId", String.valueOf(refId));
        }
        // 보호자 거래 상세 API는 거래 ID와 피보호자 ID를 함께 요구한다. ANOMALY 푸시가
        // 앱 콜드 스타트에서도 Pinia 선택 상태 없이 상세 화면을 열 수 있도록 같이 보낸다.
        if (type == NotificationType.ANOMALY && seniorId != null) {
            data.put("wardId", String.valueOf(seniorId));
        }
        return data;
    }

    private void insert(Long userId, NotificationType type, String title, String body,
        String refType, Long refId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setBody(body);
        notification.setRefType(refType);
        notification.setRefId(refId);
        notificationMapper.insert(notification);
    }
}
