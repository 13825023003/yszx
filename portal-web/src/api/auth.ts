import { http } from './request'
import type { TokenPair } from '@/types/api'
import { getRefreshToken } from '@/utils/token'

/**
 * 学员端认证接口（P-A01 ~ P-A08）。
 *
 * 说明：后端 auth 模块尚未实现，以下入参/出参类型为协议占位；
 * 接口实现后应由 openapi-typescript 生成并替换（D21），不长期手工维护两份。
 */

export interface SmsCodePayload {
  mobile: string
  type: 'register' | 'login' | 'reset'
}

export interface LoginPasswordPayload {
  account: string
  password: string
}

export interface SmsLoginPayload {
  mobile: string
  code: string
}

export interface RegisterPayload {
  mobile: string
  code: string
  password: string
}

export interface ResetVerifyPayload {
  mobile: string
  code: string
}

export interface ResetPasswordPayload {
  resetToken: string
  newPassword: string
}

export interface SmsCodeResult {
  expireSeconds: number
}

export interface ResetTokenResult {
  resetToken: string
}

export const authApi = {
  sendSmsCode: (payload: SmsCodePayload) => http.post<SmsCodeResult>('/auth/sms-code', payload),
  register: (payload: RegisterPayload) => http.post<TokenPair>('/auth/register', payload),
  loginByPassword: (payload: LoginPasswordPayload) =>
    http.post<TokenPair>('/auth/login/password', payload),
  loginBySms: (payload: SmsLoginPayload) => http.post<TokenPair>('/auth/login/sms', payload),
  verifyResetCode: (payload: ResetVerifyPayload) =>
    http.post<ResetTokenResult>('/auth/reset/verify', payload),
  resetPassword: (payload: ResetPasswordPayload) => http.post<void>('/auth/reset', payload),
  logout: () =>
    http.post<void>('/auth/logout', null, {
      headers: { 'X-Refresh-Token': getRefreshToken() ?? '' },
    }),
}
