package com.paywith.external.fcm;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 자격증명이 없는 환경에서 쓰는 무동작 구현.
 *
 * <p>로컬 개발자마다 Firebase 서비스 계정 키를 받아둘 수는 없으므로, 키가 없으면 기동이 깨지는
 * 대신 이쪽이 주입된다. 알림 행 INSERT 는 그대로 일어나므로 발송만 빠지고 나머지 흐름은 같다.
 *
 * <p>토큰을 무효로 보고하지 않는다. 여기서 지워버리면 로컬에서 한 번 돌린 것만으로 실제 기기
 * 토큰이 사라진다.
 */
public class NoOpPushClient implements PushClient {

    private static final Logger log = LoggerFactory.getLogger(NoOpPushClient.class);

    @Override
    public PushSendResult send(List<String> tokens, String title, String body,
        Map<String, String> data) {
        int count = tokens == null ? 0 : tokens.size();
        log.info("[FCM 미설정] 발송 생략. tokenCount={}, title={}", count, title);
        return new PushSendResult(0, 0, List.of());
    }
}
