/**
 * 接口协议层类型（统一响应、分页、令牌）。
 *
 * 业务 DTO 不在此手工维护：后端接口实现后，使用 `pnpm gen:types`
 * 从 /v3/api-docs 生成到 src/types/generated/api.d.ts（D21）。
 */

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  fieldErrors?: FieldErrorItem[] | null
  fileErrors?: RowErrorItem[] | null
}

export interface FieldErrorItem {
  field: string
  reason: string
}

export interface RowErrorItem {
  row: number
  field: string
  reason: string
}

export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  pageSize: number
}

export interface PageQuery {
  page?: number
  pageSize?: number
}

export interface TokenPair {
  accessToken: string
  refreshToken: string
  accessExpiresIn: number
  refreshExpiresIn: number
}
