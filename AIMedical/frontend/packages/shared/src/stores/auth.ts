import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi, setAuthToken, clearAuthToken } from '../api'
import type { UserInfo, LoginRequest, BusinessError } from '../types'

export interface AuthStoreOptions {
  /** 应用类型标识，用于localStorage的key区分 */
  appType: string
  /** 应用名称 */
  appName: string
}

/**
 * 创建认证状态管理Store
 *
 * @param options 配置选项
 * @returns Pinia store
 */
export function createAuthStore(options: AuthStoreOptions) {
  const TOKEN_KEY = `aimedical_${options.appType}_token`
  const USER_KEY = `aimedical_${options.appType}_user`

  return defineStore(`auth_${options.appType}`, () => {
    // 从localStorage初始化token和user
    const token = ref<string>(localStorage.getItem(TOKEN_KEY) || '')
    const storedUser = localStorage.getItem(USER_KEY)
    const user = ref<UserInfo | null>(storedUser ? JSON.parse(storedUser) : null)
    const isAuthenticated = computed(() => token.value !== '' && user.value !== null)

    /**
     * 保存token到localStorage
     */
    function saveToken(newToken: string): void {
      token.value = newToken
      localStorage.setItem(TOKEN_KEY, newToken)
      setAuthToken(newToken)
    }

    /**
     * 保存用户信息到localStorage
     */
    function saveUser(newUser: UserInfo): void {
      user.value = newUser
      localStorage.setItem(USER_KEY, JSON.stringify(newUser))
    }

    /**
     * 清除所有认证数据
     */
    function clearAuthData(): void {
      token.value = ''
      user.value = null
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
      clearAuthToken()
    }

    /**
     * 检查是否为业务错误
     */
    function isBusinessError(response: unknown): response is BusinessError {
      return (response as BusinessError).isBusinessError === true
    }

    /**
     * 用户登录
     * @returns 登录结果对象 { success: boolean, errorMessage?: string }
     */
    async function login(request: LoginRequest): Promise<{ success: boolean; errorMessage?: string }> {
      const response = await authApi.login(request)
      if (isBusinessError(response)) {
        // 透传后端返回的错误信息
        return { success: false, errorMessage: response.message || '登录失败，请检查用户名和密码' }
      }
      saveToken(response.token)
      saveUser(response.user)
      return { success: true }
    }

    /**
     * 用户登出
     */
    async function logout(): Promise<void> {
      await authApi.logout()
      clearAuthData()
    }

    /**
     * 刷新令牌
     */
    async function refreshToken(): Promise<boolean> {
      const response = await authApi.refresh()
      if (isBusinessError(response)) {
        clearAuthData()
        return false
      }
      saveToken(response.token)
      saveUser(response.user)
      return true
    }

    /**
     * 获取当前用户信息
     */
    async function fetchCurrentUser(): Promise<boolean> {
      const response = await authApi.me()
      if (isBusinessError(response)) {
        clearAuthData()
        return false
      }
      saveUser(response)
      return true
    }

    /**
     * 初始化认证状态（页面刷新后恢复会话）
     */
    async function initializeAuth(): Promise<boolean> {
      if (!token.value) {
        return false
      }
      // 设置axios的Authorization header
      setAuthToken(token.value)
      // 尝试获取用户信息以验证token有效性
      const success = await fetchCurrentUser()
      if (!success) {
        // token无效，尝试刷新
        return await refreshToken()
      }
      return true
    }

    return {
      token,
      user,
      isAuthenticated,
      login,
      logout,
      refreshToken,
      fetchCurrentUser,
      initializeAuth,
      clearAuthData,
    }
  })
}
