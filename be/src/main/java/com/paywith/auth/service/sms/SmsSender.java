package com.paywith.auth.service.sms;

public interface SmsSender {

    void send(String phone, String code);
}
