import { http, request } from './request'
import type { PageQuery, PageResult } from '@/types/api'
import type {
  AdminUserDetail,
  AdminUserItem,
  AdminUserPayload,
  OperationLogItem,
  PermissionTree,
  RoleItem,
  RoleOption,
  SystemSettings,
} from '@/types/system'

export interface UserListQuery extends PageQuery {
  keyword?: string
  roleId?: number
  status?: number
}

export interface LogListQuery extends PageQuery {
  keyword?: string
  operationType?: string
  startDate?: string
  endDate?: string
}

export interface LogDetailQuery {
  keyword?: string
  operationType?: string
  startDate?: string
  endDate?: string
}

export const userApi = {
  list: (params: UserListQuery) =>
    http.get<PageResult<AdminUserItem>>('/users', { params }),
  detail: (id: number) => http.get<AdminUserDetail>(`/users/${id}/detail`),
  roleOptions: () => http.get<RoleOption[]>('/users/role-options'),
  create: (payload: AdminUserPayload) => http.post<void>('/users', payload),
  update: (id: number, payload: AdminUserPayload) =>
    http.put<void>(`/users/${id}`, payload),
  updateStatus: (ids: number[], status: number) =>
    http.put<void>('/users/batch-status', { ids, status }),
  remove: (id: number) => http.delete<void>(`/users/${id}`),
  batchRemove: (ids: number[]) => http.delete<void>('/users/batch', { data: { ids } }),
  resetPassword: (id: number) =>
    http.post<{ password: string }>(`/users/${id}/reset-password`),
}

export const roleApi = {
  list: () => http.get<RoleItem[]>('/roles'),
  create: (payload: { name: string; code: string; description?: string }) =>
    http.post<void>('/roles', payload),
  remove: (id: number) => http.delete<void>(`/roles/${id}`),
  tree: (roleId?: number) =>
    http.get<PermissionTree>('/permissions/tree', { params: { roleId } }),
  savePermissions: (id: number, permissionIds: number[]) =>
    http.put<void>(`/roles/${id}/permissions`, { permissionIds }),
}

export const logApi = {
  list: (params: LogListQuery) =>
    http.get<PageResult<OperationLogItem>>('/logs', { params }),
  detail: (id: number) => http.get<OperationLogItem>(`/logs/${id}`),
  exportFile: (params: LogDetailQuery) =>
    request.get('/logs/export', {
      params,
      responseType: 'blob',
    }) as unknown as Promise<Blob>,
  clear: () => http.delete<void>('/logs'),
}

export const settingApi = {
  get: () => http.get<SystemSettings>('/settings'),
  update: (payload: SystemSettings) => http.put<SystemSettings>('/settings', payload),
  reset: () => http.post<SystemSettings>('/settings/reset'),
}

export const fileApi = {
  uploadImage: (file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return http.post<{ url: string }>('/files/image', formData)
  },
}
