# 详细设计（v6）

## 概述

修复前端 Axios 响应拦截器按 OOD §4.2 实现 `Result.code` 拆包逻辑（方案B：success 拦截器内直接处理），调整 `apiGet`/`apiPost`/`apiPut`/`apiDelete` 返回类型，同步更新测试文件。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `AIMedical/frontend/packages/shared/src/types/index.ts` | 修改 | 新增 `BusinessError` 类型 |
| `AIMedical/frontend/packages/shared/src/api/index.ts` | 修改 | success 拦截器增加 code 拆包检查；error 拦截器返回类型对齐；包装函数返回类型变更 |
| `AIMedical/frontend/packages/shared/src/api/__tests__/interceptors.test.ts` | 修改 | success interceptor 测试更新（拆包预期 + 新增非 SUCCESS 用例）；集成测试类型断言更新 |

## 类型定义

### BusinessError（新增）

**形态**：type alias
**包路径**：`@aimedical/shared/src/types`
**职责**：表示非 SUCCESS 的响应结果（包括业务错误码和网络/HTTP 错误）

```typescript
export type BusinessError = {
  code: string
  message: string
  isBusinessError?: true
}
```

**设计理由**：
- `code: string`（而非 narrow union）可承载任意业务错误码（如 `PATIENT_NOT_FOUND`、`AI_TIMEOUT`），而 `ApiError` 的 `code` 是 `'NETWORK_ERROR' | 'UNAUTHORIZED' | 'FORBIDDEN' | 'HTTP_ERROR'` 定长联合类型，无法表示业务码
- `ApiError` 在结构上是 `BusinessError` 的子类型（narrow union → `string`），因此 `Promise<T | BusinessError>` 可同时容纳 error interceptor 的 `ApiError` 对象和 success interceptor 返回的业务错误对象
- 添加 `isBusinessError?: true` 作为可选标记字段：消费者可通过 `'isBusinessError' in result` 在运行时区分返回值为成功数据还是 `BusinessError`

### 已有类型不变

- `ApiSuccess<T>`、`ApiError`、`ApiResponse<T>`、`ApiResult<T>`、`PageResponse<T>` 均保持原定义不变

## 文件变更规格

### `types/index.ts`

在 `PageResponse<T>` 定义之后追加：

```typescript
export type BusinessError = {
  code: string
  message: string
  isBusinessError?: true
}
```

### `api/index.ts`

**import 变更**：
- `import type { ApiResponse } from '../types'` → `import type { ApiResponse, BusinessError } from '../types'`
- 新增 `import type { ApiResult } from '../types'`（用于 success interceptor 内响应体类型断言）

**success 拦截器（`apiClient.interceptors.response.use` 的第一个回调）**：

```
旧实现：
  (response) => {
    return response.data as ApiResponse<unknown>
  }

新实现：
  (response) => {
    const body = response.data as ApiResult<unknown>
    if (body.code !== 'SUCCESS') {
      return ({ code: body.code, message: body.message ?? '', isBusinessError: true as const } as BusinessError) as unknown
    }
    return body.data
  }
```

要点：
- 使用 `ApiResult<unknown>` 断言而非 `ApiResponse<unknown>`：`ApiResult.code` 为 `string` 类型，与任意业务码兼容；`ApiResponse.code` 为 discriminated union，check 后无法直接索引 `body.data`（需类型守卫）
- `code !== 'SUCCESS'` 时通过 `as unknown` 绕过 TypeScript 的 interceptor 类型推断限制（Axios 拦截器回调期望返回 `AxiosResponse | Promise<AxiosResponse>`，实际运行时 interceptor pipeline 不做类型校验）
- `body.message ?? ''` 处理 `message` 可选字段的 undefined 情况
- **直接返回 BusinessError 对象（同步），不经过 `Promise.resolve()` 包装**，与 SUCCESS 分支 `return body.data` 的同步行为一致

**error 拦截器（第二个回调）**：

各分支返回值中追加 `isBusinessError: true as const`，以便消费者通过 `'isBusinessError' in result` 统一区分成功数据与错误（无论错误来源是 success 拦截器还是 error 拦截器）。各分支原有的 `as const` 断言仍保留以保持 `ApiError` 字面量推断。

**包装函数返回类型变更**：

```
apiGet<T>(url, config?) :  Promise<ApiResponse<T>>   →   Promise<T | BusinessError>
apiPost<T>(url, data?, config?) :  Promise<ApiResponse<T>>   →   Promise<T | BusinessError>
apiPut<T>(url, data?, config?) :  Promise<ApiResponse<T>>   →   Promise<T | BusinessError>
apiDelete<T>(url, config?) :  Promise<ApiResponse<T>>   →   Promise<T | BusinessError>
```

实现体中的 `as unknown as Promise<...>` 断言一并更新为新的返回类型。

### `interceptors.test.ts`

**mock 层变更**（`vi.mock('axios', ...)` 内）：

`get`/`post`/`put`/`delete` 的返回值从 `{ code: 'SUCCESS', data: null }` 改为 `null`，以模拟 interceptor 拆包后的输出（SUCCESS + null data → 拆包后为 null）。

**Success interceptor 测试（3 个现有用例）**：

每个用例验证：
- mock response: `{ data: { code: 'SUCCESS', data: { ... } } }`
- handler 返回值应当是 `body.data`（拆包后的业务数据）

更新后的测试结构：
```typescript
describe('Success interceptor', () => {
  it('unwraps SUCCESS response to body.data', () => {
    const handler = captured.success!
    const mockResponse = { data: { code: 'SUCCESS', data: { id: 1 } } }
    const result = handler(mockResponse)
    expect(result).toEqual({ id: 1 })
  })

  it('unwraps nested data from SUCCESS response', () => {
    const handler = captured.success!
    const mockResponse = { data: { code: 'SUCCESS', data: { nested: 'value' } } }
    const result = handler(mockResponse)
    expect(result).toEqual({ nested: 'value' })
  })

  it('unwraps array data from SUCCESS response', () => {
    const handler = captured.success!
    const mockResponse = { data: { code: 'SUCCESS', data: ['a', 'b'] } }
    const result = handler(mockResponse)
    expect(result).toEqual(['a', 'b'])
  })
})
```

**新增 Success interceptor 非 SUCCESS 测试（2 个用例）**：

```typescript
it('returns BusinessError when code is not SUCCESS', () => {
  const handler = captured.success!
  const mockResponse = { data: { code: 'BUSINESS_ERROR', message: '业务异常' } }
  const result = handler(mockResponse)
  expect(result).toEqual({ code: 'BUSINESS_ERROR', message: '业务异常', isBusinessError: true })
})

it('returns BusinessError with empty message fallback when message is undefined', () => {
  const handler = captured.success!
  const mockResponse = { data: { code: 'UNKNOWN_ERROR' } }
  const result = handler(mockResponse)
  expect(result).toEqual({ code: 'UNKNOWN_ERROR', message: '', isBusinessError: true })
})
```

**Error interceptor 测试（7 个现有用例）**：

无需逻辑变更。现有断言（验证 `{ code, message }` 结构）在返回类型变为 `BusinessError` 后仍然有效。

**apiGet 测试**：

- "returns ApiResponse shaped object" → "returns result after interceptor unwrapping"
- 断言从 `expect(result).toHaveProperty('code')` 改为 `expect(result).toBeNull()`（mock 的 `get` 现在返回 `null` 模拟拆包后 null data）

**apiPost/apiPut/apiDelete 测试**：

无需变更逻辑（仅验证调用参数，不验证返回值形状）。

**集成测试（Integration）**：

`apiGet response is processed by success interceptor`：
- 旧断言：`expect(result).toEqual({ code: 'SUCCESS', data: { id: 1 } })`
- 新断言：`expect(result).toEqual({ id: 1 })`（SUCCESS 分支拆包后返回 body.data）

`apiGet error is processed by error interceptor`：
- 无需变更（仍返回 `{ code: 'NETWORK_ERROR', message: '...' }`，结构上兼容 `BusinessError`）

## 错误处理

- 业务错误码（HTTP 200 + `code !== 'SUCCESS'`）：由 success 拦截器直接返回 `BusinessError` 对象，不经过 error 拦截器
- 网络/HTTP 错误：仍由 error 拦截器处理，返回 `BusinessError` 兼容对象
- consumer 侧通过 `'isBusinessError' in result` 在运行时区分成功数据与 `BusinessError`；TypeScript 层面通过 `Promise<T | BusinessError>` 联合类型提供编译期守卫

## 行为契约

1. `response.data.code === 'SUCCESS'` → success 拦截器返回 `response.data.data`（类型 `T`）
2. `response.data.code !== 'SUCCESS'` → success 拦截器返回 `{ code, message, isBusinessError: true }`（类型 `BusinessError`）
3. HTTP/网络层异常 → error 拦截器返回 `{ code, message, isBusinessError: true }`（类型 `BusinessError`，对象结构与 `ApiError` 一致）
4. 所有包装函数（`apiGet`/`apiPost`/`apiPut`/`apiDelete`）的返回类型统一为 `Promise<T | BusinessError>`
5. `ApiError` 类型仅作为 discriminated union 的成员保留于 `ApiResponse<T>` 中，不再作为包装函数的直接返回类型
6. `ApiResult<T>` 类型保持不变，作为后端原始响应体的合约类型

## 依赖关系

- 新增运行时依赖：无
- 新增测试依赖：无
- 暴露给调用方的接口变化：`@aimedical/shared` 导出新增 `BusinessError` 类型；四个包装函数返回类型变更；`BusinessError` 通过 `src/index.ts` 的 `export * from './types'` 自动传播
- 冲击面：代码库中零调用方（已通过 impl_report T6 确认），仅测试文件需同步更新

## 修订说明（v5 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| `instanceof BusinessError` 运行时不可用（`type` 别名编译后擦除） | "错误处理"节删除 `instanceof` 推荐，改用 `'isBusinessError' in result` 运行时可行方案 |
| `BusinessError` 缺少 `isBusinessError` 标记字段，偏离任务要求 | 在 `BusinessError` 类型定义中增加 `isBusinessError?: true` 可选标记字段；同步更新设计理由说明 |

## 修订说明（v5 r2）
| 审查意见 | 修改措施 |
|---------|---------|
| success 拦截器 non-SUCCESS 返回值未实际设置 `isBusinessError` 属性，`'isBusinessError' in result` 将始终返回 `false` | success 拦截器返回对象追加 `isBusinessError: true as const`；同步更新测试断言（第143行、第150行） |
| （可选）error 拦截器返回值是否应包含 `isBusinessError: true` | 采纳。error 拦截器各分支返回值中追加 `isBusinessError: true as const`，使消费者可通过统一方式 `'isBusinessError' in result` 区分成功数据与任何来源的错误 |

## 修订说明（v6 r1 — 验证反馈修复）
| 审查意见 | 修改措施 |
|---------|---------|
| verify FAILED：2 个 non-SUCCESS 测试因 handler 返回 Promise 而非 BusinessError 对象导致测试失败（实际原因：code_v5 实现使用了 `Promise.resolve()` 包装，与设计规格不符） | success 拦截器 non-SUCCESS 分支移除 `Promise.resolve()` 包装，直接返回 BusinessError 对象，与 SUCCESS 分支 `return body.data` 的同步行为一致。**设计不变，仅要求编码阶段严格执行设计规格中"不经过 Promise.resolve() 包装"的约束** |

## 修订说明（v6 r2 — RETRY 确认）
| 审查意见 | 修改措施 |
|---------|---------|
| verify_v5 FAILED：code_v5 实现未遵循设计规格——success 拦截器 non-SUCCESS 分支仍使用了 `Promise.resolve()` 包装 | **设计无需修改**：detail_v5 已正确指定直接同步返回 BusinessError 对象。本 v6 RETRY 仅确认设计保持不变，编码阶段必须严格按设计规格第82行"直接返回 BusinessError 对象（同步），不经过 Promise.resolve() 包装"执行，即 line 14 从 `return (Promise.resolve({...}) as BusinessError) as unknown` 改为 `return ({...} as BusinessError) as unknown` |
