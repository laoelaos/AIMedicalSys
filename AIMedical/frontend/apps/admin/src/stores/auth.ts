import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi, setAuthToken, clearAuthToken } from '@aimedical/shared/api'
import type { UserInfo, LoginRequest, BusinessError } from '@aimedical/shared/types'

/**
 * 认证状态管理Store
 *
 * 管理管理员认证状态，包括令牌、用户信息和登录状态。
 */
export const useAuthStore = defineStore('auth', () => {
  const token = ref<string>('')
  const user = ref<UserInfo | null>(null)
  const isAuthenticated = computed(() => token.value !== '' && user.value !== null)

  /**
   * 用户登录
   */
  async function login(request: LoginRequest): Promise<boolean> {
    const response = await authApi.login(request)
    if (isBusinessError(response)) {
      return false
    }
    token.value = response.token
    user.value = response.user
    setAuthToken(response.token)
    return true
  }

  /**
   * 用户登出
   */
  async function logout(): Promise<void> {
    await authApi.logout()
    token.value = ''
    user.value = null
    clearAuthToken()
  }

  /**
   * 刷新令牌
   */
  async function refreshToken(): Promise<boolean> {
    const response = await authApi.refresh()
    if (isBusinessError(response)) {
      return false
    }
    token.value = response.token
    user.value = response.user
    setAuthToken(response.token)
    return true
  }

  /**
   * 获取当前用户信息
   */
  async function fetchCurrentUser(): Promise<boolean> {
    const response = await authApi.me()
    if (isBusinessError(response)) {
      return false
    }
    user.value = response
    return true
  }

  /**
   * 检查是否为业务错误
   */
  function isBusinessError(response: unknown): response is BusinessError {
    return (response as BusinessError).isBusinessError === true
  }

  return {
    token,
    user,
    isAuthenticated,
    login,
    logout,
    refreshToken,
    fetchCurrentUser,
  }
})