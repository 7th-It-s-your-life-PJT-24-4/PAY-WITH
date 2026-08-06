package com.paywith.notification.domain;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Notification {

    private Long notificationId;
    private Long userId;
    private NotificationType type;
    private String title;
    private String body;
    private String refType;
    private Long refId;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
