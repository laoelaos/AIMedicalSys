import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: '/home',
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
      path: '/home',
      name: 'Home',
      component: () => import('../views/HomePage.vue'),
      meta: { requiresAuth: true },
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
    {
      path: '/triage',
      name: 'AITriage',
      component: () => import('../views/AITriagePage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/hospital-intro',
      name: 'HospitalIntro',
      component: () => import('../views/HospitalIntroPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/fee-query',
      name: 'FeeQuery',
      component: () => import('../views/FeeQueryPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/consult',
      name: 'AIConsult',
      component: () => import('../views/AIConsultPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/appointment',
      name: 'DoctorAppointment',
      component: () => import('../views/DoctorAppointmentPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/registration',
      name: 'Registration',
      component: () => import('../views/RegistrationPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/records',
      name: 'Records',
      component: () => import('../views/RecordsPage.vue'),
      meta: { requiresAuth: true },
    },
  ],
})

router.beforeEach((to, _from, next) => {
  const authStore = useAuthStore()
  const isAuthed = authStore.isLoggedIn

  if (to.meta.requiresAuth !== false && !isAuthed) {
    next('/login')
  } else if (to.meta.requiresAuth === false && isAuthed) {
    next('/home')
  } else {
    next()
  }
})

export default router
