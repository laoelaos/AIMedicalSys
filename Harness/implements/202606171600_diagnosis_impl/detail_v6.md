# 详细设计（v6 r1）

## 概述
修复 ApiClient 错误拦截器未按 OOD §3.5 实现 NETWORK_ERROR 处理的问题。修改 `api/index.ts` 的错误/成功拦截器，在 `types/index.ts` 中新增 discriminated union 类型方案，并新增带类型的请求包装函数解决消费者类型获取问题。

## 文件规划
| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `AIMedical/frontend/packages/shared/src/api/index.ts` | 修改 | 错误拦截器统一格式转换（Promise.resolve），成功拦截器返回对齐，新增带类型的请求包装函数 |
| `AIMedical/frontend/packages/shared/src/types/index.ts` | 修改 | 新增 ApiSuccess\<T\>、ApiError、ApiResponse\<T\> 类型 |

## 类型定义

### ApiSuccess\<T\>
**形态**：type alias
**包路径**：`src/types/index.ts`
**职责**：成功响应的 discriminated union 分支
```typescript
export type ApiSuccess<T> = { code: 'SUCCESS'; data: T }
```

### ApiError
**形态**：type alias
**包路径**：`src/types/index.ts`
**职责**：错误响应的 discriminated union 分支，覆盖 4 种错误场景
```typescript
export type ApiError = {
  code: 'NETWORK_ERROR' | 'UNAUTHORIZED' | 'FORBIDDEN' | 'HTTP_ERROR'
  message: string
}
```

### ApiResponse\<T\>
**形态**：type alias
**包路径**：`src/types/index.ts`
**职责**：统一的响应联合类型，消费方通过 `if (res.code === 'SUCCESS')` 判别式缩小类型
```typescript
export type ApiResponse<T> = ApiSuccess<T> | ApiError
```

### ApiResult\<T\>（已有接口，保留）
**形态**：interface
**包路径**：`src/types/index.ts`
**职责**：兼容历史引用，不作修改
```typescript
export interface ApiResult<T = unknown> {
  code: string
  message?: string
  data?: T
}
```

### PageResponse\<T\>（已有接口，保留）
**形态**：interface
**包路径**：`src/types/index.ts`
**职责**：分页响应类型，不作修改

## api/index.ts 设计

### 已有行为
当前 `apiClient` 实例已创建，导出 `{ apiClient }`。

### 成功拦截器
- 输入：Axios `response` 对象
- 输出：`return response.data as ApiResponse<unknown>`
- 不再执行 `if (response.data?.code === 'SUCCESS')` 条件判断
- 运行时 `response.data` 已是 `ApiResponse<T>` 形状（后端返回格式），直接透传

### 错误拦截器（二参）
- 输入：Axios `error` 对象
- 输出：所有分支统一使用 `Promise.resolve()`，不抛出 reject
- **约束：每个分支的返回对象在 `code` 字段上必须保留字面量类型**，具体通过 `as const` 断言实现，否则 TypeScript 将 `code` 拓宽为 `string`，导致消费方 discriminated union 类型收窄失效

错误分支映射：
| 条件 | code | message |
|------|------|---------|
| `error.response === undefined` | `'NETWORK_ERROR' as const` | `'网络不可达，请检查网络连接'` |
| `error.response.status === 401` | `'UNAUTHORIZED' as const` | `'登录已过期，请重新登录'` |
| `error.response.status === 403` | `'FORBIDDEN' as const` | `'无权限访问'` |
| 其他 HTTP 错误 | `'HTTP_ERROR' as const` | `` `请求失败（${status}）` `` |

### 新增：带类型的请求包装函数
**问题**：Axios 泛型 `apiClient.get<T>(url)` 返回 `Promise<T>`（`T` 为响应数据类型，Axios 未感知拦截器对返回值的变换），消费方无法通过标准 Axios 调用获得 `Promise<ApiResponse<T>>` 类型。

**方案**：在 `api/index.ts` 中新增一组包装函数，显式返回 `Promise<ApiResponse<T>>`。运行时依赖拦截器已完成格式转换，类型层面通过 `as unknown as Promise<ApiResponse<T>>` 跨类型断言实现。

**函数签名定义**：
```typescript
export async function apiGet<T>(url: string, config?: AxiosRequestConfig): Promise<ApiResponse<T>>
export async function apiPost<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<ApiResponse<T>>
export async function apiPut<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<ApiResponse<T>>
export async function apiDelete<T>(url: string, config?: AxiosRequestConfig): Promise<ApiResponse<T>>
```

**行为**：
- 每个函数内部调用 `apiClient` 对应方法，将返回值 `as unknown as Promise<ApiResponse<T>>`
- 运行时执行流程：`apiClient.get/post/put/delete` → Axios 内部调用拦截器 → 拦截器返回 `ApiResponse<T>`（成功时 `ApiSuccess<T>`，错误时 `ApiError`）→ 包装函数返回 `Promise<ApiResponse<T>>`
- 消费方获取结果后通过 `if (res.code === 'SUCCESS')` 判别式自动缩小类型获得 `data` 字段

**消费方使用示例**：
```typescript
import { apiGet } from '@aimedical/shared'

interface User { id: number; name: string }
const res = await apiGet<User>('/user/1')
if (res.code === 'SUCCESS') {
  // res.data 在此分支自动收窄为 User
  console.log(res.data.name)
} else {
  // res.code 在此分支自动收窄为 'NETWORK_ERROR' | 'UNAUTHORIZED' | 'FORBIDDEN' | 'HTTP_ERROR'
  console.error(res.message)
}
```

### 导入调整
`api/index.ts` 新增 import：
```typescript
import { AxiosRequestConfig } from 'axios'
import type { ApiResponse } from '../types'
```

## 错误处理
- 错误拦截器内所有分支统一使用 `Promise.resolve()`，不抛出异常或 reject
- 网络错误（`error.response === undefined`）：返回 `ApiError`（code=`NETWORK_ERROR`）
- HTTP 401：返回 `ApiError`（code=`UNAUTHORIZED`）
- HTTP 403：返回 `ApiError`（code=`FORBIDDEN`）
- 其他 HTTP 错误：返回 `ApiError`（code=`HTTP_ERROR`）
- 消费方通过响应码统一处理，不再通过 `catch` 捕获异常

## 行为契约
### 拦截器契约
- 成功拦截器返回 `response.data` 而非 `response.data.data`，不再参与数据解构
- 错误拦截器不再透传原始 Axios 错误，所有分支返回格式一致的 `{ code, message }` 对象
- 错误拦截器返回对象的 `code` 字段必须使用 `as const` 保留字面量类型

### types/index.ts 导出契约
- `ApiSuccess<T>`、`ApiError`、`ApiResponse<T>` 通过 `src/index.ts` 的 `export * from './types'` 自动暴露
- `ApiResult<T>` 保留不变，已存在的消费方代码（如有）无需迁移

### 包装函数契约
- 包装函数是消费者发起 API 调用的**推荐方式**
- `apiClient` 仍作为命名导出保留，但不推荐直接用于调用（类型不匹配）
- 每个包装函数继承对应 Axios 方法的参数签名，仅返回类型替换为 `Promise<ApiResponse<T>>`

### 兼容性约束
- 不修改任何消费方代码（当前无实际 Axios 调用的前端页面）
- `ApiResult<T>` 保留不变，不做删除或修改
- `PageResponse<T>` 保留不变

## 依赖关系
- 依赖的已有类型：`axios`（package.json devDependencies 已有）、`AxiosRequestConfig`
- 暴露给后续任务：`ApiResponse<T>`、`apiGet<T>`、`apiPost<T>`、`apiPut<T>`、`apiDelete<T>`
- 无新增外部依赖

## 修订说明（v6 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| [一般] 未指定拦截器返回值保留字面量类型的约束 | 在错误拦截器设计中增加约束：返回对象的 `code` 字段必须使用 `as const` 断言保留字面量类型；在错误处理和行为契约中分别补充此约束 |
| [一般] 未定义消费者如何从 Axios 泛型获取 ApiResponse\<T\> 类型 | 在 `api/index.ts` 中新增 4 个带类型的请求包装函数（apiGet/apiPost/apiPut/apiDelete），通过 `as unknown as Promise<ApiResponse<T>>` 断言提供正确的消费者类型；增加包装函数契约章节，明确推荐用法和消费方代码示例 |
