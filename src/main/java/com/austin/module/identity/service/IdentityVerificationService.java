package com.austin.module.identity.service;

import com.austin.module.identity.domain.IdentityVerification;

public interface IdentityVerificationService {

    IdentityVerification findByAccountId(long accountId);

    IdentityVerification submit(long accountId, String realName, String identityNumber);
}

