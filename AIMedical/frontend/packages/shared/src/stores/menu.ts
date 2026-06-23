import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { menuApi } from '../api'
import type { MenuItem, BusinessError } from '../types'

/**
 * 创建菜单状态管理Store
 *
 * @param routerInstance Vue Router实例
 * @param dynamicPageComponent 动态页面组件导入函数
 * @returns Pinia store
 */
export function createMenuStore(
  routerInstance: { addRoute: (name: string, route: any) => void; hasRoute: (name: string) => boolean },
  dynamicPageComponent: () => Promise<any>
) {
  return defineStore('menu', () => {
    const menus = ref<MenuItem[]>([])
    const activeMenu = ref<string>('')
    const routesRegistered = ref<boolean>(false)

    const hasMenus = computed(() => menus.value.length > 0)

    /**
     * 将菜单转换为路由配置
     */
    function convertMenusToRoutes(menuItems: MenuItem[]): Array<{
      path: string
      name: string
      component: () => Promise<any>
      meta: { requiresAuth: boolean }
    }> {
      const routes: Array<{
        path: string
        name: string
        component: () => Promise<any>
        meta: { requiresAuth: boolean }
      }> = []

      for (const menu of menuItems) {
        // 跳过根路径和已存在的路由
        if (menu.path === '/' || menu.path === '/dashboard' || menu.path === '/login') {
          continue
        }

        routes.push({
          path: menu.path,
          name: menu.name,
          component: dynamicPageComponent,
          meta: { requiresAuth: true },
        })

        // 处理子菜单
        if (menu.children && menu.children.length > 0) {
          for (const child of menu.children) {
            routes.push({
              path: child.path,
              name: child.name,
              component: dynamicPageComponent,
              meta: { requiresAuth: true },
            })
          }
        }
      }

      return routes
    }

    /**
     * 注册动态路由
     */
    function registerDynamicRoutes(menuItems: MenuItem[]): void {
      if (routesRegistered.value) {
        return
      }

      const routes = convertMenusToRoutes(menuItems)
      for (const route of routes) {
        // 检查路由是否已存在
        if (!routerInstance.hasRoute(route.name)) {
          routerInstance.addRoute('Layout', route)
        }
      }
      routesRegistered.value = true
    }

    /**
     * 获取用户菜单列表
     */
    async function fetchMenus(): Promise<boolean> {
      const response = await menuApi.tree()
      if (isBusinessError(response)) {
        return false
      }
      menus.value = response
      // 注册动态路由
      registerDynamicRoutes(response)
      return true
    }

    /**
     * 设置激活菜单
     */
    function setActiveMenu(path: string): void {
      activeMenu.value = path
    }

    /**
     * 清空菜单
     */
    function clearMenus(): void {
      menus.value = []
      activeMenu.value = ''
      routesRegistered.value = false
    }

    /**
     * 检查是否为业务错误
     */
    function isBusinessError(response: unknown): response is BusinessError {
      return (response as BusinessError).isBusinessError === true
    }

    return {
      menus,
      activeMenu,
      hasMenus,
      routesRegistered,
      fetchMenus,
      setActiveMenu,
      clearMenus,
      registerDynamicRoutes,
    }
  })
}
