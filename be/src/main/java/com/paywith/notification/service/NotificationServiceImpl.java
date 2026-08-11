package com.paywith.notification.service;

import com.paywith.notification.domain.Notification;
import com.paywith.notification.domain.NotificationType;
import com.paywith.notification.mapper.NotificationMapper;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationMapper notificationMapper;

    public NotificationServiceImpl(NotificationMapper notificationMapper) {
        this.notificationMapper = notificationMapper;
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
        } catch (RuntimeException e) {
            log.error("알림 저장 실패 — 호출 흐름은 유지. userId={}, type={}", userId, type, e);
        }
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
