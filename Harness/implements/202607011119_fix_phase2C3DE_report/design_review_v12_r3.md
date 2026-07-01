# 设计审查报告（v12 r3）

## 审查结果
REJECTED

## 发现

### **[一般]** 9l — import 新增列自相矛盾

**位置**：`detail_v12.md:276,278`（9l 条目）
**问题**：import 列标注为"无需"，但注意段落明确写明"需要追加 import java.util.concurrent.TimeoutException"。MockAiService.java 实际 import 为 `import java.util.concurrent.CompletableFuture;`（具体类，非通配符），因此 `TimeoutException` 未被覆盖，确实需要单独 import。表格（"无需"）与描述（"需要"）直接矛盾，实现者可能仅阅读表格而遗漏 import，导致编译错误。

### **[一般]** 9q — 移除 null 检查的技术理由错误

**位置**：`detail_v12.md:344`（9q 理由）
**问题**：设计称"AiResult.isSuccess() 内部已 Objects.requireNonNull(data)，data 非 null 已保证"。但实际 `AiResult.java:36-38` 的 `isSuccess()` 实现为 `return success;`，**不包含** `Objects.requireNonNull` 检查。`Objects.requireNonNull(data)` 仅在 `AiResult.success(T data)` 工厂方法中。虽然实际数据流中 success=true 的实例均通过该工厂方法创建，data 确实不会为 null，但给出的技术理由是错误且具有误导性的。

### **[轻微]** 9h — 用途描述与实际修改范围不完全对应

**位置**：`detail_v12.md:189`（9h 用途）
**问题**：设计说"为 callAiWithTimeout 和 resolveVisitId 提供独立线程池"，但实际代码修改（第 139 行的 supplyAsync）仅应用于 `resolveVisitId` 方法。`callAiWithTimeout` 使用的是 `aiService.generateMedicalRecord()` 返回的 CompletableFuture，不涉及 supplyAsync，也无需 executor。

## 修改要求

### 9l
- 将 import 列从"无需"改为"需要"（或删除"无需"），确保与注意段落一致，避免误导实现者。

### 9q
- 修正移除 null 检查的理由：改为"`AiResult.success(data)` 工厂方法内部使用 `Objects.requireNonNull(data)`，因此正常创建的 success 结果 data 不可能为 null"，或简化为"遵循 9k 在 audit 入口处已拦截 null data 的防御设计，此处不再需要重复检查"。
