import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

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
  // Use auth store instead of directly reading localStorage for consistency
  const authStore = useAuthStore()
  const isAuthed = authStore.isLoggedIn

  if (to.meta.requiresAuth !== false && !isAuthed) {
    next('/login')
  } else if (to.meta.requiresAuth === false && isAuthed) {
    next('/profile')
  } else {
    next()
  }
})

export default router
