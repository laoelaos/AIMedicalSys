import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useMenuStore } from '../stores/menu'

/**
 * 路由配置
 *
 * 定义医生端应用的路由结构和导航守卫。
 *
 * <p>Phase 3 新增静态业务路由：
 * - 包A 诊疗闭环：/queue、/patient/:patientId/*（信息卡、病情录入、病历列表/详情/表单、处方表单）
 * - 包B AI 入口：/ai/*（诊断、检查、开方、审核、病历生成）
 *
 * <p>注意路由顺序：静态字面量路径（如 .../medical-records/new）须排在动态参数路径
 * （如 .../medical-records/:id）之前，否则 "new" 会被当作 :id 匹配。
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
      // ---- 包A：诊疗闭环 ----
      {
        path: '/queue',
        name: 'ConsultationQueue',
        component: () => import('../views/ConsultationQueue.vue'),
        meta: { requiresAuth: true },
      },
      {
        path: '/patient/:patientId',
        name: 'PatientInfo',
        component: () => import('../views/PatientInfo.vue'),
        meta: { requiresAuth: true },
      },
      {
        path: '/patient/:patientId/condition',
        name: 'ConditionEntry',
        component: () => import('../views/ConditionEntry.vue'),
        meta: { requiresAuth: true },
      },
      {
        path: '/patient/:patientId/medical-records',
        name: 'MedicalRecordList',
        component: () => import('../views/MedicalRecordList.vue'),
        meta: { requiresAuth: true },
      },
      // "new" 必须在 ":id" 之前
      {
        path: '/patient/:patientId/medical-records/new',
        name: 'MedicalRecordForm',
        component: () => import('../views/MedicalRecordForm.vue'),
        meta: { requiresAuth: true },
      },
      {
        path: '/patient/:patientId/medical-records/:recordId',
        name: 'MedicalRecordDetail',
        component: () => import('../views/MedicalRecordDetail.vue'),
        meta: { requiresAuth: true },
      },
      {
        // 编辑已有病历草稿：使用独立命名路由与 path param，避免"new 实际是编辑"的语义混乱
        path: '/patient/:patientId/medical-records/:recordId/edit',
        name: 'MedicalRecordEdit',
        component: () => import('../views/MedicalRecordForm.vue'),
        meta: { requiresAuth: true },
      },
      {
        path: '/patient/:patientId/prescriptions/new',
        name: 'PrescriptionForm',
        component: () => import('../views/PrescriptionForm.vue'),
        meta: { requiresAuth: true },
      },
      // ---- 包B：AI 入口 ----
      {
        path: '/ai/diagnosis',
        name: 'AiDiagnosis',
        component: () => import('../views/ai/AiDiagnosis.vue'),
        meta: { requiresAuth: true },
      },
      {
        path: '/ai/examination',
        name: 'AiExamination',
        component: () => import('../views/ai/AiExamination.vue'),
        meta: { requiresAuth: true },
      },
      {
        path: '/ai/prescription-assist',
        name: 'AiPrescriptionAssist',
        component: () => import('../views/ai/AiPrescriptionAssist.vue'),
        meta: { requiresAuth: true },
      },
      {
        path: '/ai/prescription-audit',
        name: 'AiPrescriptionAudit',
        component: () => import('../views/ai/AiPrescriptionAudit.vue'),
        meta: { requiresAuth: true },
      },
      {
        path: '/ai/medical-record-gen',
        name: 'AiMedicalRecordGen',
        component: () => import('../views/ai/AiMedicalRecordGen.vue'),
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
