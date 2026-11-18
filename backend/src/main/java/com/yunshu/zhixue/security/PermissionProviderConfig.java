package com.yunshu.zhixue.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

/**
 * 权限提供者兜底配置。
 *
 * <p>system 模块实现 {@link PermissionProvider} 后，本兜底 Bean 会自动失效。
 * 在实现之前，所有带 {@code @RequirePermission} 的接口都会按「无权限」拒绝（fail-closed）。</p>
 */
@Slf4j
@Configuration
public class PermissionProviderConfig {

    @Bean
    @ConditionalOnMissingBean(PermissionProvider.class)
    public PermissionProvider defaultPermissionProvider() {
        log.warn("尚未实现 PermissionProvider，管理端权限点校验将全部拒绝；"
                + "请在 system 模块提供实现。");
        return adminUserId -> Set.of();
    }
}
