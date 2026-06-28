import axios, { AxiosRequestConfig, AxiosResponse } from 'axios'
import type { ApiResult, BusinessError } from '../types'

/**
 * 共享 axios 客户端与请求辅助函数。
 *
 * <p>从 index.ts 抽离到独立文件，避免业务 API 模块（如 doctor.ts）在 re-export 时
 * 与 index.ts 形成循环依赖。authApi/menuApi/doctorApi 均从此处导入 apiGet/apiPost 等。
 */
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
      // 清理失效令牌并跳转登录页（避免在登录页重复跳转）
      clearAuthToken()
      if (typeof window !== 'undefined' && !window.location.pathname.endsWith('/login')) {
        window.location.href = '/login'
      }
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
