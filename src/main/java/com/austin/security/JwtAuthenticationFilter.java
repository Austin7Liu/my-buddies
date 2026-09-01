package com.austin.security;

import com.austin.module.auth.token.JwtService;
import com.austin.module.auth.token.TokenClaims;
import com.austin.module.auth.token.TokenStore;
import com.austin.module.auth.token.TokenType;
import com.austin.module.account.domain.AccountStatus;
import com.austin.module.account.domain.UserAccount;
import com.austin.module.account.service.UserAccountService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final TokenStore tokenStore;
    private final UserAccountService userAccountService;
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
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            Long.toString(claims.accountId()), null, List.of());
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (RuntimeException exception) {
            SecurityContextHolder.clearContext();
            authenticationEntryPoint.commence(request, response, null);
            return;
        }
        filterChain.doFilter(request, response);
    }
}
