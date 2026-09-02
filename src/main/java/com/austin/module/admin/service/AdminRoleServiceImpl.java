package com.austin.module.admin.service;

import com.austin.common.exception.ConflictException;
import com.austin.common.exception.ResourceNotFoundException;
import com.austin.module.account.domain.AccountStatus;
import com.austin.module.account.domain.UserAccount;
import com.austin.module.account.service.UserAccountService;
import com.austin.module.admin.domain.AccountAdminRole;
import com.austin.module.admin.domain.AdminRole;
import com.austin.module.admin.domain.AdminRoleAuditAction;
import com.austin.module.admin.domain.AdminRoleAuditLog;
import com.austin.module.admin.domain.AdminRoleCode;
import com.austin.module.admin.mapper.AccountAdminRoleMapper;
import com.austin.module.admin.mapper.AdminRoleAuditLogMapper;
import com.austin.module.admin.mapper.AdminRoleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminRoleServiceImpl implements AdminRoleService {

    private final AdminRoleMapper adminRoleMapper;
    private final AccountAdminRoleMapper accountAdminRoleMapper;
    private final AdminRoleAuditLogMapper auditLogMapper;
    private final UserAccountService userAccountService;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public Set<AdminRoleCode> findAssignedRoles(long accountId) {
        List<AdminRoleCode> codes = accountAdminRoleMapper.selectRoleCodes(accountId);
        return codes.isEmpty() ? Set.of() : Set.copyOf(codes);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<AdminRoleCode> findEffectiveRoles(long accountId) {
        Set<AdminRoleCode> roles = findAssignedRoles(accountId);
        if (!roles.contains(AdminRoleCode.SUPER_ADMIN)) {
            return roles;
        }
        return Set.copyOf(EnumSet.allOf(AdminRoleCode.class));
    }

    @Override
    @Transactional
    public Set<AdminRoleCode> grant(
            long operatorAccountId, long targetAccountId, AdminRoleCode roleCode) {
        UserAccount target = userAccountService.getById(targetAccountId);
        if (target.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new ConflictException("只能向正常账户授予后台角色");
        }
        AdminRole role = lockRole(roleCode);
        if (findAssignment(targetAccountId, role.getId()) != null) {
            return findAssignedRoles(targetAccountId);
        }
        LocalDateTime now = LocalDateTime.now(clock);
        try {
            accountAdminRoleMapper.insert(AccountAdminRole.builder()
                    .accountId(targetAccountId)
                    .roleId(role.getId())
                    .grantedBy(operatorAccountId)
                    .grantedAt(now)
                    .build());
        } catch (DuplicateKeyException exception) {
            throw new ConflictException("后台角色已发生变化，请刷新后重试", exception);
        }
        writeAudit(operatorAccountId, targetAccountId, roleCode, AdminRoleAuditAction.GRANT, now);
        return findAssignedRoles(targetAccountId);
    }

    @Override
    @Transactional
    public Set<AdminRoleCode> revoke(
            long operatorAccountId, long targetAccountId, AdminRoleCode roleCode) {
        AdminRole role = lockRole(roleCode);
        AccountAdminRole assignment = findAssignment(targetAccountId, role.getId());
        if (assignment == null) {
            return findAssignedRoles(targetAccountId);
        }
        if (roleCode == AdminRoleCode.SUPER_ADMIN) {
            long superAdminCount = accountAdminRoleMapper.selectCount(
                    new LambdaQueryWrapper<AccountAdminRole>().eq(AccountAdminRole::getRoleId, role.getId()));
            if (superAdminCount <= 1) {
                throw new ConflictException("不能撤销系统中最后一个超级管理员");
            }
        }
        if (accountAdminRoleMapper.deleteById(assignment.getId()) != 1) {
            throw new ConflictException("后台角色已发生变化，请刷新后重试");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        writeAudit(operatorAccountId, targetAccountId, roleCode, AdminRoleAuditAction.REVOKE, now);
        return findAssignedRoles(targetAccountId);
    }

    private AdminRole lockRole(AdminRoleCode roleCode) {
        AdminRole role = adminRoleMapper.lockByCode(roleCode);
        if (role == null) {
            throw new ResourceNotFoundException("后台角色不存在");
        }
        return role;
    }

    private AccountAdminRole findAssignment(long accountId, long roleId) {
        return accountAdminRoleMapper.selectOne(new LambdaQueryWrapper<AccountAdminRole>()
                .eq(AccountAdminRole::getAccountId, accountId)
                .eq(AccountAdminRole::getRoleId, roleId));
    }

    private void writeAudit(long operatorAccountId, long targetAccountId,
            AdminRoleCode roleCode, AdminRoleAuditAction action, LocalDateTime occurredAt) {
        auditLogMapper.insert(AdminRoleAuditLog.builder()
                .operatorAccountId(operatorAccountId)
                .targetAccountId(targetAccountId)
                .roleCode(roleCode)
                .action(action)
                .occurredAt(occurredAt)
                .build());
    }
}
