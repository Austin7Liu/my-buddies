package com.austin.module.auth.sms;

public interface SmsSender {
    void sendVerificationCode(String phone, String code);
}
