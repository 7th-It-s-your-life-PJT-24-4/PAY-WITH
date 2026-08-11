package com.paywith.external.fcm;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** FCM HTTP v1 로 실제 발송한다. 빈 등록은 {@link com.paywith.config.FirebaseConfig} 가 한다. */
public class FirebasePushClient implements PushClient {

    private static final Logger log = LoggerFactory.getLogger(FirebasePushClient.class);

    /** sendEachForMulticast 한 번에 넣을 수 있는 상한. 초과하면 FCM 이 요청 전체를 거절한다. */
    private static final int MAX_TOKENS_PER_REQUEST = 500;

    private final FirebaseMessaging firebaseMessaging;

    public FirebasePushClient(FirebaseMessaging firebaseMessaging) {
        this.firebaseMessaging = firebaseMessaging;
    }

    @Override
    public PushSendResult send(List<String> tokens, String title, String body,
        Map<String, String> data) {
        if (tokens == null || tokens.isEmpty()) {
            return PushSendResult.empty();
        }

        int successCount = 0;
        int failureCount = 0;
        List<String> invalidTokens = new ArrayList<>();

        // 보호자가 500 명을 넘을 일은 없지만, 넘는 순간 조용히 전부 실패하는 종류의 한도라
        // 나눠 보낸다.
        for (int from = 0; from < tokens.size(); from += MAX_TOKENS_PER_REQUEST) {
            List<String> chunk =
                tokens.subList(from, Math.min(from + MAX_TOKENS_PER_REQUEST, tokens.size()));
            PushSendResult chunkResult = sendChunk(chunk, title, body, data);
            successCount += chunkResult.getSuccessCount();
            failureCount += chunkResult.getFailureCount();
            invalidTokens.addAll(chunkResult.getInvalidTokens());
        }

        return new PushSendResult(successCount, failureCount, invalidTokens);
    }

    private PushSendResult sendChunk(List<String> tokens, String title, String body,
        Map<String, String> data) {
        MulticastMessage.Builder message = MulticastMessage.builder()
            .addAllTokens(tokens)
            .setNotification(Notification.builder().setTitle(title).setBody(body).build());
        if (data != null && !data.isEmpty()) {
            message.putAllData(data);
        }

        BatchResponse batchResponse;
        try {
            // sendEachForMulticast 는 토큰마다 개별 요청을 보내고 결과를 모아 준다. 하나가
            // 실패해도 나머지는 나간다(구 sendMulticast 는 배치 전체가 함께 실패했다).
            batchResponse = firebaseMessaging.sendEachForMulticast(message.build());
        } catch (FirebaseMessagingException e) {
            // 요청 자체가 성립하지 않은 경우(자격증명 만료, 네트워크 등). 토큰 문제가 아니므로
            // 지우지 않고 실패로만 센다.
            log.error("FCM 발송 요청 실패. tokenCount={}, errorCode={}",
                tokens.size(), e.getMessagingErrorCode(), e);
            return new PushSendResult(0, tokens.size(), List.of());
        }

        List<String> invalidTokens = new ArrayList<>();
        List<SendResponse> responses = batchResponse.getResponses();
        for (int i = 0; i < responses.size(); i++) {
            SendResponse response = responses.get(i);
            if (response.isSuccessful()) {
                continue;
            }
            String token = tokens.get(i);
            MessagingErrorCode errorCode = errorCodeOf(response);
            if (isTokenDead(errorCode)) {
                invalidTokens.add(token);
            }
            log.warn("FCM 발송 실패. errorCode={}, tokenSuffix={}", errorCode, suffix(token));
        }

        return new PushSendResult(
            batchResponse.getSuccessCount(), batchResponse.getFailureCount(), invalidTokens);
    }

    private MessagingErrorCode errorCodeOf(SendResponse response) {
        FirebaseMessagingException exception = response.getException();
        return exception == null ? null : exception.getMessagingErrorCode();
    }

    /**
     * 다시 보내도 영영 실패하는 것들만 고른다. UNAVAILABLE·INTERNAL 같은 일시적 오류를 여기
     * 넣으면 멀쩡한 토큰을 지워 그 기기로 다시는 알림이 가지 않는다.
     */
    private boolean isTokenDead(MessagingErrorCode errorCode) {
        return errorCode == MessagingErrorCode.UNREGISTERED
            || errorCode == MessagingErrorCode.INVALID_ARGUMENT;
    }

    /** 토큰은 기기 식별자라 통째로 로그에 남기지 않는다. */
    private String suffix(String token) {
        return token.length() <= 6 ? "***" : "***" + token.substring(token.length() - 6);
    }
}
