# 任务指令（v6）

## 动作
NEW

## 任务描述
修复问题10：ApiClient 错误拦截器未按 OOD §3.5 实现 NETWORK_ERROR 处理。

**修改文件1**：`AIMedical/frontend/packages/shared/src/api/index.ts`
- 将错误拦截器从 `return Promise.reject(error)` 改为统一格式转换，所有分支使用 `Promise.resolve()`
- 网络错误（`error.response === undefined`）：`{ code: 'NETWORK_ERROR', message: '网络不可达，请检查网络连接' }`
- HTTP 401：`{ code: 'UNAUTHORIZED', message: '登录已过期，请重新登录' }`
- HTTP 403：`{ code: 'FORBIDDEN', message: '无权限访问' }`
- 其他 HTTP 错误：`{ code: 'HTTP_ERROR', message: '请求失败（${status}）' }`
- 成功拦截器：将 `return response.data.data` 改为 `return response.data`（对齐 discriminated union 类型方案）

**修改文件2**：`AIMedical/frontend/packages/shared/src/types/index.ts`
- 新增 `ApiSuccess<T> = { code: 'SUCCESS'; data: T }` 类型
- 新增 `ApiError = { code: 'NETWORK_ERROR' | 'UNAUTHORIZED' | 'FORBIDDEN' | 'HTTP_ERROR'; message: string }` 类型
- 新增 `ApiResponse<T> = ApiSuccess<T> | ApiError` 联合类型
- 保留 `ApiResult<T>`（兼容历史引用）和 `PageResponse<T>`

**关键约束**：
- 所有错误分支统一使用 `Promise.resolve()`，不抛出 `reject`，保持与成功拦截器一致的返回格式
- 消费方通过 `if (res.code === 'SUCCESS')` 判别式自动缩小类型获取 data 字段
- 不修改任何消费方代码（当前无实际 Axios 调用的前端页面）

## 选择理由
最后一项待办事项。问题10 为唯一剩余的真实代码缺陷（类型①），前后端完全解耦，不依赖此前任何后端/POM 任务。OOD §3.5 和诊断报告用户意见已给出明确的实现方向和 TypeScript 类型方案。

## 任务上下文
### 问题诊断
OOD §3.5 明确要求 Axios 错误拦截器统一捕获网络错误（DNS 解析失败、连接超时、请求被取消等），返回格式 `{ code: "NETWORK_ERROR", message: "网络不可达，请检查网络连接" }`。当前 `api/index.ts:16-18` 的二参错误拦截器仅执行 `return Promise.reject(error)`，直接透传原始 Axios 错误，未做任何格式转换。

### 用户意见
- 所有错误分支统一使用 `Promise.resolve()`
- TypeScript 类型方案：方案A discriminated union
- 成功拦截器统一返回 `ApiResponse<T>`（将现有 `return response.data.data` 改为 `return response.data` 与类型对齐）

### 已知类型不一致（修复前既有问题）
当前成功拦截器第12行 `return response.data.data` 返回 `T`，第14行 `return response.data` 返回 `ApiResult<T>`。本修复需统一为 `ApiResponse<T>`。

## 已有代码上下文
### 当前 api/index.ts
```typescript
import axios from 'axios'

const apiClient = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' },
})

apiClient.interceptors.response.use(
  (response) => {
    if (response.data?.code === 'SUCCESS') {
      return response.data.data
    }
    return response.data
  },
  (error) => {
    return Promise.reject(error)
  },
)

export { apiClient }
```

### 当前 types/index.ts
```typescript
export interface ApiResult<T = unknown> {
  code: string
  message?: string
  data?: T
}

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  page: number
  size: number
}
```
