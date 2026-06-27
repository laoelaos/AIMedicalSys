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
 * <p>安全说明（技术债务 T12 评估结论）：
 * 当前 Phase1 使用 localStorage 存储 JWT，存在 XSS 攻击风险（恶意脚本可读取 token）。
 * 这是有意为之的过渡方案，原因：
 * <ul>
 *   <li>跨域分离部署下 httpOnly cookie 需要 SameSite/CORS 精细配置，Phase1 为单体部署</li>
 *   <li>Spring Security 现有 JwtAuthenticationFilter 基于 Authorization header，改造为 cookie 需同步调整后端</li>
 * </ul>
 * Phase2+ 计划迁移到 httpOnly + SameSite=Strict cookie 方案，届时：
 * <ul>
 *   <li>后端登录/刷新接口通过 Set-Cookie 返回 token，不再在响应体返回</li>
 *   <li>前端不再手动管理 token，axios 配置 withCredentials=true</li>
 *   <li>CSRF 防护需同步启用（Spring Security CSRF token）</li>
 * </ul>
 *
 * @param options 配置选项
 * @returns Pinia store
 */
export function createAuthStore(options: AuthStoreOptions) {
  const TOKEN_KEY = `aimedical_${options.appType}_token`
  const USER_KEY = `aimedical_${options.appType}_user`

  return defineStore(`auth_${options.appType}`, () => {
    // 从localStorage初始化token和user
    // 安全提示：localStorage 可被 XSS 读取，Phase2+ 将迁移到 httpOnly cookie
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
      saveToken(response.access_token)
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
      saveToken(response.access_token)
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
     * 编辑当前用户个人资料
     * @param data 待更新的字段（nickname/phone/email）
     * @returns 更新结果 { success: boolean, user?: UserInfo, errorMessage?: string }
     */
    async function updateProfile(
      data: { nickname?: string; phone?: string; email?: string }
    ): Promise<{ success: boolean; user?: UserInfo; errorMessage?: string }> {
      const response = await authApi.updateMe(data)
      if (isBusinessError(response)) {
        return { success: false, errorMessage: response.message || '个人资料更新失败' }
      }
      saveUser(response)
      return { success: true, user: response }
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
      updateProfile,
      initializeAuth,
      clearAuthData,
    }
  })
}
