# 设计审查报告（v5 r1）

## 审查结果
REJECTED

## 发现

- **[一般]** `instanceof BusinessError` 运行时不可用。`BusinessError` 定义为 `type` alias，TypeScript 类型别名在编译后完全擦除，`instanceof BusinessError` 在运行时会导致 `ReferenceError`。"错误处理" 节（L178）推荐消费者通过该方式区分成功与错误分支，属于错误的类型认知，若被编码将引发运行时崩溃。应当移除 `instanceof` 用法，或改用 `'message' in result` 等运行时可行方案。

- **[一般]** `BusinessError` 缺少 `isBusinessError` 标记字段，偏离任务文件预期。任务文件（task_v5.md L13）明确要求 `BusinessError` "含 `isBusinessError` 标记"。设计选择了选项 C（新增 `BusinessError` 但不含标记），既不等同于"复用 `ApiError`"，也未满足"含标记"约束。虽提供了合理性说明（方案 B 下路径天然隔离），但作为设计规格应对齐任务要求。

## 修改要求（仅 REJECTED 时）

1. **`instanceof BusinessError` 问题**："错误处理"节 L178 删除 `result instanceof BusinessError` 的推荐，改用纯运行时可行的区分方式（如 `'message' in result`）。如果希望保留类型层面的区分，应改为导出 `BusinessError` 作为 class 或添加判别式字段。

2. **`isBusinessError` 标记缺失**：按任务文件要求，在 `BusinessError` 类型定义中增加 `isBusinessError?: true` 标记字段；或采用任务允许的另一方案（复用 `ApiError`，但需解决其 `code` 窄联合无法承载业务码的问题，例如将 `ApiError.code` 拓宽为 `string`）。
