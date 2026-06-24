import { createRouter, createWebHistory } from 'vue-router'
import { getAccessToken } from '@aimedical/shared'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: '/profile',
    },
    {
      path: '/login',
      name: 'Login',
      component: () => import('../views/LoginPage.vue'),
      meta: { requiresAuth: false },
    },
    {
      path: '/register',
      name: 'Register',
      component: () => import('../views/RegisterPage.vue'),
      meta: { requiresAuth: false },
    },
    {
      path: '/profile',
      name: 'Profile',
      component: () => import('../views/ProfilePage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/health-record',
      name: 'HealthRecord',
      component: () => import('../views/HealthRecordPage.vue'),
      meta: { requiresAuth: true },
    },
  ],
})

router.beforeEach((to, _from, next) => {
  const token = getAccessToken()
  if (to.meta.requiresAuth !== false && !token) {
    next('/login')
  } else if (to.meta.requiresAuth === false && token) {
    next('/profile')
  } else {
    next()
  }
})

export default router
