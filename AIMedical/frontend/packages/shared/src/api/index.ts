import axios, { AxiosRequestConfig, AxiosResponse } from 'axios'
import type { BusinessError, ApiResult, LoginRequest, LoginResponse, UserInfo, MenuItem } from '../types'

const apiClient = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' },
})

/**
 * 将业务错误统一包装为 BusinessError 对象
 */
function toBusinessError(code: string, message: string): BusinessError {
  return { code, message, isBusinessError: true }
}

/**
 * 响应拦截器
 *
 * <p>统一 Promise 状态约定：
 * - HTTP 层成功（2xx）：检查业务 code，SUCCESS 时返回数据，业务错误时 reject(BusinessError)
 * - HTTP 层失败（非 2xx）：统一 reject(BusinessError)
 *
 * <p>上层 apiGet/apiPost/apiPut/apiDelete 通过 try-catch 捕获 BusinessError 并转为返回值，
 * 从而对外暴露统一的 `Promise<T | BusinessError>` 接口。
 */
apiClient.interceptors.response.use(
  (response: AxiosResponse) => {
    const body = response.data as ApiResult<unknown>
    if (body.code !== 'SUCCESS') {
      const error = toBusinessError(body.code, body.message ?? '')
      return Promise.reject(error)
    }
    response.data = body.data
    return response
  },
  (error) => {
    if (error.response === undefined) {
      return Promise.reject(toBusinessError('NETWORK_ERROR', '网络不可达，请检查网络连接'))
    }
    const status = error.response.status
    if (status === 401) {
      return Promise.reject(toBusinessError('UNAUTHORIZED', '登录已过期，请重新登录'))
    }
    if (status === 403) {
      return Promise.reject(toBusinessError('FORBIDDEN', '无权限访问'))
    }
    // 后端返回的业务错误体（GlobalExceptionHandler 返回 Result JSON）
    const body = error.response.data as ApiResult<unknown> | undefined
    if (body && body.code) {
      return Promise.reject(toBusinessError(body.code, body.message ?? `请求失败（${status}）`))
    }
    return Promise.reject(toBusinessError('HTTP_ERROR', `请求失败（${status}）`))
  },
)

export async function apiGet<T>(url: string, config?: AxiosRequestConfig): Promise<T | BusinessError> {
  try {
    const response = await apiClient.get<unknown>(url, config)
    return (response as AxiosResponse<T>).data
  } catch (error) {
    return error as BusinessError
  }
}

export async function apiPost<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T | BusinessError> {
  try {
    const response = await apiClient.post<unknown>(url, data, config)
    return (response as AxiosResponse<T>).data
  } catch (error) {
    return error as BusinessError
  }
}

export async function apiPut<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T | BusinessError> {
  try {
    const response = await apiClient.put<unknown>(url, data, config)
    return (response as AxiosResponse<T>).data
  } catch (error) {
    return error as BusinessError
  }
}

export async function apiDelete<T>(url: string, config?: AxiosRequestConfig): Promise<T | BusinessError> {
  try {
    const response = await apiClient.delete<unknown>(url, config)
    return (response as AxiosResponse<T>).data
  } catch (error) {
    return error as BusinessError
  }
}

export { apiClient }

/**
 * 设置认证令牌
 */
export function setAuthToken(token: string): void {
  apiClient.defaults.headers.common['Authorization'] = `Bearer ${token}`
}

/**
 * 清除认证令牌
 */
export function clearAuthToken(): void {
  delete apiClient.defaults.headers.common['Authorization']
}

/**
 * 认证相关API
 */
export const authApi = {
  /**
   * 用户登录
   */
  login: (request: LoginRequest): Promise<LoginResponse | BusinessError> => {
    return apiPost<LoginResponse>('/auth/login', request)
  },

  /**
   * 用户登出
   */
  logout: (): Promise<void | BusinessError> => {
    return apiPost<void>('/auth/logout')
  },

  /**
   * 刷新令牌
   */
  refresh: (): Promise<LoginResponse | BusinessError> => {
    return apiPost<LoginResponse>('/auth/refresh')
  },

  /**
   * 获取当前用户信息
   */
  me: (): Promise<UserInfo | BusinessError> => {
    return apiGet<UserInfo>('/auth/me')
  },

  /**
   * 编辑当前用户个人资料（昵称、手机号、邮箱）
   */
  updateMe: (data: { nickname?: string; phone?: string; email?: string }): Promise<UserInfo | BusinessError> => {
    return apiPut<UserInfo>('/auth/me', data)
  },
}

/**
 * 菜单相关API
 */
export const menuApi = {
  /**
   * 获取当前用户菜单树
   */
  tree: (): Promise<MenuItem[] | BusinessError> => {
    return apiGet<MenuItem[]>('/menu/tree')
  },

  /**
   * 获取所有菜单（管理员）
   */
  all: (): Promise<MenuItem[] | BusinessError> => {
    return apiGet<MenuItem[]>('/menu/all')
  },
}
