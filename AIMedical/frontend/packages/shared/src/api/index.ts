import axios, { AxiosRequestConfig, AxiosResponse } from 'axios'
import type { BusinessError, ApiResult, LoginRequest, LoginResponse, UserInfo, MenuItem } from '../types'

const apiClient = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' },
})

apiClient.interceptors.response.use(
  (response: AxiosResponse) => {
    const body = response.data as ApiResult<unknown>
    if (body.code !== 'SUCCESS') {
      const error: BusinessError = { code: body.code, message: body.message ?? '', isBusinessError: true }
      return Promise.reject(error)
    }
    response.data = body.data
    return response
  },
  (error) => {
    if (error.response === undefined) {
      const err: BusinessError = { code: 'NETWORK_ERROR', message: '网络不可达，请检查网络连接', isBusinessError: true }
      return Promise.resolve(err)
    }
    if (error.response.status === 401) {
      const err: BusinessError = { code: 'UNAUTHORIZED', message: '登录已过期，请重新登录', isBusinessError: true }
      return Promise.resolve(err)
    }
    if (error.response.status === 403) {
      const err: BusinessError = { code: 'FORBIDDEN', message: '无权限访问', isBusinessError: true }
      return Promise.resolve(err)
    }
    const err: BusinessError = { code: 'HTTP_ERROR', message: `请求失败（${error.response.status}）`, isBusinessError: true }
    return Promise.resolve(err)
  },
)

export async function apiGet<T>(url: string, config?: AxiosRequestConfig): Promise<T | BusinessError> {
  try {
    const response = await apiClient.get<unknown>(url, config)
    if ((response as unknown as BusinessError).isBusinessError) {
      return response as unknown as BusinessError
    }
    return (response as AxiosResponse<T>).data
  } catch (error) {
    return error as BusinessError
  }
}

export async function apiPost<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T | BusinessError> {
  try {
    const response = await apiClient.post<unknown>(url, data, config)
    if ((response as unknown as BusinessError).isBusinessError) {
      return response as unknown as BusinessError
    }
    return (response as AxiosResponse<T>).data
  } catch (error) {
    return error as BusinessError
  }
}

export async function apiPut<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T | BusinessError> {
  try {
    const response = await apiClient.put<unknown>(url, data, config)
    if ((response as unknown as BusinessError).isBusinessError) {
      return response as unknown as BusinessError
    }
    return (response as AxiosResponse<T>).data
  } catch (error) {
    return error as BusinessError
  }
}

export async function apiDelete<T>(url: string, config?: AxiosRequestConfig): Promise<T | BusinessError> {
  try {
    const response = await apiClient.delete<unknown>(url, config)
    if ((response as unknown as BusinessError).isBusinessError) {
      return response as unknown as BusinessError
    }
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