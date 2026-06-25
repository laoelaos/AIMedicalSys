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
  routerInstance: {
    addRoute: (name: string, route: any) => void
    hasRoute: (name: string) => boolean
    removeRoute: (name: string) => void
  },
  dynamicPageComponent: () => Promise<any>
) {
  return defineStore('menu', () => {
    const menus = ref<MenuItem[]>([])
    const activeMenu = ref<string>('')
    const routesRegistered = ref<boolean>(false)
    // 已注册的动态路由 name 列表，用于登出时清理
    const registeredRouteNames = ref<string[]>([])

    const hasMenus = computed(() => menus.value.length > 0)

    /**
     * 生成路由 name，优先使用 permission(code)，避免使用中文显示名导致冲突
     */
    function routeNameOf(menu: MenuItem): string {
      // permission 字段对应后端 Function.code，唯一且为英文标识
      return menu.permission || `menu_${menu.id}`
    }

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
          name: routeNameOf(menu),
          component: dynamicPageComponent,
          meta: { requiresAuth: true },
        })

        // 处理子菜单
        if (menu.children && menu.children.length > 0) {
          for (const child of menu.children) {
            if (child.path === '/' || child.path === '/dashboard' || child.path === '/login') {
              continue
            }
            routes.push({
              path: child.path,
              name: routeNameOf(child),
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
      const names: string[] = []
      for (const route of routes) {
        // 检查路由是否已存在
        if (!routerInstance.hasRoute(route.name)) {
          routerInstance.addRoute('Layout', route)
          names.push(route.name)
        }
      }
      registeredRouteNames.value = names
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
     * 清空菜单并移除动态路由
     *
     * <p>登出或token失效时调用，避免路由表膨胀和脏路由残留。
     */
    function clearMenus(): void {
      // 移除已注册的动态路由
      for (const name of registeredRouteNames.value) {
        if (routerInstance.hasRoute(name)) {
          routerInstance.removeRoute(name)
        }
      }
      registeredRouteNames.value = []
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
