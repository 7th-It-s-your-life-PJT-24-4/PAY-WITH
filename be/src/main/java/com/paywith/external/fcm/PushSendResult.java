package com.paywith.external.fcm;

import java.util.Collections;
import java.util.List;

/**
 * 발송 결과. 성공 건수는 로그·지표용이고, 실제로 후속 처리가 필요한 값은 {@code invalidTokens} 다.
 *
 * <p>FCM 은 토큰별로 개별 응답을 주므로 일부만 실패할 수 있다. 그래서 성패를 boolean 하나로
 * 접지 않는다.
 */
public class PushSendResult {

    private static final PushSendResult EMPTY = new PushSendResult(0, 0, Collections.emptyList());

    private final int successCount;
    private final int failureCount;

    /**
     * 기기에서 앱이 지워졌거나(UNREGISTERED) 토큰 형식이 깨진(INVALID_ARGUMENT) 것들. 계속 두면
     * 발송할 때마다 실패하므로 호출 측이 users.fcm_token 에서 지운다.
     */
    private final List<String> invalidTokens;

    public PushSendResult(int successCount, int failureCount, List<String> invalidTokens) {
        this.successCount = successCount;
        this.failureCount = failureCount;
        this.invalidTokens = invalidTokens;
    }

    public static PushSendResult empty() {
        return EMPTY;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public int getFailureCount() {
        return failureCount;
    }

    public List<String> getInvalidTokens() {
        return invalidTokens;
    }
}
