# Phase 1 Package D — 医生端与管理端登录/菜单动态化审查报告

## 审查文件清单（共 20 个）

| # | 文件路径 | 行数 |
|---|---------|------|
| 1 | `frontend/packages/shared/src/types/index.ts` | 71 |
| 2 | `frontend/packages/shared/src/api/index.ts` | 166 |
| 3 | `frontend/packages/shared/src/stores/auth.ts` | 177 |
| 4 | `frontend/packages/shared/src/stores/menu.ts` | 164 |
| 5 | `frontend/packages/shared/src/components/LoginBase.vue` | 183 |
| 6 | `frontend/packages/shared/src/components/HeaderBase.vue` | 91 |
| 7 | `frontend/packages/shared/src/components/SidebarBase.vue` | 210 |
| 8 | `frontend/packages/shared/src/components/LayoutBase.vue` | 73 |
| 9 | `frontend/packages/shared/src/components/index.ts` | 4 |
| 10 | `frontend/packages/shared/package.json` | 47 |
| 11 | `frontend/apps/doctor/src/stores/auth.ts` | 20 |
| 12 | `frontend/apps/doctor/src/stores/menu.ts` | 28 |
| 13 | `frontend/apps/doctor/src/views/Login.vue` | 45 |
| 14 | `frontend/apps/doctor/src/views/DynamicPage.vue` | 47 |
| 15 | `frontend/apps/doctor/src/router/index.ts` | 81 |
| 16 | `frontend/apps/doctor/src/components/Header.vue` | 28 |
| 17 | `frontend/apps/doctor/src/components/Sidebar.vue` | 25 |
| 18 | `frontend/apps/doctor/src/components/Layout.vue` | 23 |
| 19 | `frontend/apps/admin/src/stores/auth.ts` | 20 |
| 20 | `frontend/apps/admin/src/stores/menu.ts` | 28 |
| 21 | `frontend/apps/admin/src/views/Login.vue` | 45 |
| 22 | `frontend/apps/admin/src/views/DynamicPage.vue` | 47 |
| 23 | `frontend/apps/admin/src/router/index.ts` | 81 |
| 24 | `frontend/apps/admin/src/components/Header.vue` | 28 |
| 25 | `frontend/apps/admin/src/components/Sidebar.vue` | 25 |
| 26 | `frontend/apps/admin/src/components/Layout.vue` | 23 |

---

## 一、正确性 (Correctness) — 评分：78/100

### 优势
- **localStorage 持久化正确**：`saveToken` / `saveUser` / `clearAuthData` 三者在 reactive ref 和 localStorage 之间保持同步，无遗漏
- **initializeAuth 降级链正确**：`fetchCurrentUser → refreshToken → 返回 false` 的降级链逻辑清晰
- **业务错误类型守卫一致**：`isBusinessError` 类型谓词在 shared/auth 和 shared/menu 中实现一致
- **导航守卫对已认证/未认证的判别完整**：`requiresAuth` 假值则跳过，`/login` 路径做清理，已认证用户访问登录页自动重定向到首页
- **动态路由清理干净**：`clearMenus` 遍历 `registeredRouteNames` 逐一 `removeRoute`，无脏路由残留

### 缺陷

| # | 严重度 | 问题 | 位置 |
|---|--------|------|------|
| 🔴 CRITICAL | **`logout()` 缺失 `try-finally`，API 失败时会导致认证数据残留** | `createAuthStore.logout()` L98-101：若 `authApi.logout()` 抛出异常（网络断开等），`clearAuthData()` 不会执行，token/user 仍留在 localStorage 和 reactive state 中，后续请求仍会携带过期 header | `shared/stores/auth.ts:98-101` |
| 🔴 CRITICAL | **导航守卫没有处理竞态（race condition）** | `router.beforeEach` 中 `await menuStore.fetchMenus()` 为异步操作，若用户连续快速触发路由跳转（如双击链接），可能有两个导航同时通过 `if (!menuStore.hasMenus)`（L61），导致 `fetchMenus()` 被并发调用两次，产生重复的 `addRoute` | `doctor/router/index.ts:49-78`, `admin/router/index.ts:49-78` |
| 🟡 MEDIUM | **401 拦截器在活跃会话中不会触发自动 refresh** | `api/index.ts` 的 401 拦截器（L42-43）直接 reject `BusinessError('UNAUTHORIZED')`，但 shared auth store 中的 `refreshToken()` 仅在 `initializeAuth()` 中调用（页面刷新场景）。若 token 在活跃会话中过期，用户直接跳转到登录页，不会尝试刷新 | `shared/api/index.ts:42-43` |
| 🟡 MEDIUM | **`convertMenusToRoutes` 硬编码父路由名 `'Layout'`** | L99 调用 `routerInstance.addRoute('Layout', route)`，若任意一端的 router 中 Layout 路由的 name 被修改（如改为 `'MainLayout'`），`addRoute` 会静默回到根路由注册，导致动态路由不生效 | `shared/stores/menu.ts:99` |
| 🟡 MEDIUM | **`convertMenusToRoutes` 仅处理两级菜单，三级+ 子菜单被忽略** | L68-80 的 `children` 循环不做递归，深度嵌套的菜单项不会变成路由 | `shared/stores/menu.ts:68-80` |
| ⚪ LOW | `apiGet<void>` 中 `void` 作为泛型在 TS 中可能不会按预期工作 | L124 `apiPost<void>` 返回 `Promise<void \| BusinessError>`，实际调用 `response.data` 为 `undefined`，类型正确但不如 `undefined` 直观 | `shared/api/index.ts:124` |

---

## 二、设计合理性 (Design) — 评分：82/100

### 优势
- **工厂模式 + 薄包装层**：`createAuthStore` / `createMenuStore` 接受配置参数（`appType`、`routerInstance`），两端仅需 3-5 行包装代码即可复用全部逻辑，避免重复
- **依赖方向严格单向**：shared → 不依赖任何 app；apps → 依赖 shared，符合 monorepo 规范
- **Router 实例外部注入**：`createMenuStore` 将 `routerInstance` 作为参数传入，使 store 不直接 import vue-router，可测试性提升
- **纯 UI 组件化**：Base 组件通过 props + slots + emit 实现，不直接依赖 store；HeaderBase 的 logout 通过 emit 委托给 wrapper，无 store 导入
- **懒初始化打破循环依赖**：doctor/admin menu stores 采用 `getStoreFactory()` 延迟初始化模式，避免 `router/index ↔ stores/menu` 的循环引用

### 缺陷

| # | 严重度 | 问题 | 位置 |
|---|--------|------|------|
| 🟡 MEDIUM | **循环依赖仍然脆弱，依赖模块加载顺序的隐含约定** | `doctor/stores/menu.ts` L13 `import router from '../router'` 在顶层执行，router 模块顶层又 import `useMenuStore`，形成 A→B→A 的循环。虽然 ES module 的 live binding 能让此工作，但 webpack/rollup 的 scope hoisting 或 tree-shaking 可能改变执行顺序导致 `router` 为 `undefined` | `doctor/stores/menu.ts:13`, `doctor/router/index.ts:3` |
| 🟡 MEDIUM | **`activeMenu` 状态冗余** | `SidebarBase` 的 `isActive()` 同时检查 `props.activeMenu` 和 `route.path`，且 `route.path` 作为 fallback。`setActiveMenu` 不会在浏览器前进/后退时更新，导致 `activeMenu` 与实际路由不同步。可完全依赖 `route.path`（删掉 `activeMenu`） | `shared/stores/menu.ts:23-24`, `shared/components/SidebarBase.vue:112-115` |
| 🟡 MEDIUM | **`SidebarBase` 直接调用 `useRoute()`，隐含对 vue-router 的依赖** | 代码注释声称"纯 UI 组件，不依赖具体应用的 store"，但直接使用 `useRoute()` 使其依赖 vue-router，与注入 `routerInstance` 的模式不一致 | `shared/components/SidebarBase.vue:86` |
| ⚪ LOW | **`createMenuStore` 的 `routerInstance` 参数类型是手写的子集** | 使用 `{ addRoute, hasRoute, removeRoute }` 手写接口，不如直接使用 vue-router 的 `Router` 类型完整，且失去了对 `addRoute` 完整签名（RouteRecordRaw）的类型校验 | `shared/stores/menu.ts:14-18` |

---

## 三、目标语言特性 (Vue 3 / TypeScript idioms) — 评分：85/100

### 优势
- 全 Composition API + `<script setup lang="ts">`，无 Options API 代码
- Pinia composition stores（`defineStore('id', () => { ... })`）使用正确
- 类型安全：`defineProps<{...}>()` 纯类型声明、`defineEmits<{(e: 'login', ...): void}>()`、`InstanceType<typeof LoginBase>`、泛型 API 函数 `apiGet<T>` / `apiPost<T>`
- `v-bind()` in CSS 实现动态样式绑定（渐变色传参）
- `computed` / `ref` 使用恰当，`computed(() => ...)` 语义正确
- 动态 import 懒加载路由组件

### 缺陷

| # | 严重度 | 问题 | 位置 |
|---|--------|------|------|
| 🟡 MEDIUM | **`apiGet<T>` catch 分支类型不安全** | L62 `return error as BusinessError` 是类型断言（type assertion），不是类型收窄。若 axios interceptor 之外抛出非 BusinessError 异常（如断言错误），此断言会掩盖真实类型 | `shared/api/index.ts:62` |
| 🟡 MEDIUM | **`BusinessError.isBusinessError` 为可选属性，判别联合不严谨** | `isBusinessError?: true` 是可选属性，普通的 `{ code, message }` 对象若不含 `isBusinessError` 属性，`in` 检查或 `=== true` 会返回 false，但不会在编译期报错。最好改为必选属性 | `shared/types/index.ts:24-28` |
| ⚪ LOW | **`LoginBase.vue` 在模板中直接调用方法 `isActive(path)`** | 每次 re-render 都会重新计算，简单路径比较开销可忽略，但若菜单层级变深应换为 `computed` | `shared/components/SidebarBase.vue:11,22,45` |

---

## 四、可读性/可维护性 (Readability) — 评分：90/100

### 优势
- **高质量的 JSDoc**：每个文件的头部注释说明设计意图、技术债务编号（T3/T4/T5/T10/T11/T12）、评估结论；函数文档清晰
- **命名一致性**：`createAuthStore` / `createMenuStore` / `LoginBase` / `SidebarBase` / `HeaderBase` / `LayoutBase`，模式统一
- **薄包装层职责明确**：各 app 的 Login.vue / Layout.vue / Header.vue / Sidebar.vue / stores/auth.ts / stores/menu.ts 都在文件头注释说明"薄包装组件"
- **清晰的安全债务标注**：所有 XSS / HTTPS 相关注释包含 Phase2 迁移方案和部署要求

### 缺陷

| # | 严重度 | 问题 | 位置 |
|---|--------|------|------|
| ⚪ LOW | 部分 JSDoc 过于冗长（auth.ts 的安全说明段落建议挪到 ADR 或架构文档） | `shared/stores/auth.ts:16-32` |
| ⚪ LOW | `LoginBase` 使用 `emit('login')` + `defineExpose({ setError, setLoading })` 的双向通信模式不直观 | 父组件需要同时监听事件 + 调用暴露方法，相比 props 传 `error`/`loading` 更为绕路 | `shared/components/LoginBase.vue:60-95` |
| ⚪ LOW | `SidebarBase.vue` 中 `menu` 循环使用了同一个变量名 `menu`（L16）和循环内的 `menu` 函数参数（L91）在外层函数中没有命名冲突但可读性略差 | `shared/components/SidebarBase.vue:16,91` |

---

## 五、安全性 (Security) — 评分：65/100

### 优势
- **Token 隔离**：通过 `appType` 作为 localStorage key 前缀（`aimedical_doctor_token` / `aimedical_admin_token`），两端 token 互不影响
- **安全债务明确记录**：XSS 风险、HTTPS 要求、Phase2 迁移计划都已写入代码注释，不会遗漏
- `clearAuthToken()` 正确删除 `Authorization` header

### 缺陷

| # | 严重度 | 问题 | 位置 |
|---|--------|------|------|
| 🔴 CRITICAL | **`logout()` 缺少 `try-finally`，API 异常时认证数据不清除** | 同正确性部分。攻击者可能利用此漏洞阻止登出，保留有效 session | `shared/stores/auth.ts:98-101` |
| 🟡 MEDIUM | **无前端输入校验** | `LoginBase` 对 `username` / `password` 不做任何前端校验（minLength / 特殊字符 / XSS payload 过滤），完全依赖后端校验。虽然非直接安全问题，但增加攻击面 | `shared/components/LoginBase.vue:64-67` |
| 🟡 MEDIUM | **用户信息明文存 localStorage** | `saveUser` 将完整的 `UserInfo`（含 `role` / `permissions`）以 JSON 存入 localStorage，可由 XSS 读取 | `shared/stores/auth.ts:57-59` |
| ⚪ LOW | **无 CSRF 防护** | 当前 Phase1 使用 Bearer token（Authorization header）不易受 CSRF 攻击，但注释中提到的 Phase2 Cookie 方案需要同步启用 CSRF token | 已在注释中记录 |

---

## 六、性能 (Performance) — 评分：80/100

### 优势
- 所有业务页面组件均使用动态 import（`() => import(...)`），code splitting 正确
- `routesRegistered` 标志位防止重复注册动态路由
- `hasRoute()` 二次校验避免重复 addRoute
- `registerDynamicRoutes` 仅注册一次，后续导航直接从已注册的路由命中

### 缺陷

| # | 严重度 | 问题 | 位置 |
|---|--------|------|------|
| 🟡 MEDIUM | **导航守卫阻塞直到 `fetchMenus()` 完成** | `await menuStore.fetchMenus()` 在 `router.beforeEach` 中同步等待 API 返回，期间所有导航操作挂起，UI 无反馈。慢网络下用户体验差 | `doctor/router/index.ts:62`, `admin/router/index.ts:62` |
| ⚪ LOW | **`SidebarBase` 每次重新渲染时 `isActive()` / `hasChildren()` 方法调用** | 模板中 `isActive('/dashboard')` / `hasChildren(menu)` / `isActive(menu.path)` / `isParentActive(menu)` 每次重新渲染都会执行，但此类纯函数开销可忽略 | `shared/components/SidebarBase.vue` 多个位置 |
| ⚪ LOW | `expandedKeys.value = next` 每次 toggle 创建新 Set 对象（非必须） | 可改用 `reactive(new Set())`，但 toggle 场景极少成为瓶颈 | `shared/components/SidebarBase.vue:109` |

---

## 综合评分 & Phase 1 完成度置信度

| 维度 | 评分 |
|------|------|
| 正确性 (Correctness) | 78/100 |
| 设计合理性 (Design) | 82/100 |
| 语言特性 (Vue 3/TS idioms) | 85/100 |
| 可读性/可维护性 (Readability) | 90/100 |
| 安全性 (Security) | 65/100 |
| 性能 (Performance) | 80/100 |
| **综合加权** | **80/100** |

**Phase 1 Package D 完成度置信度：88%**

---

## 必须修复的 Critical 问题

### C1. `createAuthStore.logout()` 缺少 try-finally

```ts
// shared/stores/auth.ts 当前代码（L98-101）
async function logout(): Promise<void> {
  await authApi.logout()
  clearAuthData()
}

// 修复方案
async function logout(): Promise<void> {
  try {
    await authApi.logout()
  } finally {
    clearAuthData()
  }
}
```

`finally` 确保无论 API 调用是否成功（网络断开、500 等），token/user 数据都被清除，防止 session 残留。

### C2. 导航守卫竞态（race condition）

```ts
// doctor & admin router/index.ts
router.beforeEach(async (to, from, next) => {
  // ...
  if (!menuStore.hasMenus) {               // 非原子检查
    const success = await menuStore.fetchMenus()  // 此处可被第二个导航并发进入
    // ...
  }
})
```

**修复方案**：在 `createMenuStore` 中添加 `fetching` 标志，`fetchMenus` 内部用互斥：

```ts
const fetching = ref(false)

async function fetchMenus(): Promise<boolean> {
  if (fetching.value) {
    while (fetching.value) {
      await new Promise(r => setTimeout(r, 50))
    }
    return menus.value.length > 0
  }
  fetching.value = true
  try {
    // ... existing logic
  } finally {
    fetching.value = false
  }
}
```

### 其他推荐修复（Medium 优先级）

| # | 建议 |
|---|------|
| M1 | 为 `logout()` 加 try-finally（同 C1 方案） |
| M2 | 在 `api/index.ts` 的 401 拦截器中集成 refresh token 逻辑（或 postMessage 通知 store 刷新） |
| M3 | 将 `createMenuStore` 的 `'Layout'` 硬编码改为参数化 |
| M4 | 考虑将 `convertMenusToRoutes` 改为递归以支持任意深度菜单 |
| M5 | 在 `SidebarBase` 中用 `reactive(new Set())` 替代 ref+Set 全量替换模式 |
