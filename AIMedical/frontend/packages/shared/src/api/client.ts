import axios, { AxiosRequestConfig, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import type { ApiResult, BusinessError } from '../types'
import { getAccessToken, setTokens, clearTokens, getRefreshToken } from '../utils'

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

// Request interceptor: attach JWT token
apiClient.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = getAccessToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// Response interceptor: unwrap Result<T>, handle errors
// Note: we intentionally unwrap AxiosResponse → data for ergonomic API wrappers.
// Axios strict interceptor typing disagrees with this pattern, so we assert.
apiClient.interceptors.response.use(
  ((response: AxiosResponse<ApiResult<unknown>>): unknown => {
    const body = response.data as ApiResult<unknown>
    if (body.code !== 'SUCCESS') {
      return { code: body.code, message: body.message ?? '', isBusinessError: true as const } as BusinessError
    }
    return body.data
  }) as Parameters<typeof apiClient.interceptors.response.use>[0],
  async (error: { response?: AxiosResponse; config?: InternalAxiosRequestConfig & { _retry?: boolean }; code?: string; message?: string }) => {
    if (error.response === undefined) {
      return { code: 'NETWORK_ERROR' as const, message: '网络不可达，请检查网络连接', isBusinessError: true as const } as BusinessError
    }

    // Try refresh token on 401 (only once)
    if (error.response.status === 401 && error.config && !error.config._retry) {
      const refreshToken = getRefreshToken()
      if (refreshToken) {
        error.config._retry = true
        try {
          const refreshResponse = await axios.post<ApiResult<{ access_token: string; refresh_token: string }>>(
            '/api/auth/refresh',
            { refresh_token: refreshToken }
          )
          const refreshBody = refreshResponse.data
          if (refreshBody.code === 'SUCCESS' && refreshBody.data) {
            setTokens(refreshBody.data.access_token, refreshBody.data.refresh_token)
            // Notify all Pinia stores that tokens were refreshed
            window.dispatchEvent(new CustomEvent('aimedical:tokens-refreshed', {
              detail: { access_token: refreshBody.data.access_token }
            }))
            if (error.config.headers) {
              error.config.headers.Authorization = `Bearer ${refreshBody.data.access_token}`
            }
            return apiClient(error.config)
          }
        } catch (refreshError) {
          // Refresh failed (token expired / network / server error), clear tokens
          console.warn('[api] token refresh failed:', (refreshError as Error)?.message ?? 'unknown error')
        }
      }
      clearTokens()
      return { code: 'UNAUTHORIZED' as const, message: '登录已过期，请重新登录', isBusinessError: true as const } as BusinessError
    }

    const requestUrl = error.config?.url ?? 'unknown'
    const status = error.response.status
    console.warn('[api] HTTP error for', requestUrl, 'status:', status)
    if (status === 401) {
      return { code: 'UNAUTHORIZED' as const, message: '登录已过期，请重新登录', isBusinessError: true as const } as BusinessError
    }
    if (status === 403) {
      return { code: 'FORBIDDEN' as const, message: '无权限访问', isBusinessError: true as const } as BusinessError
    }

    // Backend business error body (GlobalExceptionHandler returns Result JSON)
    const body = error.response.data as ApiResult<unknown> | undefined
    if (body && body.code) {
      return { code: body.code, message: body.message ?? `请求失败（${status}）`, isBusinessError: true as const } as BusinessError
    }
    return { code: 'HTTP_ERROR' as const, message: `请求失败（${status}）`, isBusinessError: true as const } as BusinessError
  },
)

// ==================== API Wrappers (try-catch pattern) ====================

export async function apiGet<T>(url: string, config?: AxiosRequestConfig): Promise<T | BusinessError> {
  try {
    return await apiClient.get(url, config) as T
  } catch {
    return { code: 'NETWORK_ERROR', message: '网络不可达，请检查网络连接', isBusinessError: true as const } as BusinessError
  }
}

export async function apiPost<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T | BusinessError> {
  try {
    return await apiClient.post(url, data, config) as T
  } catch {
    return { code: 'NETWORK_ERROR', message: '网络不可达，请检查网络连接', isBusinessError: true as const } as BusinessError
  }
}

export async function apiPut<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T | BusinessError> {
  try {
    return await apiClient.put(url, data, config) as T
  } catch {
    return { code: 'NETWORK_ERROR', message: '网络不可达，请检查网络连接', isBusinessError: true as const } as BusinessError
  }
}

export async function apiDelete<T>(url: string, config?: AxiosRequestConfig): Promise<T | BusinessError> {
  try {
    return await apiClient.delete(url, config) as T
  } catch {
    return { code: 'NETWORK_ERROR', message: '网络不可达，请检查网络连接', isBusinessError: true as const } as BusinessError
  }
}

export { apiClient }

// ==================== Token Helpers ====================

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
