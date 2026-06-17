# R2-A3: 前端 Monorepo — 与 OOD 设计 §2.4 一致性审查

审查时间：2026-06-17

### 审查范围

- `AIMedical/frontend/package.json`
- `AIMedical/frontend/tsconfig.base.json`
- `AIMedical/frontend/.gitignore`
- `AIMedical/frontend/packages/shared/package.json`
- `AIMedical/frontend/packages/shared/src/index.ts`
- `AIMedical/frontend/packages/shared/src/api/index.ts`
- `AIMedical/frontend/packages/shared/src/types/index.ts`
- `AIMedical/frontend/packages/shared/src/utils/index.ts`
- `AIMedical/frontend/packages/ui-core/package.json`
- `AIMedical/frontend/packages/ui-core/src/index.ts`
- `AIMedical/frontend/packages/ui-core/src/components/index.ts`
- `AIMedical/frontend/apps/patient/package.json`
- `AIMedical/frontend/apps/patient/index.html`
- `AIMedical/frontend/apps/patient/src/main.ts`
- `AIMedical/frontend/apps/patient/src/App.vue`
- `AIMedical/frontend/apps/patient/vite.config.ts`
- `AIMedical/frontend/apps/patient/tsconfig.json`
- `AIMedical/frontend/apps/doctor/package.json`
- `AIMedical/frontend/apps/doctor/index.html`
- `AIMedical/frontend/apps/doctor/src/main.ts`
- `AIMedical/frontend/apps/doctor/src/App.vue`
- `AIMedical/frontend/apps/doctor/vite.config.ts`
- `AIMedical/frontend/apps/doctor/tsconfig.json`
- `AIMedical/frontend/apps/admin/package.json`
- `AIMedical/frontend/apps/admin/index.html`
- `AIMedical/frontend/apps/admin/src/main.ts`
- `AIMedical/frontend/apps/admin/src/App.vue`
- `AIMedical/frontend/apps/admin/vite.config.ts`
- `AIMedical/frontend/apps/admin/tsconfig.json`

### 发现

#### [一般] ApiClient 错误拦截器未按 §3.5 实现 NETWORK_ERROR 处理

- **位置**：`packages/shared/src/api/index.ts:16-18`
- **描述**：设计文档 §3.5 要求 Axios 错误拦截器统一捕获网络错误（DNS 解析失败、连接超时、请求被取消等），返回固定格式 `{ code: "NETWORK_ERROR", message: "网络不可达，请检查网络连接" }`。当前实现仅为 `return Promise.reject(error)`，直接透传原始 Axios 错误，未做任何转换。前端代码若依赖 `code === "NETWORK_ERROR"` 判断网络错误将无法生效，未捕获的 Promise 异常可能在运行期导致控制台报错。
- **建议**：在错误拦截器中判断 `!error.response`（Axios 网络错误特征），返回 `Promise.resolve({ code: "NETWORK_ERROR", message: "网络不可达，请检查网络连接" })`，使调用方可通过统一格式处理。

#### [轻微] shared 包中 axios 声明为 devDependency 而非 dependency

- **位置**：`packages/shared/package.json:6-8`
- **描述**：`axios` 是 `packages/shared` 运行时的直接依赖（`api/index.ts` 中 `import axios from 'axios'`），但被声明在 `devDependencies` 中。npm workspaces 会将 axios 提升至根 `node_modules`，因此当前不会导致运行时错误，但语义上不符合依赖分类规范：runtime 依赖应放在 `dependencies` 中。
- **建议**：将 `axios` 从 `devDependencies` 移至 `dependencies`。

### 本轮统计

| 严重程度 | 数量 |
|---------|------|
| 严重 | 0 |
| 一般 | 1 |
| 轻微 | 1 |

### 总评

前端 Monorepo 整体实现与 OOD §2.4 设计高度一致。workspace 配置、内部包命名与导出、三端应用结构、tsconfig 继承、Vite 代理配置、占位页面内容均正确实现了设计意图。存在两项偏离：ApiClient 错误拦截器未按 §3.5 处理网络错误（一般性问题，建议修复），以及 axios 依赖分类不准确（轻微）。建议在 Phase 0 收尾阶段修复 Axios 错误拦截器的设计偏差。
