/** 后端统一响应结构（04.API接口设计 §2.1） */
export interface ApiResponse<T = unknown> {
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
  field?: string
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

/** 双令牌响应（access 2h / refresh 14d） */
export interface TokenPair {
  accessToken: string
  refreshToken: string
  accessExpiresIn: number
  refreshExpiresIn: number
}
