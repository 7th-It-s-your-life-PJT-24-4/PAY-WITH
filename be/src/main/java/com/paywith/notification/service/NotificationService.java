package com.paywith.notification.service;

import com.paywith.notification.domain.NotificationType;

/**
 * 보호자 알림 저장. notifications 행 INSERT 까지만 담당한다 — 발송 채널(FCM 등)은 미도입이라
 * 커밋 이후 단계로 남아 있고, 조회(알림함 API)는 이 인프라의 범위 밖이다.
 */
public interface NotificationService {

    /**
     * 피보호자의 ACTIVE 보호자 전원에게 같은 내용의 알림 행을 남긴다 — 읽음 여부(is_read)는
     * 보호자별로 관리해야 하므로 보호자 수만큼 행이 생긴다.
     */
    void notifyGuardians(Long seniorId, NotificationType type, String title, String body,
        String refType, Long refId);
}
