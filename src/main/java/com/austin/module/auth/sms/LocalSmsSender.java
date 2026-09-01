package com.austin.module.auth.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile({"dev", "test"})
public class LocalSmsSender implements SmsSender {

    @Override
    public void sendVerificationCode(String phone, String code) {
        log.warn("[DEV ONLY] verification code for {} is {}", maskPhone(phone), code);
    }

    private String maskPhone(String phone) {
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}
