package com.austin.module.admin.controller;

import com.austin.common.model.ApiResponse;
import com.austin.module.admin.controller.response.AdminRolesResponse;
import com.austin.module.admin.domain.AdminRoleCode;
import com.austin.module.admin.service.AdminRoleService;
import jakarta.validation.constraints.Positive;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin")
public class AdminRoleController {

    private final AdminRoleService adminRoleService;

    @GetMapping("/me/roles")
    public ApiResponse<AdminRolesResponse> getMyRoles(Authentication authentication) {
        long accountId = accountId(authentication);
        return ApiResponse.success(new AdminRolesResponse(
                accountId, adminRoleService.findEffectiveRoles(accountId)));
    }

    @GetMapping("/accounts/{targetAccountId}/roles")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<AdminRolesResponse> getAssignedRoles(
            @PathVariable @Positive long targetAccountId) {
        return response(targetAccountId, adminRoleService.findAssignedRoles(targetAccountId));
    }

    @PutMapping("/accounts/{targetAccountId}/roles/{roleCode}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<AdminRolesResponse> grant(
            Authentication authentication,
            @PathVariable @Positive long targetAccountId,
            @PathVariable AdminRoleCode roleCode) {
        return response(targetAccountId, adminRoleService.grant(
                accountId(authentication), targetAccountId, roleCode));
    }

    @DeleteMapping("/accounts/{targetAccountId}/roles/{roleCode}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<AdminRolesResponse> revoke(
            Authentication authentication,
            @PathVariable @Positive long targetAccountId,
            @PathVariable AdminRoleCode roleCode) {
        return response(targetAccountId, adminRoleService.revoke(
                accountId(authentication), targetAccountId, roleCode));
    }

    private ApiResponse<AdminRolesResponse> response(long accountId, Set<AdminRoleCode> roles) {
        return ApiResponse.success(new AdminRolesResponse(accountId, roles));
    }

    private long accountId(Authentication authentication) {
        return Long.parseLong(authentication.getName());
    }
}
