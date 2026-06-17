# R{A3}: 前端骨架 + 文档 + 项目配置 — OOD 设计一致性审查

审查时间：2026-06-18

### 审查范围

**前端 Monorepo 根配置：**
- `AIMedical/frontend/package.json`
- `AIMedical/frontend/tsconfig.base.json`
- `AIMedical/frontend/.gitignore`

**前端共享包 (packages/)：**
- `packages/shared/package.json`
- `packages/shared/src/index.ts`
- `packages/shared/src/api/index.ts`
- `packages/shared/src/api/__tests__/interceptors.test.ts`
- `packages/shared/src/types/index.ts`
- `packages/shared/src/types/__tests__/types.test.ts`
- `packages/shared/src/utils/index.ts`
- `packages/shared/vitest.config.ts`
- `packages/ui-core/package.json`
- `packages/ui-core/src/index.ts`
- `packages/ui-core/src/components/index.ts`

**前端三端应用 (apps/)：**
- `apps/patient/package.json`, `index.html`, `src/main.ts`, `src/App.vue`, `vite.config.ts`, `tsconfig.json`, `env.d.ts`
- `apps/doctor/package.json`, `index.html`, `src/main.ts`, `src/App.vue`, `vite.config.ts`, `tsconfig.json`, `env.d.ts`
- `apps/admin/package.json`, `index.html`, `src/main.ts`, `src/App.vue`, `vite.config.ts`, `tsconfig.json`, `env.d.ts`

**文档与配置：**
- `Docs/04_ood_phase0.md`
- `Docs/QUICKSTART.md`
- `CONTRIBUTING.md`
- `.github/pull_request_template.md`
- `.gitignore`

### 发现

#### [轻微] Axios 响应拦截器使用 `as unknown` 绕过类型检查

- **位置**：`AIMedical/frontend/packages/shared/src/api/index.ts:14`
- **描述**：success 拦截器和 apiGet/Post/Put/Delete 函数均通过 `as unknown` 双重转型对接 Axios 类型系统和业务类型签名。例如第 14 行将 BusinessError 对象转型为 `as BusinessError as unknown`，第 33 行 `apiClient.get(url, config) as unknown as Promise<T | BusinessError>`。运行时行为正确，但这些转型完全绕过 TypeScript 的类型检查，若后续修改返回形状，编译器不会发出警告。
- **建议**：可考虑将 Axios 实例的类型声明为 `AxiosInstance<BusinessError | T>` 或在 wrapper 层使用类型守卫函数收敛转型点，使类型检查在单一位置生效而不是散布到每个 API 函数。

#### [轻微] frontend/.gitignore 内容与根 .gitignore 大量重复

- **位置**：`AIMedical/frontend/.gitignore`
- **描述**：frontend 级 `.gitignore` 中的 `node_modules/` 和 `dist/` 已在根 `.gitignore` 中以全局模式覆盖（`AIMedicalSys/.gitignore:66-68`）。同样，`.env.local` 也被根级的 `.env.*` 全局模式覆盖。仅 `*.tsbuildinfo` 是 frontend 独有的新增项。
- **建议**：可精简为仅保留 `*.tsbuildinfo`；保留重复项不影响功能，但会轻微降低可维护性。

### 本轮统计

| 严重程度 | 数量 |
|---------|------|
| 严重 | 0 |
| 一般 | 0 |
| 轻微 | 2 |

### 总评

前端骨架、文档和项目配置的实现与 OOD §2.4、§3.5、§9.3、§11.1、§11.2 的设计高度一致。所有关键检查项均通过：

| 检查项 | OOD 要求 | 实现 | 结果 |
|--------|---------|------|------|
| npm workspaces | `["packages/*", "apps/*"]` (§2.4) | ✅ 一致 | ✅ |
| 内部包名 | `@aimedical/shared`, `@aimedical/ui-core`, `@aimedical/app-{端名}` (§2.4) | ✅ 全部正确 | ✅ |
| workspace:* 依赖 | shared←ui-core←三端 | ✅ 链式引用正确 | ✅ |
| Vite 端口 | patient=5173, doctor=5174, admin=5175 (§9.3) | ✅ 三端端口正确 | ✅ |
| Vite proxy /api | target localhost:8080, changeOrigin:true (§9.3) | ✅ 三端配置一致 | ✅ |
| index.html lang/title | zh-CN / "智慧云脑诊疗平台 - {端名}" (§2.4) | ✅ 三端均正确 | ✅ |
| App.vue 占位 | 系统名称 + 占位提示 (§2.4) | ✅ 三端均正确 | ✅ |
| shared 导出三子模块 | api/, types/, utils/ (§2.4) | ✅ 正确导出 | ✅ |
| ui-core 依赖 shared | `"workspace:*"` (§2.4) | ✅ 正确声明 | ✅ |
| Axios 拦截器 | 检查 Result.code，非 SUCCESS 走错误处理 (§3.5) | ✅ 逻辑正确 | ✅ |
| QUICKSTART.md 完整度 | 前置条件、命令序列、验证步骤、FAQ (§11.2) | ✅ 全部覆盖 | ✅ |
| CONTRIBUTING.md 覆盖度 | 分支、Commit、PR、CR 必查项 (§11.1) | ✅ 全部覆盖 | ✅ |
| PR 模板 | 含 Summary/Scope/Verification/OOD Impact/Checklist | ✅ 符合规范 | ✅ |

测试方面，`interceptors.test.ts` 对 Axios 拦截器的成功/错误/分支测试覆盖完整（14 个 it），`types.test.ts` 对类型系统的 discriminated union 分支覆盖充分（11 个 it），并且包含了 wrapper 函数与拦截器的集成测试。代码质量良好，与当前 Phase 0 冻结边界一致。
