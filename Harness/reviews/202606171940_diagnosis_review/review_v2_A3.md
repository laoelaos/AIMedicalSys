# R{A3}: 前端 (apps + shared + ui-core) 实现与 OOD 设计一致性审查

审查时间：2026-06-17

### 审查范围

- `AIMedical/frontend/apps/patient/index.html`, `src/App.vue`, `src/main.ts`, `vite.config.ts`, `tsconfig.json`, `env.d.ts`, `package.json`
- `AIMedical/frontend/apps/doctor/index.html`, `src/App.vue`, `src/main.ts`, `vite.config.ts`, `tsconfig.json`, `env.d.ts`, `package.json`
- `AIMedical/frontend/apps/admin/index.html`, `src/App.vue`, `src/main.ts`, `vite.config.ts`, `tsconfig.json`, `env.d.ts`, `package.json`
- `AIMedical/frontend/packages/shared/package.json`, `src/index.ts`, `src/api/index.ts`, `src/types/index.ts`, `src/utils/index.ts`, `vitest.config.ts`
- `AIMedical/frontend/packages/ui-core/package.json`, `src/index.ts`, `src/components/index.ts`
- `AIMedical/frontend/package.json`, `tsconfig.base.json`, `.gitignore`

### 发现

#### [一般] Axios 响应拦截器未实现 OOD §4.2 规定的 Result.code 拆包逻辑

- **位置**：`packages/shared/src/api/index.ts:10-26`
- **描述**：OOD §4.2 规定响应拦截器应对 `Result.code` 做拆包：`code === "SUCCESS"` → 返回 `response.data.data`（仅内部数据）；`code !== "SUCCESS"` → 走错误处理。但实际拦截器返回 `response.data`（完整包装体）而非仅 `data` 字段。测试文件（`interceptors.test.ts:44`）明确注释并验证了"does not unwrap nested data (returns response.data as-is)"，确认该行为是有意为之的设计选择。这导致消费者始终需要手动检查 `result.code === 'SUCCESS'` 后才能访问 `result.data`，与 OOD 设计契约不一致。
- **建议**：若决定保留当前"不拆包"设计（返回完整 `ApiResponse<T>` 由消费者自行判别），应在设计文档中同步更新该行为描述。若需对齐 OOD，应在成功拦截器中添加 `if (data.code === 'SUCCESS') return data.data` 分支。

#### [轻微] 三端 index.html `<title>` 与 OOD §2.4 定义不一致

- **位置**：`apps/patient/index.html:6`, `apps/doctor/index.html:6`, `apps/admin/index.html:6`
- **描述**：OOD §2.4 挂载点骨架规定 `<title>智慧云脑诊疗平台</title>`（统称，无端名后缀）。但三端实际代码分别使用了 "智慧云脑诊疗平台 - 患者端"、"智慧云脑诊疗平台 - 医生端"、"智慧云脑诊疗平台 - 管理员端"。虽然从浏览器标签页区分角度提升了易用性，但偏离了设计文档的标题约定。
- **建议**：统一为 OOD 规定的 `智慧云脑诊疗平台`，或在设计文档中补充说明允许按端名差异化。

#### [轻微] packages/shared 中 axios 置于 devDependencies

- **位置**：`packages/shared/package.json:7`
- **描述**：`axios` 是 `@aimedical/shared` 的运行期依赖（`src/api/index.ts` 直接引用 `import axios from 'axios'`），但被声明在 `devDependencies` 而非 `dependencies` 中。因 npm workspace 的 hoisting 机制，当前开发环境下可正常工作，但严格来说运行期依赖应放在 `dependencies` 中，以确保作为独立包使用时依赖正确安装。
- **建议**：将 `axios` 从 `devDependencies` 移至 `dependencies`。

### 本轮统计

| 严重程度 | 数量 |
|---------|------|
| 严重 | 0 |
| 一般 | 1 |
| 轻微 | 2 |

### 总评

前端三端应用骨架（apps/patient, doctor, admin）整体符合 OOD §2.4 设计：index.html 挂载点正确（lang="zh-CN", div#app）、App.vue 正确渲染各端区分标题（患者端/医生端/管理员端）、main.ts 符合骨架规范、vite.config.ts 端口与代理配置正确（5173/5174/5175 → localhost:8080）、package.json 的 npm workspace 引用（@aimedical/shared、@aimedical/ui-core）正确。packages/shared 和 packages/ui-core 的包结构、导出入口与 OOD §2.4 一致。Phase 0 前端认证相关拦截逻辑（AuthStore/JWT）未启用，符合 §3.5 约定。主要问题在于 Axios 响应拦截器的 Result.code 拆包行为与 OOD §4.2 不一致——当前实现返回完整 ApiResponse<T> 而非仅内部 data，属于有意的设计分歧，需在设计文档或实现之间做一致性对齐。标题后缀与 axios 依赖位置属于轻微问题。
