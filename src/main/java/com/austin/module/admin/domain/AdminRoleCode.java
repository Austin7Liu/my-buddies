package com.austin.module.admin.domain;

public enum AdminRoleCode {
    CONTENT_ADMIN,
    RISK_REVIEWER,
    SECURITY_REVIEWER,
    SUPER_ADMIN;

    public String authority() {
        return "ROLE_" + name();
    }
}
