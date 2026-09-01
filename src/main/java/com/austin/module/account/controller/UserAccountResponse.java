package com.austin.module.account.controller;

import com.austin.module.account.domain.AccountStatus;
import com.austin.module.account.domain.UserAccount;
import java.time.LocalDateTime;

public record UserAccountResponse(
        Long id,
        String maskedPhone,
        AccountStatus accountStatus,
        LocalDateTime cancelRequestedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static UserAccountResponse from(UserAccount account) {
        return new UserAccountResponse(
                account.getId(),
                maskPhone(account.getPhone()),
                account.getAccountStatus(),
                account.getCancelRequestedAt(),
                account.getCreatedAt(),
                account.getUpdatedAt());
    }

    private static String maskPhone(String phone) {
        if (phone == null) {
            return null;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}
