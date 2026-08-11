package com.paywith.notification.mapper;

import com.paywith.notification.domain.Notification;
import com.paywith.notification.domain.TransferNotificationInfo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface NotificationMapper {

    int insert(Notification notification);

    /** 알림 수신자 해석 — ACTIVE 페어링 보호자만. */
    List<Long> findActiveGuardIds(@Param("seniorId") Long seniorId);

    /**
     * 발송 대상 토큰. 대상 기준은 {@link #findActiveGuardIds} 와 같고 토큰이 없는 보호자만
     * 빠진다 — 둘의 조건이 갈리면 행은 남았는데 발송은 안 되는(또는 그 반대) 이유를 추적할 수
     * 없게 된다.
     */
    List<String> findActiveGuardTokens(@Param("seniorId") Long seniorId);

    /** 피보호자 본인에게 보낼 때 쓴다(승인 결과 등). 토큰이 없으면 null. */
    String findFcmTokenByUserId(@Param("userId") Long userId);

    /**
     * 송금 알림에 필요한 정보를 한 번에 가져온다. 수신자 해석과 문구에 넣을 값을 따로 조회하면
     * 같은 거래를 두 번 읽게 된다. 거래가 없으면 null.
     */
    TransferNotificationInfo findTransferNotificationInfo(
        @Param("transactionId") Long transactionId);
}
