export type ApiSuccess<T> = { code: 'SUCCESS'; data: T }

export type ApiError = {
  code: 'NETWORK_ERROR' | 'UNAUTHORIZED' | 'FORBIDDEN' | 'HTTP_ERROR'
  message: string
}

export type ApiResponse<T> = ApiSuccess<T> | ApiError

export interface ApiResult<T = unknown> {
  code: string
  message?: string
  data?: T
}

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  page: number
  size: number
}

export type BusinessError = {
  code: string
  message: string
  isBusinessError?: true
}
