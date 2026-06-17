# 测试报告（v6 r1）

## 测试文件清单
| 测试文件 | 被测模块 |
|---------|---------|
| `AIMedical/frontend/packages/shared/src/types/__tests__/types.test.ts` | `types/index.ts` (ApiSuccess\<T\>, ApiError, ApiResponse\<T\>,  discriminated union) |
| `AIMedical/frontend/packages/shared/src/api/__tests__/interceptors.test.ts` | `api/index.ts` (成功拦截器, 错误拦截器, apiGet/apiPost/apiPut/apiDelete 包装函数) |

## 新增配置文件
| 文件 | 用途 |
|-----|------|
| `AIMedical/frontend/packages/shared/vitest.config.ts` | Vitest 测试框架配置，环境 node，开启 globals |

## 运行依赖
测试框架使用 **vitest**，运行前需在 `packages/shared` 目录下执行：
```
npm install -D vitest
```

## 用例覆盖

### types/index.ts（7 个用例）
- **ApiSuccess\<T\>**:
  - 正向：创建含 data 的 SUCCESS 分支
  - 边界：支持不同 data 类型（string / number / object）
- **ApiError**:
  - 正向：NETWORK_ERROR / UNAUTHORIZED / FORBIDDEN / HTTP_ERROR 四个分支各自可创建
  - 约束：code 字段保留字面量类型（`as const` 断言）
- **ApiResponse\<T\> 判别式联合**:
  - 正向：通过 `res.code === 'SUCCESS'` 收窄获得 data
  - 正向：通过 `res.code !== 'SUCCESS'` 收窄获得 message
  - 覆盖：非 SUCCESS 各分支（UNAUTHORIZED / FORBIDDEN / HTTP_ERROR）均可正确收窄
- **ApiResult 向后兼容**：旧接口格式仍可用（接口已保留）

### api/index.ts（19 个用例）
- **成功拦截器**（3 个用例）：
  - 正向：返回 `response.data`（含 SUCCESS 和 data）
  - 正向：不额外展开嵌套 data（`response.data.data` 不变）
  - 边界：透传数组 data
- **错误拦截器**（5+2 个用例）：
  - 正向：`error.response === undefined` → `{ code: 'NETWORK_ERROR', message: '网络不可达，请检查网络连接' }`
  - 正向：401 → `{ code: 'UNAUTHORIZED', message: '登录已过期，请重新登录' }`
  - 正向：403 → `{ code: 'FORBIDDEN', message: '无权限访问' }`
  - 正向：500 → `{ code: 'HTTP_ERROR', message: '请求失败（500）' }`
  - 边界：404（非 401/403 的 HTTP 错误） → `{ code: 'HTTP_ERROR', message: '请求失败（404）' }`
  - 行为：所有分支返回 resolved promise（不 reject）
  - 契约：所有分支返回的对象具有 `code` 和 `message` 属性
- **包装函数**（9 个用例）：
  - `apiGet`：调用 `apiClient.get(url)` / 传递 config / 返回含 code 的对象
  - `apiPost`：调用 `apiClient.post(url, data)` / 传递 config / 无 data 时传 undefined
  - `apiPut`：调用 `apiClient.put(url, data)` / 传递 config
  - `apiDelete`：调用 `apiClient.delete(url)` / 传递 config

## 约束满足检查
- [x] 基于行为契约编写，不测实现细节
- [x] 每个行为契约至少一个正向用例
- [x] 覆盖维度：正常路径、边界条件、错误路径、状态交互
- [x] 测试文件放入项目约定目录（`__tests__/` 目录，Vitest 默认发现）
- [x] 每个被测类型对应一个测试文件（types 一个文件、api 一个文件）
- [x] 用例独立，不依赖执行顺序
- [x] 未修改编码 agent 的源码文件（types/index.ts、api/index.ts 未修改）

## 修订说明（审查反馈处理）

| # | 审查意见 | 处理 | 说明 |
|---|---------|------|------|
| 1 | `ApiResult` 向后兼容测试未实际引用 `ApiResult` 类型 | ✅ 已修复 | 导入 `ApiResult` 类型并用于测试变量声明 |
| 2 | 测试名 "rejects invalid codes at compile time" 有误导性 | ✅ 已修复 | 重命名为 "uses as const assertion for literal type preservation" |
| 3 | 缺少包装函数与拦截器的集成测试 | ✅ 已修复 | 新增 `Integration: wrapper functions & interceptors` 测试套件，覆盖成功路径和错误路径 |
| 4 | `captured` 对象在各测试套件间不被重置 | ❌ 未采纳 | 重置 `captured` 会破坏所有测试（handlers 仅在模块加载时设置一次）；当前无测试修改 `captured`，跨测试污染风险为理论值；需保持 `beforeEach` 仅调用 `vi.clearAllMocks()` |

## 设计偏差说明
无偏差。测试覆盖范围与详细设计 v6 r1 定义的行为契约完全对齐。
