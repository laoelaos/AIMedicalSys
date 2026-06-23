# 设计审查报告（v4 r1）

## 审查结果
REJECTED

## 发现

### **[一般] 设计未覆盖任务要求的"日志级别验证"**

**问题**：`task_v4.md` 第13行明确要求测试需"验证响应状态码、错误码、日志级别"三个维度。而 `detail_v4.md` §测试行为仅描述了状态码和错误码验证（第57-59行），完全未提及日志级别的验证方案。

**为什么是问题**：任务描述中对测试覆盖有两个明确要求——"验证响应格式"和"日志级别"。缺少日志级别验证意味着：
1. `HttpMessageNotReadableException` handler 应使用 `log.warn`（客户端请求格式错误），若实现误写成 `log.error`，测试无法捕获
2. `HttpMessageNotWritableException` handler 应使用 `log.error`（服务端序列化失败），若实现误写成 `log.warn`，测试无法捕获
3. 现有 `shouldHandleGenericExceptionWith500` 也缺少日志级别断言（既有问题），不应在新测试中延续该缺口

**期望的修正方向**：
- 在 §测试行为中补充日志级别验证方案，明确使用 `ListAppender`（logback-classic 已由 `spring-boot-starter-test` → `spring-boot-starter-logging` 传递引入测试 classpath，无需新增依赖）
- 对 `shouldHandleMessageNotReadableWith400`：验证日志级别为 `Level.WARN`
- 对 `shouldHandleMessageNotWritableWith500`：验证日志级别为 `Level.ERROR`
- 参考诊断报告问题9 的 ListAppender 骨架代码模式

### **[轻微] 测试未包含 `getData()` 断言**

**问题**：现有测试模式中，`shouldHandleBusinessExceptionWith400` 和 `shouldHandleGenericExceptionWith500` 均显式验证 `assertNull(body.getData())`，而新增的两个测试设计未包含此断言。

**为什么是问题**：虽然 `Result.fail(ErrorCode)` 工厂方法不会设置 `data` 字段（默认为 null），保持显式断言有助于与现有测试风格一致，并在未来重构 `Result.fail` 实现时提供回归保护。

**期望的修正方向**：在新增的两个测试方法中各增加一行 `assertNull(body.getData())`，与现有 `shouldHandleBusinessExceptionWith400`、`shouldHandleGenericExceptionWith500` 保持模式一致。

## 修改要求

### 问题1（一般）：补充日志级别验证设计
在 §测试行为中，为 `shouldHandleMessageNotReadableWith400` 和 `shouldHandleMessageNotWritableWith500` 分别补充日志级别验证方案，推荐使用 `ListAppender` 模式。

### 问题2（轻微）：补充 `getData()` 断言
在新增的两个测试方法中各增加 `assertNull(body.getData())` 断言。
