import type { TokenPair } from '@/types/api'

/**
 * 管理端令牌独立存储，与学员端使用不同 key，互不干扰（D9）。
 */
const ACCESS_TOKEN_KEY = 'yszh.admin.access_token'
const REFRESH_TOKEN_KEY = 'yszh.admin.refresh_token'

export function getAccessToken(): string | null {
  return localStorage.getItem(ACCESS_TOKEN_KEY)
}

export function getRefreshToken(): string | null {
  return localStorage.getItem(REFRESH_TOKEN_KEY)
}

export function setTokens(tokens: TokenPair): void {
  localStorage.setItem(ACCESS_TOKEN_KEY, tokens.accessToken)
  localStorage.setItem(REFRESH_TOKEN_KEY, tokens.refreshToken)
}

export function clearTokens(): void {
  localStorage.removeItem(ACCESS_TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
}
