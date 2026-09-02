package com.austin.module.admin.controller.response;

import com.austin.module.admin.domain.AdminRoleCode;
import java.util.Set;

public record AdminRolesResponse(long accountId, Set<AdminRoleCode> roles) {
}
