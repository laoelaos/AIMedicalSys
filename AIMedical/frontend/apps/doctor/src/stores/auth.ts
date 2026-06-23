/**
 * 医生认证Store
 *
 * 使用共享Store工厂创建，针对医生端配置。
 */
import { createAuthStore } from '@aimedical/shared/stores/auth'

export const useAuthStore = createAuthStore({
  appType: 'doctor',
  appName: '医生端',
})
