package com.yunshu.zhixue.modules.system.bootstrap;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yunshu.zhixue.modules.system.entity.AdminUser;
import com.yunshu.zhixue.modules.system.entity.Role;
import com.yunshu.zhixue.modules.system.mapper.AdminUserMapper;
import com.yunshu.zhixue.modules.system.mapper.RoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 内置超级管理员初始化（03 数据库设计 §7、需求 4.5.1）。
 *
 * <p>schema.sql 不保存管理员明文密码或固定哈希。应用启动时若不存在内置管理员，
 * 使用 {@code app.default-password} 生成 BCrypt 哈希并创建 {@code admin} 账号；
 * 已存在时不做任何修改，避免覆盖生产密码。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAccountInitializer implements ApplicationRunner {

    private static final String BUILTIN_USERNAME = "admin";
    private static final String SUPER_ADMIN_CODE = "super_admin";
    private static final int STATUS_ENABLED = 1;
    private static final int BUILTIN_YES = 1;

    private final AdminUserMapper adminUserMapper;
    private final RoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.default-password}")
    private String defaultPassword;

    @Override
    public void run(ApplicationArguments args) {
        Long existing = adminUserMapper.selectCount(Wrappers.<AdminUser>lambdaQuery()
                .eq(AdminUser::getIsBuiltin, BUILTIN_YES));
        if (existing != null && existing > 0) {
            return;
        }

        Role superAdmin = roleMapper.selectOne(Wrappers.<Role>lambdaQuery()
                .eq(Role::getCode, SUPER_ADMIN_CODE)
                .last("LIMIT 1"));
        if (superAdmin == null) {
            log.warn("未找到 super_admin 角色，跳过内置管理员初始化，请确认 schema.sql 种子数据已执行");
            return;
        }

        AdminUser admin = new AdminUser();
        admin.setUsername(BUILTIN_USERNAME);
        admin.setPasswordHash(passwordEncoder.encode(defaultPassword));
        admin.setRoleId(superAdmin.getId());
        admin.setStatus(STATUS_ENABLED);
        admin.setIsBuiltin(BUILTIN_YES);
        adminUserMapper.insert(admin);

        log.info("已初始化内置超级管理员账号 admin，密码取自 app.default-password");
    }
}
