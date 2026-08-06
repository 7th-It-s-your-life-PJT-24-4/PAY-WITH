package com.paywith.notification.mapper;

import com.paywith.notification.domain.Notification;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface NotificationMapper {

    int insert(Notification notification);

    /** 알림 수신자 해석 — ACTIVE 페어링 보호자만. */
    List<Long> findActiveGuardIds(@Param("seniorId") Long seniorId);
}
