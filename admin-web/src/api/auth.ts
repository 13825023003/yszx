import { http } from './request'
import type { TokenPair } from '@/types/api'
import type { AdminProfile } from '@/types/system'
import { getRefreshToken } from '@/utils/token'

/**
 * 管理端认证接口（A-A01 ~ A-A04）。
 */

export interface AdminLoginPayload {
  /** 用户名或手机号 */
  account: string
  password: string
}

export const authApi = {
  login: (payload: AdminLoginPayload) => http.post<TokenPair>('/auth/login', payload),
  me: () => http.get<AdminProfile>('/auth/me'),
  logout: () =>
    http.post<void>('/auth/logout', null, {
      headers: { 'X-Refresh-Token': getRefreshToken() ?? '' },
    }),
}
