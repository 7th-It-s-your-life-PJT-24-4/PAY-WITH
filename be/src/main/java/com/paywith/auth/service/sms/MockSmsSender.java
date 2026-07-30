package com.paywith.auth.service.sms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

// Solapi 등 실제 SMS 프로바이더 연동 전까지 사용하는 목(mock) 구현체.
// 실제 연동 시 이 클래스 대신 SmsSender를 구현한 새 @Component로 교체하면 된다.
@Component
public class MockSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(MockSmsSender.class);

    @Override
    public void send(String phone, String code) {
        log.info("[MOCK SMS] {} 인증번호: {}", phone, code);
    }
}
