package com.yunshu.zhixue.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 管理端 RBAC 权限点校验（API 04 §2.8）。
 *
 * <p>用法：{@code @RequirePermission("question:edit")}。
 * 权限码与 permission 表一一对应，见 API 04 附录 A。</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {

    /** 权限码，如 {@code subject:edit}。 */
    String value();
}
