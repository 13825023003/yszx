package com.yunshu.zhixue.modules.system.service;

import com.yunshu.zhixue.modules.system.SystemConstants;
import com.yunshu.zhixue.modules.system.entity.AdminUser;
import com.yunshu.zhixue.modules.system.mapper.AdminUserMapper;
import com.yunshu.zhixue.security.AccountStatusValidator;
import com.yunshu.zhixue.security.TokenDomain;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 管理端账号状态校验：禁用或逻辑删除后，旧 access token 立即被拒绝。
 */
@Component
@RequiredArgsConstructor
public class SystemAccountStatusValidator implements AccountStatusValidator {

    private final AdminUserMapper adminUserMapper;

    @Override
    public boolean isActive(TokenDomain domain, Long userId) {
        if (domain != TokenDomain.ADMIN) {
            return true;
        }
        AdminUser user = adminUserMapper.selectById(userId);
        return user != null
                && Integer.valueOf(SystemConstants.STATUS_ENABLED).equals(user.getStatus());
    }
}
