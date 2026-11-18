import axios, {
  type AxiosError,
  type AxiosRequestConfig,
  type InternalAxiosRequestConfig,
} from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResponse, FieldErrorItem, TokenPair } from '@/types/api'
import { clearTokens, getAccessToken, getRefreshToken, setTokens } from '@/utils/token'

/**
 * 统一请求封装：
 * - 自动携带学员端 access token；
 * - 统一解包 { code, message, data }；
 * - 401 时使用 refresh token 轮换并重放请求（并发请求排队等待）；
 * - 登录态失效时清理本地令牌并跳转登录页。
 */

export class ApiError extends Error {
  constructor(
    public readonly code: number,
    message: string,
    public readonly fieldErrors?: FieldErrorItem[] | null,
    public readonly httpStatus?: number,
  ) {
    super(message)
    this.name = 'ApiError'
  }
}

export interface RequestOptions extends AxiosRequestConfig {
  /** 为 true 时不自动弹出错误提示（由页面自行处理） */
  silent?: boolean
}

interface RetryConfig extends InternalAxiosRequestConfig {
  _retry?: boolean
  silent?: boolean
}

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 30_000,
})

let refreshing = false
let refreshWaiters: Array<(token: string | null) => void> = []

async function refreshAccessToken(): Promise<string | null> {
  if (refreshing) {
    return new Promise((resolve) => refreshWaiters.push(resolve))
  }
  const refreshToken = getRefreshToken()
  if (!refreshToken) {
    return null
  }
  refreshing = true
  try {
    const { data } = await axios.post<ApiResponse<TokenPair>>(
      `${import.meta.env.VITE_API_BASE_URL}/auth/refresh`,
      null,
      { headers: { 'X-Refresh-Token': refreshToken }, timeout: 15_000 },
    )
    const tokens = data.data
    if (data.code === 0 && tokens?.accessToken) {
      setTokens(tokens)
      refreshWaiters.forEach((resolve) => resolve(tokens.accessToken))
      return tokens.accessToken
    }
    clearTokens()
    refreshWaiters.forEach((resolve) => resolve(null))
    return null
  } catch {
    clearTokens()
    refreshWaiters.forEach((resolve) => resolve(null))
    return null
  } finally {
    refreshWaiters = []
    refreshing = false
  }
}

function redirectToLogin(): void {
  const base = import.meta.env.BASE_URL
  const loginPath = `${base}login`
  if (window.location.pathname.startsWith(loginPath)) {
    return
  }
  const redirect = encodeURIComponent(window.location.pathname + window.location.search)
  window.location.href = `${loginPath}?redirect=${redirect}`
}

service.interceptors.request.use((config) => {
  const token = getAccessToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

service.interceptors.response.use(
  (response) => {
    const body = response.data as ApiResponse<unknown>
    if (body && body.code === 0) {
      return body.data as never
    }
    return Promise.reject(
      new ApiError(body?.code ?? -1, body?.message ?? '请求失败', body?.fieldErrors, response.status),
    )
  },
  async (error: AxiosError<ApiResponse<unknown>>) => {
    const config = error.config as RetryConfig | undefined
    const status = error.response?.status
    const body = error.response?.data
    const options = config as RequestOptions | undefined

    if (status === 401 && config && !config._retry && getRefreshToken()) {
      config._retry = true
      const token = await refreshAccessToken()
      if (token) {
        config.headers.Authorization = `Bearer ${token}`
        return service(config)
      }
    }

    const apiError = new ApiError(
      body?.code ?? -1,
      body?.message ?? (status ? `请求失败（${status}）` : '网络异常，请稍后重试'),
      body?.fieldErrors,
      status,
    )

    if (apiError.code === 10001) {
      clearTokens()
      redirectToLogin()
    } else if (!options?.silent) {
      ElMessage.error(apiError.message)
    }

    return Promise.reject(apiError)
  },
)

export const http = {
  get<T>(url: string, config?: RequestOptions): Promise<T> {
    return service.get(url, config) as unknown as Promise<T>
  },
  post<T>(url: string, data?: unknown, config?: RequestOptions): Promise<T> {
    return service.post(url, data, config) as unknown as Promise<T>
  },
  put<T>(url: string, data?: unknown, config?: RequestOptions): Promise<T> {
    return service.put(url, data, config) as unknown as Promise<T>
  },
  patch<T>(url: string, data?: unknown, config?: RequestOptions): Promise<T> {
    return service.patch(url, data, config) as unknown as Promise<T>
  },
  delete<T>(url: string, config?: RequestOptions): Promise<T> {
    return service.delete(url, config) as unknown as Promise<T>
  },
}

export { service as request }
