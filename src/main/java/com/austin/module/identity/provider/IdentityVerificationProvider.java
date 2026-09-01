package com.austin.module.identity.provider;

public interface IdentityVerificationProvider {

    IdentityProviderResult verify(IdentityVerificationCommand command);
}

