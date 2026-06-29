/**
 * 管理员认证Store
 *
 * <p>使用共享Store工厂创建，针对管理员端配置。
 *
 * <p>T10 评估结论：此薄包装层必要，无法通过环境变量消除。原因：
 * <ul>
 *   <li>appType 决定 localStorage 的 key（aimedical_admin_token），实现各端 token 隔离</li>
 *   <li>Vite 环境变量（VITE_APP_TYPE）在模块加载时确定，但 createAuthStore 工厂需要在
 *       defineStore 调用时传入配置，环境变量方案与工厂模式无本质区别，反而增加配置复杂度</li>
 *   <li>各端需独立的 Pinia store 实例（store id 不同），不能共享</li>
 * </ul>
 * 保留当前薄包装层是最简方案。
 */
import { createAuthStore } from '@aimedical/shared/stores/auth'

export const useAuthStore = createAuthStore({
  appType: 'admin',
  appName: '管理员端',
})
