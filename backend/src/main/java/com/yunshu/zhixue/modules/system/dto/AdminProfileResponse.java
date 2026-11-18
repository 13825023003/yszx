package com.yunshu.zhixue.modules.system.dto;

import java.util.List;

/**
 * 当前登录管理员信息（A-A04）：供前端菜单与按钮按权限码渲染。
 *
 * @param permissions 权限码集合；超级管理员返回 {@code ["*"]}
 */
public record AdminProfileResponse(Long id,
                                   String username,
                                   String avatar,
                                   Long roleId,
                                   String roleName,
                                   String roleCode,
                                   List<String> permissions) {
}
