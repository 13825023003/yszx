/** 系统管理模块接口类型：后台用户、角色权限、操作日志、系统设置。 */

export interface AdminProfile {
  id: number
  username: string
  avatar: string | null
  roleId: number
  roleName: string
  roleCode: string
  /** 超级管理员为 ["*"] */
  permissions: string[]
}

export interface AdminUserItem {
  id: number
  username: string
  mobile: string | null
  email: string | null
  avatar: string | null
  roleId: number
  roleName: string
  roleCode: string
  status: number
  isBuiltin: number
  lastLoginAt: string | null
  createdAt: string | null
}

export interface AdminUserDetail extends AdminUserItem {
  recentLogs: OperationLogItem[]
}

export interface AdminUserPayload {
  username: string
  mobile: string
  email?: string | null
  avatar?: string | null
  roleId: number
  status: number
  password?: string
}

export interface RoleOption {
  id: number
  name: string
  code: string
  assignable: boolean
}

export interface RoleItem {
  id: number
  name: string
  code: string
  description: string | null
  userCount: number
  permissionLabels: string[]
}

export interface PermissionItem {
  id: number
  name: string
  code: string
  checked: boolean
}

export interface PermissionGroup {
  id: number
  name: string
  code: string
  permissions: PermissionItem[]
}

export interface PermissionTree {
  groups: PermissionGroup[]
  total: number
}

export interface OperationLogItem {
  id: number
  userId: number
  username: string
  action: string
  targetType: string
  targetName: string
  ip: string
  createdAt: string
}

export interface SystemSettings {
  siteName: string
  siteDescription: string
  logoUrl: string | null
  defaultAvatar: string | null
}
