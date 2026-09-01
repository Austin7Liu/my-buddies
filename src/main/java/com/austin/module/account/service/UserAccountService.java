package com.austin.module.account.service;

import com.austin.module.account.domain.UserAccount;

public interface UserAccountService {

    UserAccount create(String phone);

    UserAccount findOrCreateByPhone(String phone);

    UserAccount getById(long accountId);

    UserAccount requestCancellation(long accountId);

    UserAccount revokeCancellation(long accountId);

    UserAccount completeCancellation(long accountId);
}
