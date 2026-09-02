package com.austin.module.admin.service;

import com.austin.module.admin.domain.AdminRoleCode;
import java.util.Set;

public interface AdminRoleService {

    Set<AdminRoleCode> findAssignedRoles(long accountId);

    Set<AdminRoleCode> findEffectiveRoles(long accountId);

    Set<AdminRoleCode> grant(long operatorAccountId, long targetAccountId, AdminRoleCode roleCode);

    Set<AdminRoleCode> revoke(long operatorAccountId, long targetAccountId, AdminRoleCode roleCode);
}
