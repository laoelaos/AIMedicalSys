# 任务指令（v5）

## 动作
NEW

## 任务描述
修复 frontend Axios 响应拦截器，按 OOD §4.2 实现 Result.code 拆包逻辑（方案B：success 拦截器内直接处理），调整 apiGet/apiPost/apiPut/apiDelete 返回类型，同步更新测试文件。

预期文件变更：
| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 修改 | `AIMedical/frontend/packages/shared/src/api/index.ts` | success 拦截器增加 code 检查拆包；error 拦截器增加业务错误区分逻辑；包装函数返回类型从 `Promise<ApiResponse<T>>` 调整为 `Promise<T \| ApiError>` |
| 修改 | `AIMedical/frontend/packages/shared/src/types/index.ts` | 新增 `BusinessError` 类型（含 `isBusinessError` 标记）或复用 `ApiError` |
| 修改 | `AIMedical/frontend/packages/shared/src/api/__tests__/interceptors.test.ts` | 新增 code !== "SUCCESS" 测试；调整集成测试返回类型断言 |

## 选择理由
T6 为剩余任务中唯一高优先级项（功能安全）；所有 POM/结构基础修复已完成；与后端任务（T10/T11）无依赖关系；当前零调用方冲击面，修复窗口期仍在开放。

## 任务上下文

### OOD §4.2 规定
- `response.data.code === "SUCCESS"` → 返回 `response.data.data`
- `response.data.code !== "SUCCESS"` → 走错误处理

### 当前代码行为（index.ts:10-26）
- success 拦截器直接 `return response.data as ApiResponse<unknown>` — 未检查 code 字段
- error 拦截器覆盖 NETWORK_ERROR/UNAUTHORIZED/FORBIDDEN/HTTP_ERROR，但业务错误码（HTTP 200 + code !== "SUCCESS"）不会进入 error 拦截器

### 推荐方案（方案B）
在 success 拦截器内检查 `code !== "SUCCESS"` 并直接返回业务错误对象，不经过 error 拦截器。改动局部、风险可控。

### 返回类型变更
- `apiGet<T>(...)`: `Promise<ApiResponse<T>>` → `Promise<T | ApiError>`
- SUCCESS 分支返回 `body.data`（类型 T）
- 非 SUCCESS 分支返回 `{ code, message }`（类型 ApiError）

### 已有类型定义
```typescript
// types/index.ts
export type ApiSuccess<T> = { code: 'SUCCESS'; data: T }
export type ApiError = {
  code: 'NETWORK_ERROR' | 'UNAUTHORIZED' | 'FORBIDDEN' | 'HTTP_ERROR'
  message: string
}
export type ApiResponse<T> = ApiSuccess<T> | ApiError
export interface ApiResult<T = unknown> {
  code: string
  message?: string
  data?: T
}
```

### 已有测试文件
`interceptors.test.ts` 包含：
- Success interceptor: 3 个用例（均验证返回完整 ApiResponse，不含拆包检查）
- Error interceptor: 7 个用例（NETWORK_ERROR/UNAUTHORIZED/FORBIDDEN/HTTP_ERROR）
- apiGet/apiPost/apiPut/apiDelete: 各 2-3 个用例验证调用
- Integration: 2 个用例（apiGet 经 success/error 拦截器处理）

## 验证方式
1. `npx vitest run packages/shared/src/api/__tests__/interceptors.test.ts` 全部通过
2. 前端 lint 通过（项目配置的 lint 命令）
