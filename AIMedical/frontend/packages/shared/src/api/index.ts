import axios, { AxiosRequestConfig } from 'axios'
import type { ApiResponse, BusinessError, ApiResult } from '../types'

const apiClient = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' },
})

apiClient.interceptors.response.use(
  (response) => {
    const body = response.data as ApiResult<unknown>
    if (body.code !== 'SUCCESS') {
      return ({ code: body.code, message: body.message ?? '', isBusinessError: true as const } as BusinessError) as unknown
    }
    return body.data
  },
  (error) => {
    if (error.response === undefined) {
      return Promise.resolve({ code: 'NETWORK_ERROR' as const, message: '网络不可达，请检查网络连接', isBusinessError: true as const })
    }
    if (error.response.status === 401) {
      return Promise.resolve({ code: 'UNAUTHORIZED' as const, message: '登录已过期，请重新登录', isBusinessError: true as const })
    }
    if (error.response.status === 403) {
      return Promise.resolve({ code: 'FORBIDDEN' as const, message: '无权限访问', isBusinessError: true as const })
    }
    return Promise.resolve({ code: 'HTTP_ERROR' as const, message: `请求失败（${error.response.status}）`, isBusinessError: true as const })
  },
)

export async function apiGet<T>(url: string, config?: AxiosRequestConfig): Promise<T | BusinessError> {
  return apiClient.get(url, config) as unknown as Promise<T | BusinessError>
}

export async function apiPost<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T | BusinessError> {
  return apiClient.post(url, data, config) as unknown as Promise<T | BusinessError>
}

export async function apiPut<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T | BusinessError> {
  return apiClient.put(url, data, config) as unknown as Promise<T | BusinessError>
}

export async function apiDelete<T>(url: string, config?: AxiosRequestConfig): Promise<T | BusinessError> {
  return apiClient.delete(url, config) as unknown as Promise<T | BusinessError>
}

export { apiClient }
