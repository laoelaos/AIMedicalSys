import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useMenuStore } from '../stores/menu'

/**
 * 路由配置
 *
 * 定义医生端应用的路由结构和导航守卫。
 */
const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: '/',
    name: 'Layout',
    component: () => import('../components/Layout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        redirect: '/dashboard',
      },
      {
        path: '/dashboard',
        name: 'Dashboard',
        component: () => import('../views/Dashboard.vue'),
        meta: { requiresAuth: true },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

/**
 * 路由守卫
 *
 * 检查用户认证状态，未认证用户重定向到登录页。
 * 菜单获取失败时也重定向到登录页。
 * 进入登录页时清理动态路由，避免路由表膨胀。
 */
router.beforeEach(async (to, from, next) => {
  const authStore = useAuthStore()
  const menuStore = useMenuStore()

  if (to.meta.requiresAuth) {
    if (!authStore.isAuthenticated) {
      // 未认证跳转登录前，清理可能残留的菜单和动态路由
      menuStore.clearMenus()
      return next('/login')
    }

    // 加载菜单数据
    if (!menuStore.hasMenus) {
      const success = await menuStore.fetchMenus()
      if (!success) {
        // 菜单获取失败（网络错误/Token过期），清除认证状态并重定向到登录页
        await authStore.logout()
        menuStore.clearMenus()
        return next('/login')
      }
    }
  } else if (to.path === '/login') {
    // 进入登录页时，清理动态路由和菜单状态（登出场景）
    menuStore.clearMenus()
    if (authStore.isAuthenticated) {
      return next('/')
    }
  }

  next()
})

export default router