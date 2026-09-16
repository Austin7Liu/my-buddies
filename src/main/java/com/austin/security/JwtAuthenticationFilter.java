package com.austin.security;

import com.austin.module.auth.token.JwtService;
import com.austin.module.auth.token.TokenClaims;
import com.austin.module.auth.token.TokenStore;
import com.austin.module.auth.token.TokenType;
import com.austin.module.account.domain.AccountStatus;
import com.austin.module.account.domain.UserAccount;
import com.austin.module.account.service.UserAccountService;
import com.austin.module.admin.service.AdminRoleService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final TokenStore tokenStore;
    private final UserAccountService userAccountService;
    private final AdminRoleService adminRoleService;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            TokenClaims claims = jwtService.parse(authorization.substring(7), TokenType.ACCESS);
            if (tokenStore.isAccessTokenBlacklisted(claims.tokenId())
                    || tokenStore.isSessionRevoked(claims.sessionId())) {
                throw new IllegalArgumentException("访问令牌已注销");
            }
            UserAccount account = userAccountService.getById(claims.accountId());
            if (account.getAccountStatus() == AccountStatus.BANNED
                    || account.getAccountStatus() == AccountStatus.CANCELLED) {
                throw new IllegalArgumentException("账户不可用");
            }
            List<SimpleGrantedAuthority> authorities = loadAuthorities(claims.accountId(), request);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            Long.toString(claims.accountId()), null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (RuntimeException exception) {
            log.debug("Access authentication failed. path={}, exceptionType={}",
                    request.getRequestURI(), exception.getClass().getName());
            SecurityContextHolder.clearContext();
            authenticationEntryPoint.commence(request, response, null);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private List<SimpleGrantedAuthority> loadAuthorities(long accountId, HttpServletRequest request) {
        try {
            return adminRoleService.findEffectiveRoles(accountId)
                    .stream()
                    .map(role -> new SimpleGrantedAuthority(role.authority()))
                    .toList();
        } catch (RuntimeException exception) {
            log.error("Failed to load admin roles; continuing without admin authorities. accountId={}, path={}",
                    accountId, request.getRequestURI(), exception);
            return List.of();
        }
    }
}
