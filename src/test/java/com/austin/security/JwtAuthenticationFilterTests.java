package com.austin.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.austin.module.account.domain.AccountStatus;
import com.austin.module.account.domain.UserAccount;
import com.austin.module.account.service.UserAccountService;
import com.austin.module.admin.service.AdminRoleService;
import com.austin.module.auth.token.JwtService;
import com.austin.module.auth.token.TokenClaims;
import com.austin.module.auth.token.TokenStore;
import com.austin.module.auth.token.TokenType;
import jakarta.servlet.FilterChain;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtAuthenticationFilterTests {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void roleLookupFailureKeepsUserAuthenticatedWithoutAdminAuthorities() throws Exception {
        JwtService jwtService = mock(JwtService.class);
        TokenStore tokenStore = mock(TokenStore.class);
        UserAccountService userAccountService = mock(UserAccountService.class);
        AdminRoleService adminRoleService = mock(AdminRoleService.class);
        RestAuthenticationEntryPoint entryPoint = mock(RestAuthenticationEntryPoint.class);
        FilterChain filterChain = mock(FilterChain.class);
        long accountId = 900000000000000002L;
        TokenClaims claims = new TokenClaims(
                accountId, "access-id", "session-id", TokenType.ACCESS, Instant.now().plusSeconds(60));
        UserAccount account = mock(UserAccount.class);
        when(jwtService.parse("access-token", TokenType.ACCESS)).thenReturn(claims);
        when(userAccountService.getById(accountId)).thenReturn(account);
        when(account.getAccountStatus()).thenReturn(AccountStatus.ACTIVE);
        when(adminRoleService.findEffectiveRoles(accountId))
                .thenThrow(new IllegalStateException("role query failed"));
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST", "/api/v1/identity-verification");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer access-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
                jwtService, tokenStore, userAccountService, adminRoleService, entryPoint);

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName())
                .isEqualTo(Long.toString(accountId));
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities()).isEmpty();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void invalidAccessTokenStillReturnsUnauthorized() throws Exception {
        JwtService jwtService = mock(JwtService.class);
        TokenStore tokenStore = mock(TokenStore.class);
        UserAccountService userAccountService = mock(UserAccountService.class);
        AdminRoleService adminRoleService = mock(AdminRoleService.class);
        RestAuthenticationEntryPoint entryPoint = mock(RestAuthenticationEntryPoint.class);
        FilterChain filterChain = mock(FilterChain.class);
        when(jwtService.parse("invalid-token", TokenType.ACCESS))
                .thenThrow(new IllegalArgumentException("invalid token"));
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST", "/api/v1/identity-verification");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer invalid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
                jwtService, tokenStore, userAccountService, adminRoleService, entryPoint);

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(entryPoint).commence(request, response, null);
    }
}
