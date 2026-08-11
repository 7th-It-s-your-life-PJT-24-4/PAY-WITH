package com.paywith.notification.service;

import com.paywith.external.fcm.PushClient;
import com.paywith.external.fcm.PushSendResult;
import com.paywith.user.mapper.UserMapper;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 실제 발송을 별도 스레드에서 처리한다.
 *
 * <p>{@link NotificationServiceImpl} 안의 메서드로 두지 않고 빈을 나눈 이유는 {@code @Async} 가
 * 프록시로 동작하기 때문이다. 같은 클래스 안에서 부르면 프록시를 거치지 않아 그냥 동기 호출이
 * 되고, 그러면 FCM 응답을 기다리는 동안 결제·송금 요청이 붙잡힌다.
 */
@Component
public class PushDispatcher {

    private static final Logger log = LoggerFactory.getLogger(PushDispatcher.class);

    private final PushClient pushClient;
    private final UserMapper userMapper;

    public PushDispatcher(PushClient pushClient, UserMapper userMapper) {
        this.pushClient = pushClient;
        this.userMapper = userMapper;
    }

    /**
     * 예외를 밖으로 내보내지 않는다. 별도 스레드라 어차피 호출 측이 받을 수 없고, 여기서 던지면
     * 잡히지 않은 채 스레드풀 로그로만 남는다.
     */
    @Async("pushTaskExecutor")
    public void dispatch(List<String> tokens, String title, String body, Map<String, String> data) {
        try {
            PushSendResult result = pushClient.send(tokens, title, body, data);
            clearInvalidTokens(result.getInvalidTokens());

            if (result.getFailureCount() > 0) {
                log.warn("푸시 발송 일부 실패. 성공={}, 실패={}",
                    result.getSuccessCount(), result.getFailureCount());
            }
        } catch (RuntimeException e) {
            log.error("푸시 발송 처리 중 오류. tokenCount={}, title={}", tokens.size(), title, e);
        }
    }

    /**
     * 다시 보내도 실패할 토큰을 지운다. 안 지우면 발송할 때마다 같은 토큰에서 실패가 쌓인다.
     *
     * <p>여기엔 트랜잭션이 없다. 비동기 스레드라 호출 측 트랜잭션이 이어지지 않고, mybatis-spring
     * 은 트랜잭션 밖의 문장을 실행 후 커밋한다. UPDATE 한 건이라 묶을 것도 없다.
     */
    private void clearInvalidTokens(List<String> invalidTokens) {
        if (invalidTokens.isEmpty()) {
            return;
        }
        try {
            int cleared = userMapper.clearFcmTokens(invalidTokens);
            log.info("만료된 FCM 토큰 정리. 대상={}, 삭제={}", invalidTokens.size(), cleared);
        } catch (RuntimeException e) {
            // 정리에 실패해도 발송 자체는 끝난 뒤다. 다음 발송에서 다시 시도된다.
            log.error("만료된 FCM 토큰 정리 실패. count={}", invalidTokens.size(), e);
        }
    }
}
