package com.paywith.external.fcm;

import java.util.List;
import java.util.Map;

/**
 * 푸시 발송 포트. 자격증명이 없는 환경(로컬 개발)에서는 {@link NoOpPushClient} 가 대신 주입되므로
 * 호출 측은 구현체를 구분하지 않는다.
 *
 * <p>구현체는 예외를 던지지 않고 {@link PushSendResult} 로만 결과를 알린다. 발송 실패가 알림을
 * 남기는 호출 흐름(결제·송금)을 뒤집어서는 안 되기 때문이다. 어느 토큰이 죽었는지는 결과의
 * {@code invalidTokens} 로 돌려주고, 지우는 판단은 호출 측이 한다.
 */
public interface PushClient {

    /**
     * @param tokens 수신 기기 토큰. 비어 있으면 아무것도 하지 않고 빈 결과를 돌려준다.
     * @param data   앱이 알림을 눌렀을 때 어디로 보낼지 정하는 값(거래 ID 등). null 허용.
     */
    PushSendResult send(List<String> tokens, String title, String body, Map<String, String> data);
}
