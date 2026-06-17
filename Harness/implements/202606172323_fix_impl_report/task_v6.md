# 任务指令（v6）

## 动作
RETRY

## 任务描述
修复 Axios 响应拦截器 Result.code 拆包逻辑（T6），移除 success 拦截器 non-SUCCESS 分支的 `Promise.resolve()` 包装，使返回值保持同步以对齐 SUCCESS 分支行为。

预期文件路径：
- `AIMedical/frontend/packages/shared/src/api/index.ts` — success 拦截器 non-SUCCESS 分支

## 选择理由
R5 实现已完成编码和测试验证，但 verify FAILED — 2 个 non-SUCCESS 分支测试因 handler 返回 Promise 而测试未 await 导致失败。`Promise.resolve()` 包装在 Axios 拦截器中并非必要（运行时接受同步返回值），且与 SUCCESS 分支 `return body.data` 的同步行为不一致。移除后两分支行为一致，无需修改测试即可通过。

## 任务上下文

### 失败的测试用例
1. `returns BusinessError when code is not SUCCESS` — `expected Promise{} to deeply equal { code: 'BUSINESS_ERROR', ... }`
2. `returns BusinessError with empty message fallback when message is undefined` — `expected Promise{} to deeply equal { code: 'UNKNOWN_ERROR', ... }`

原因：success 拦截器 non-SUCCESS 分支返回 `Promise.resolve({...})`，测试直接调用 handler 未 await 获得 Promise 对象而非 BusinessError。

### 当前代码（api/index.ts:13-14）
```typescript
if (body.code !== 'SUCCESS') {
  return (Promise.resolve({ code: body.code, message: body.message ?? '', isBusinessError: true as const }) as BusinessError) as unknown
}
```

### 目标代码
```typescript
if (body.code !== 'SUCCESS') {
  return ({ code: body.code, message: body.message ?? '', isBusinessError: true as const } as BusinessError) as unknown
}
```

### 详细设计
详见 `detail_v5.md` — 整体设计不变，仅移除 `Promise.resolve()` 包装层。

### 测试说明
测试文件 `interceptors.test.ts` 无需修改，现有 non-SUCCESS 测试用例（第58-70行）直接断言 handler 同步返回值即可通过。

## RETRY 说明
- 失败原因：success 拦截器 non-SUCCESS 分支使用 `Promise.resolve()` 包装返回值，与 SUCCESS 分支的同步返回行为不一致；测试以同步方式捕获 handler 返回值，收到 Promise 而非 BusinessError
- 修正方向：仅移除 `Promise.resolve()` 包装，直接返回 BusinessError 对象；测试文件无需变更
