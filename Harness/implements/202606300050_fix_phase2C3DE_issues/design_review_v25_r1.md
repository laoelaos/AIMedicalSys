# 设计审查报告（v25 r1）

## 审查结果
APPROVED

## 发现

经逐项比对设计描述与源文件（`PrescriptionAssistServiceImplTest.java` L674、L710、L846-847 及 `PrescriptionAssistServiceImpl.java` L371-380）：

- **修改 1**（L674）：`"java.lang.RuntimeException: "` → `"java.util.concurrent.ExecutionException: "` 准确对应 `future.get()` 将 `RuntimeException` 包装为 `ExecutionException` 后，生产代码 `e.getClass().getName()` 返回 `"java.util.concurrent.ExecutionException"` 的事实。
- **修改 2**（L710）：`contains("TimeoutException")` → `contains("ExecutionException")` 准确对应 `completeExceptionally(new TimeoutException(...))` 经 `future.get()` 包装后，catch 块接收到的类型为 `ExecutionException` 而非 `TimeoutException`。
- **修改 3**（L846-847）：`mock` → `spy(new ObjectMapper())` 修复了 `readTree` 返回 null 导致 NPE 的根因；`when(...).thenThrow(...)` → `doThrow(...).when(...)` 是 spy 模式下避免调用真实方法的正确写法。

无严重、无一般问题。
