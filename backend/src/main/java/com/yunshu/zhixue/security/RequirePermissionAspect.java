package com.yunshu.zhixue.security;

import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * {@link RequirePermission} 切面：按当前管理员的权限码集合校验接口访问权。
 */
@Aspect
@Component
@RequiredArgsConstructor
public class RequirePermissionAspect {

    private final ObjectProvider<PermissionProvider> permissionProviderProvider;

    @Around("@annotation(requirePermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint,
                                  RequirePermission requirePermission) throws Throwable {
        AuthUser user = SecurityUtils.currentUser()
                .orElseThrow(() -> new BizException(ErrorCode.UNAUTHORIZED));
        if (user.domain() != TokenDomain.ADMIN) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        PermissionProvider provider = permissionProviderProvider.getIfAvailable();
        Set<String> permissions = provider == null
                ? Set.of() : provider.getPermissions(user.id());
        if (!permissions.contains("*")
                && !permissions.contains(requirePermission.value())) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权限执行该操作");
        }
        return joinPoint.proceed();
    }
}
