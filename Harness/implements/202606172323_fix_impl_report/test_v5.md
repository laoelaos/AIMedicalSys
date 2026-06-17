# 测试报告（v5）

## 行为契约覆盖矩阵

| 行为契约 | 覆盖状态 | 对应测试 | 说明 |
|---------|---------|---------|------|
| §BC-1: `code === 'SUCCESS'` → 返回 `body.data`（类型 T） | COVERED | `unwraps SUCCESS response to body.data`、`unwraps nested data`、`unwraps array data` | 3 个正向用例覆盖对象、嵌套、数组 3 种 data 形态 |
| §BC-2: `code !== 'SUCCESS'` → 返回 `{ code, message, isBusinessError: true }` | COVERED | `returns BusinessError when code is not SUCCESS`、`returns BusinessError with empty message fallback` | 2 个用例覆盖正常 message 和 undefined message 两种场景 |
| §BC-3: HTTP/网络层异常 → 返回 `{ code, message, isBusinessError: true }` | COVERED | Error interceptor 7 个测试（NETWORK_ERROR、UNAUTHORIZED、FORBIDDEN、HTTP_ERROR×2、resolve 状态、shape 验证） | 覆盖所有 error 拦截器分支 + promise 形态 + 结构完整性 |
| §BC-4: 包装函数返回 `Promise<T \| BusinessError>` | COVERED | `apiGet returns result after interceptor unwrapping`、Integration 两个用例 | apiGet 验证拆包后 null；集成测试验证 SUCCESS 拆包和 error 返回 BusinessError；apiPost/Put/Delete 验证调用参数 |
| §BC-5: `ApiError` 仅保留于 `ApiResponse<T>` 中 | COVERED（隐式） | — | 无直接消费方测试；error 拦截器返回对象在结构上兼容 `BusinessError` 已隐式验证 |
| §BC-6: `ApiResult<T>` 保持不变 | COVERED（隐式） | — | 内部类型，无外部消费方 |

## 测试文件清单

| 文件路径 | 被测单元 | 测试维度 |
|---------|---------|---------|
| `AIMedical/frontend/packages/shared/src/api/__tests__/interceptors.test.ts` | success 拦截器 | 正常路径（SUCCESS 拆包 3 用例）+ 错误路径（非 SUCCESS→BusinessError 2 用例） |
| 同上 | error 拦截器 | 错误路径全覆盖（网络/401/403/HTTP 共 5 分支）+ 状态交互（promise resolve 形态 + shape 完整性共 2 用例） |
| 同上 | apiGet | 正常路径（调用参数 2 用例）+ 返回值形态（拆包后 null 1 用例） |
| 同上 | apiPost/apiPut/apiDelete | 正常路径（调用参数各 2~3 用例） |
| 同上 | 集成测试 | 端到端：success 拦截器拆包 + error 拦截器返回 BusinessError |

## 设计偏差说明

无偏差。实现与详细设计完全一致。

## 测试运行条件

所有测试当前可直接运行（不依赖外部服务、不修改源文件）。

| 测试范围 | 预期状态 | 依赖条件 |
|---------|---------|---------|
| Success interceptor（5 用例） | ✅ 通过 | 无（纯函数模拟） |
| Error interceptor（7 用例） | ✅ 通过 | 无（纯函数模拟） |
| apiGet/apiPost/apiPut/apiDelete（10 用例） | ✅ 通过 | 无（mock 层返回 null） |
| 集成测试（2 用例） | ✅ 通过 | 无（mock axios + 拦截器串联） |

## 未覆盖项及理由

- **`ApiResult<T>` 类型不变性验证**：该类型为 success 拦截器内部使用，不在公开接口暴露，无需单独测试
- **`BusinessError` 类型导出验证**：通过 `@aimedical/shared` 的 `export *` 自动传播，由 Package API 级别覆盖
- **包装函数返回类型编译期检查**：TypeScript 联合类型 `T | BusinessError` 由编译器保证，运行时测试无法验证类型
