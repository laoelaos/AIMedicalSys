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

/**
 * 用户信息类型
 */
export interface UserInfo {
  id: number
  username: string
  realName: string
  role: string
  position?: string
  permissions?: string[]
}

/**
 * 菜单项类型
 */
export interface MenuItem {
  id: number
  name: string
  path: string
  icon?: string
  permission?: string
  sortOrder?: number
  children?: MenuItem[]
}

/**
 * 登录请求类型
 */
export interface LoginRequest {
  username: string
  password: string
}

/**
 * 登录响应类型
 */
export interface LoginResponse {
  token: string
  tokenType: string
  expiresIn: number
  user: UserInfo
}
