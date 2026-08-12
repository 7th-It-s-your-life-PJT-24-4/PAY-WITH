package com.paywith.notification.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

import com.paywith.external.fcm.PushClient;
import com.paywith.external.fcm.PushSendResult;
import com.paywith.user.mapper.UserMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("푸시 발송 처리")
class PushDispatcherTest {

    private static final List<String> TOKENS = List.of("TOKEN_A", "TOKEN_B");
    private static final Map<String, String> DATA = Map.of("type", "ANOMALY");

    @Mock
    private PushClient pushClient;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private PushDispatcher dispatcher;

    @Test
    @DisplayName("영구 실패한 토큰은 지운다 — 두면 발송할 때마다 같은 실패가 쌓인다")
    void dispatch_clearsInvalidTokens() {
        given(pushClient.send(TOKENS, "제목", "내용", DATA))
            .willReturn(new PushSendResult(1, 1, List.of("TOKEN_B")));

        dispatcher.dispatch(TOKENS, "제목", "내용", DATA);

        then(userMapper).should().clearFcmTokens(List.of("TOKEN_B"));
    }

    @Test
    @DisplayName("전부 성공하면 토큰을 건드리지 않는다")
    void dispatch_doesNotTouchTokensWhenAllSucceed() {
        given(pushClient.send(TOKENS, "제목", "내용", DATA))
            .willReturn(new PushSendResult(2, 0, List.of()));

        dispatcher.dispatch(TOKENS, "제목", "내용", DATA);

        then(userMapper).should(never()).clearFcmTokens(anyList());
    }

    @Test
    @DisplayName("발송이 통째로 실패해도 예외를 내보내지 않는다 — 비동기라 받을 곳이 없다")
    void dispatch_doesNotThrowWhenSendFails() {
        given(pushClient.send(any(), any(), any(), any()))
            .willThrow(new RuntimeException("FCM 연결 실패"));

        assertThatCode(() -> dispatcher.dispatch(TOKENS, "제목", "내용", DATA))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("토큰 정리가 실패해도 예외를 내보내지 않는다 — 발송은 이미 끝났다")
    void dispatch_doesNotThrowWhenCleanupFails() {
        given(pushClient.send(TOKENS, "제목", "내용", DATA))
            .willReturn(new PushSendResult(1, 1, List.of("TOKEN_B")));
        willThrow(new RuntimeException("DB 오류")).given(userMapper).clearFcmTokens(anyList());

        assertThatCode(() -> dispatcher.dispatch(TOKENS, "제목", "내용", DATA))
            .doesNotThrowAnyException();
    }
}
