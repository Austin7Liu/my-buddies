package com.austin.module.admin.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.austin.module.account.domain.UserAccount;
import com.austin.module.account.service.UserAccountService;
import com.austin.module.admin.domain.AccountAdminRole;
import com.austin.module.admin.domain.AdminRole;
import com.austin.module.admin.domain.AdminRoleAuditLog;
import com.austin.module.admin.domain.AdminRoleCode;
import com.austin.module.admin.mapper.AccountAdminRoleMapper;
import com.austin.module.admin.mapper.AdminRoleAuditLogMapper;
import com.austin.module.admin.mapper.AdminRoleMapper;
import com.austin.module.admin.service.AdminRoleService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Clock;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminRoleControllerTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserAccountService userAccountService;
    @Autowired private AdminRoleService adminRoleService;
    @Autowired private AdminRoleMapper adminRoleMapper;
    @Autowired private AccountAdminRoleMapper accountAdminRoleMapper;
    @Autowired private AdminRoleAuditLogMapper auditLogMapper;
    @Autowired private Clock clock;

    private UserAccount superAdmin;
    private UserAccount target;

    @BeforeEach
    void setUp() {
        superAdmin = userAccountService.create("13900139200");
        target = userAccountService.create("13900139201");
        bootstrapSuperAdmin(superAdmin.getId());
    }

    @Test
    void superAdminCanGrantRoleAndChangeIsAudited() throws Exception {
        mockMvc.perform(put("/api/v1/admin/accounts/{accountId}/roles/{role}",
                        target.getId(), AdminRoleCode.CONTENT_ADMIN)
                        .with(user(superAdmin.getId().toString()).roles("SUPER_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accountId").value(target.getId()))
                .andExpect(jsonPath("$.data.roles[0]").value("CONTENT_ADMIN"));

        assertThat(adminRoleService.findAssignedRoles(target.getId()))
                .containsExactly(AdminRoleCode.CONTENT_ADMIN);
        AdminRoleAuditLog audit = auditLogMapper.selectOne(
                new LambdaQueryWrapper<AdminRoleAuditLog>()
                        .eq(AdminRoleAuditLog::getTargetAccountId, target.getId()));
        assertThat(audit.getOperatorAccountId()).isEqualTo(superAdmin.getId());
        assertThat(audit.getRoleCode()).isEqualTo(AdminRoleCode.CONTENT_ADMIN);
        assertThat(audit.getAction().name()).isEqualTo("GRANT");
    }

    @Test
    void userWithoutSuperAdminRoleCannotManageRoles() throws Exception {
        mockMvc.perform(put("/api/v1/admin/accounts/{accountId}/roles/{role}",
                        target.getId(), AdminRoleCode.CONTENT_ADMIN)
                        .with(user(target.getId().toString())))
                .andExpect(status().isForbidden());
    }

    @Test
    void superAdminGetsAllEffectiveRoles() throws Exception {
        mockMvc.perform(get("/api/v1/admin/me/roles")
                        .with(user(superAdmin.getId().toString()).roles("SUPER_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roles.length()").value(4))
                .andExpect(jsonPath("$.data.roles").isArray());
    }

    @Test
    void cannotRevokeLastSuperAdmin() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/accounts/{accountId}/roles/{role}",
                        superAdmin.getId(), AdminRoleCode.SUPER_ADMIN)
                        .with(user(superAdmin.getId().toString()).roles("SUPER_ADMIN")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.message")
                        .value("不能撤销系统中最后一个超级管理员"));
    }

    @Test
    void invalidRoleCodeReturnsBadRequestInsteadOfUnauthorized() throws Exception {
        mockMvc.perform(put("/api/v1/admin/accounts/{accountId}/roles/UNKNOWN", target.getId())
                        .with(user(superAdmin.getId().toString()).roles("SUPER_ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_ARGUMENT"));
    }

    private void bootstrapSuperAdmin(long accountId) {
        AdminRole role = adminRoleMapper.selectOne(new LambdaQueryWrapper<AdminRole>()
                .eq(AdminRole::getCode, AdminRoleCode.SUPER_ADMIN));
        accountAdminRoleMapper.insert(AccountAdminRole.builder()
                .accountId(accountId)
                .roleId(role.getId())
                .grantedBy(accountId)
                .grantedAt(LocalDateTime.now(clock))
                .build());
    }
}
