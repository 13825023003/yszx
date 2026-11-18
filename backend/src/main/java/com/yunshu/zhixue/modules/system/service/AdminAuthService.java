package com.yunshu.zhixue.modules.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yunshu.zhixue.common.constant.AppConstants;
import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import com.yunshu.zhixue.modules.system.SystemConstants;
import com.yunshu.zhixue.modules.system.dto.AdminLoginRequest;
import com.yunshu.zhixue.modules.system.dto.AdminProfileResponse;
import com.yunshu.zhixue.modules.system.dto.TokenPairResponse;
import com.yunshu.zhixue.modules.system.entity.AdminUser;
import com.yunshu.zhixue.modules.system.entity.Role;
import com.yunshu.zhixue.modules.system.mapper.AdminUserMapper;
import com.yunshu.zhixue.modules.system.mapper.RoleMapper;
import com.yunshu.zhixue.security.JwtPrincipal;
import com.yunshu.zhixue.security.JwtTokenProvider;
import com.yunshu.zhixue.security.RefreshTokenStore;
import com.yunshu.zhixue.security.SecurityUtils;
import com.yunshu.zhixue.security.TokenBlacklistService;
import com.yunshu.zhixue.security.TokenDomain;
import com.yunshu.zhixue.security.TokenPair;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * 管理端认证服务（A-A01 ~ A-A03）。
 *
 * <p>失败计数与令牌快照存于 Redis；账号数据由系统管理模块维护。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private static final String LOGIN_FAIL_KEY_PREFIX = "login:fail:";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final int STATUS_ENABLED = 1;

    private final AdminUserMapper adminUserMapper;
    private final RoleMapper roleMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenStore refreshTokenStore;
    private final TokenBlacklistService tokenBlacklistService;
    private final StringRedisTemplate stringRedisTemplate;
    private final PasswordEncoder passwordEncoder;
    private final AdminPermissionService adminPermissionService;
    private final OperationLogService operationLogService;

    public TokenPairResponse login(AdminLoginRequest request) {
        String account = request.account().trim();
        String failKey = LOGIN_FAIL_KEY_PREFIX + account;

        if (currentFailures(failKey) >= AppConstants.LOGIN_FAIL_LIMIT) {
            throw new BizException(ErrorCode.ADMIN_ACCOUNT_LOCKED);
        }

        AdminUser user = findByAccount(account);
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            recordFailure(failKey);
            throw new BizException(ErrorCode.ADMIN_LOGIN_FAILED);
        }
        if (!Integer.valueOf(STATUS_ENABLED).equals(user.getStatus())) {
            throw new BizException(ErrorCode.ADMIN_ACCOUNT_DISABLED);
        }

        stringRedisTemplate.delete(failKey);

        TokenPair pair = jwtTokenProvider.createTokenPair(
                TokenDomain.ADMIN, user.getId(), user.getUsername());
        refreshTokenStore.save(TokenDomain.ADMIN, user.getId(), pair.refreshJti(),
                jwtTokenProvider.refreshTokenTtl(TokenDomain.ADMIN));

        AdminUser lastLogin = new AdminUser();
        lastLogin.setId(user.getId());
        lastLogin.setLastLoginAt(LocalDateTime.now(ZoneOffset.UTC));
        adminUserMapper.updateById(lastLogin);
        operationLogService.record(user.getId(), user.getUsername(),
                SystemConstants.ACTION_LOGIN, "认证", "后台登录");

        return TokenPairResponse.from(pair);
    }

    /**
     * 当前管理员信息（A-A04）：角色与权限码供前端菜单、按钮渲染。
     */
    public AdminProfileResponse me() {
        Long userId = SecurityUtils.requireUserId();
        AdminUser user = adminUserMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        Role role = roleMapper.selectById(user.getRoleId());
        return new AdminProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getAvatar(),
                user.getRoleId(),
                role == null ? "未知角色" : role.getName(),
                role == null ? "" : role.getCode(),
                adminPermissionService.getPermissionCodes(userId).stream().sorted().toList());
    }

    /**
     * 轮换刷新令牌：旧 refreshToken 校验通过后立即失效，并签发新双令牌。
     */
    public TokenPairResponse refresh(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        JwtPrincipal principal = jwtTokenProvider.parseRefreshToken(refreshToken, TokenDomain.ADMIN);
        if (!refreshTokenStore.isActive(TokenDomain.ADMIN, principal.userId(), principal.jti())) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        AdminUser user = adminUserMapper.selectById(principal.userId());
        if (user == null || !Integer.valueOf(STATUS_ENABLED).equals(user.getStatus())) {
            refreshTokenStore.revokeAll(TokenDomain.ADMIN, principal.userId());
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        refreshTokenStore.revoke(TokenDomain.ADMIN, principal.userId(), principal.jti());

        TokenPair pair = jwtTokenProvider.createTokenPair(
                TokenDomain.ADMIN, user.getId(), user.getUsername());
        refreshTokenStore.save(TokenDomain.ADMIN, user.getId(), pair.refreshJti(),
                jwtTokenProvider.refreshTokenTtl(TokenDomain.ADMIN));
        return TokenPairResponse.from(pair);
    }

    /**
     * 登出（幂等）：access jti 进黑名单，refresh 快照失效；令牌缺失或已失效均返回成功。
     */
    public void logout(String authorizationHeader, String refreshToken) {
        Long logUserId = null;
        String logAccount = null;

        String accessToken = stripBearer(authorizationHeader);
        if (StringUtils.hasText(accessToken)) {
            try {
                JwtPrincipal principal = jwtTokenProvider.parseAccessToken(
                        accessToken, TokenDomain.ADMIN);
                tokenBlacklistService.blacklist(principal.jti(), principal.expiresAt());
                logUserId = principal.userId();
                logAccount = principal.account();
            } catch (RuntimeException ex) {
                log.debug("登出时 access token 已失效，忽略: {}", ex.getMessage());
            }
        }
        if (StringUtils.hasText(refreshToken)) {
            try {
                JwtPrincipal principal = jwtTokenProvider.parseRefreshToken(
                        refreshToken, TokenDomain.ADMIN);
                refreshTokenStore.revoke(TokenDomain.ADMIN, principal.userId(), principal.jti());
                if (logUserId == null) {
                    logUserId = principal.userId();
                    logAccount = principal.account();
                }
            } catch (RuntimeException ex) {
                log.debug("登出时 refresh token 已失效，忽略: {}", ex.getMessage());
            }
        }
        if (logUserId != null) {
            operationLogService.record(logUserId, logAccount,
                    SystemConstants.ACTION_LOGOUT, "认证", "后台登出");
        }
    }

    private AdminUser findByAccount(String account) {
        AdminUser user = adminUserMapper.selectOne(Wrappers.<AdminUser>lambdaQuery()
                .eq(AdminUser::getUsername, account)
                .last("LIMIT 1"));
        if (user != null) {
            return user;
        }
        return adminUserMapper.selectOne(Wrappers.<AdminUser>lambdaQuery()
                .eq(AdminUser::getMobile, account)
                .last("LIMIT 1"));
    }

    private int currentFailures(String failKey) {
        String value = stringRedisTemplate.opsForValue().get(failKey);
        if (!StringUtils.hasText(value)) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            stringRedisTemplate.delete(failKey);
            return 0;
        }
    }

    private void recordFailure(String failKey) {
        Long failures = stringRedisTemplate.opsForValue().increment(failKey);
        Duration ttl = AppConstants.LOGIN_LOCK_DURATION;
        if (failures != null && failures >= AppConstants.LOGIN_FAIL_LIMIT) {
            ttl = AppConstants.LOGIN_LOCK_DURATION;
        }
        stringRedisTemplate.expire(failKey, ttl);
    }

    private String stripBearer(String authorizationHeader) {
        if (!StringUtils.hasText(authorizationHeader)
                || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return authorizationHeader.substring(BEARER_PREFIX.length()).trim();
    }
}
