package com.yunshu.zhixue.security;

import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 双端 JWT 认证过滤器：每个安全过滤链只认自己令牌域的 token。
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider tokenProvider;
    private final TokenBlacklistService tokenBlacklistService;
    private final TokenDomain tokenDomain;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final ObjectProvider<AccountStatusValidator> accountStatusValidatorProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 登出是幂等接口：即使 access token 已过期或非法也必须成功返回，
        // 由认证服务自行解析并失效令牌，因此这里不做前置认证。
        if (request.getServletPath().endsWith("/auth/logout")) {
            filterChain.doFilter(request, response);
            return;
        }

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length()).trim();
            try {
                JwtPrincipal principal = tokenProvider.parseAccessToken(token, tokenDomain);
                if (tokenBlacklistService.isBlacklisted(principal.jti())) {
                    throw new BizException(ErrorCode.UNAUTHORIZED, "登录状态已失效");
                }
                AccountStatusValidator statusValidator =
                        accountStatusValidatorProvider.getIfAvailable();
                if (statusValidator != null
                        && !statusValidator.isActive(tokenDomain, principal.userId())) {
                    throw new BizException(ErrorCode.UNAUTHORIZED, "账号已禁用或不存在");
                }
                AuthUser authUser = new AuthUser(principal.userId(), principal.account(),
                        principal.domain());
                List<SimpleGrantedAuthority> authorities = tokenDomain == TokenDomain.ADMIN
                        ? List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                        : List.of(new SimpleGrantedAuthority("ROLE_STUDENT"));
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(authUser, null, authorities);
                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception ex) {
                SecurityContextHolder.clearContext();
                authenticationEntryPoint.commence(request, response,
                        new InsufficientAuthenticationException("登录状态无效或已过期", ex));
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
