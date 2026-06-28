import type { BusinessError, LoginRequest, LoginResponse, UserInfo, MenuItem } from '../types'
import { apiGet, apiPost, apiPut } from './client'

// 重新导出 axios 客户端与底层请求函数（供外部直接使用）
export { apiClient, apiGet, apiPost, apiPut, apiDelete, setAuthToken, clearAuthToken } from './client'

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

// 医生端 API
export { doctorApi } from './doctor'
