/**
 * 医生菜单Store
 *
 * 使用共享Store工厂创建，针对医生端配置。
 */
import { createMenuStore } from '@aimedical/shared/stores/menu'
import router from '../router'

export const useMenuStore = createMenuStore(router, () => import('../views/DynamicPage.vue'))
