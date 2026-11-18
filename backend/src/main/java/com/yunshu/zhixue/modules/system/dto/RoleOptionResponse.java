package com.yunshu.zhixue.modules.system.dto;

/**
 * 用户管理使用的角色选项（A-M09）：超级管理员角色仅用于筛选，不可分配。
 */
public record RoleOptionResponse(Long id, String name, String code, boolean assignable) {
}
