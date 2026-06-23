/**
 * 管理员认证Store
 *
 * 使用共享Store工厂创建，针对管理员端配置。
 */
import { createAuthStore } from '@aimedical/shared/stores/auth'

export const useAuthStore = createAuthStore({
  appType: 'admin',
  appName: '管理员端',
})
