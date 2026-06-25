/**
 * 管理员菜单Store
 *
 * 使用共享Store工厂创建，针对管理员端配置。
 *
 * <p>采用延迟初始化模式：不在模块顶层调用 createMenuStore，
 * 而是在首次 useMenuStore() 调用时才创建 store factory。
 * 这样可打破 router/index.ts ↔ stores/menu.ts 的循环依赖：
 * router 模块加载时导入 useMenuStore（仅函数引用，不触发 createMenuStore），
 * router 默认导出就绪后，路由守卫调用 useMenuStore() 时才传入 router 实例。
 */
import { createMenuStore } from '@aimedical/shared/stores/menu'
import router from '../router'

type MenuStoreFactory = ReturnType<typeof createMenuStore>

let storeFactory: MenuStoreFactory | null = null

function getStoreFactory(): MenuStoreFactory {
  if (!storeFactory) {
    storeFactory = createMenuStore(router, () => import('../views/DynamicPage.vue'))
  }
  return storeFactory
}

export function useMenuStore() {
  return getStoreFactory()()
}
