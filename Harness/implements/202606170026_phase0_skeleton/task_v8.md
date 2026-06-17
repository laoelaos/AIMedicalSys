# 任务指令（v8）

## 动作
RETRY

## 任务描述
修复 MockAiServiceTest.java 的 13 处泛型编译错误，使 ai-impl 模块 test-compile 通过

文件路径：`backend/ai-impl/src/test/java/com/aimedical/modules/ai/impl/mock/MockAiServiceTest.java`

## 选择理由
v7 的 ai-impl 模块实现已完成（MockAiService、NoOpDegradationStrategy、FallbackAiService 源代码编译通过），但 MockAiServiceTest 存在 13 处泛型赋值错误导致 test-compile 阻断，需要修正测试代码的 Java 类型使用方式。

## 任务上下文
- Java 泛型不变性：`CompletableFuture<AiResult<TriageResponse>>` 不能赋值给 `CompletableFuture<AiResult<?>>`
- 项目使用 Java 17，支持 `var` 关键字（Java 10+）
- MockAiService 各方法返回类型为 `CompletableFuture<AiResult<XxxResponse>>`（精确泛型），AiService 接口同样声明精确泛型返回类型
- AiResult 是泛型类 `AiResult<T>`，有 `isSuccess()`, `isDegraded()`, `getData()` 方法

## 已有代码上下文
将 MockAiServiceTest.java 中所有 13 处如下模式：

```java
CompletableFuture<AiResult<?>> future = service.triage(new TriageRequest());
```

改为：

```java
var future = service.triage(new TriageRequest());
```

`var` 会推断出 `CompletableFuture<AiResult<TriageResponse>>` 类型，`future.join()` 返回 `AiResult<TriageResponse>`，后续调用 `result.isSuccess()`、`result.isDegraded()`、`result.getData()` 均兼容。

其他 12 个方法同理（diagnosis、prescriptionCheck、generateMedicalRecord、analysisReportForInspection、analysisReportForLabTest、imageAnalysis、knowledgeBaseQuery、recommendExamination、prescriptionAssist、recommendExecutionOrder、schedule、discussionConclusion）。

无需修改 MockAiService、NoOpDegradationStrategy、FallbackAiService 的源代码（已在 v7 中编译通过）。

## RETRY 说明
- 失败原因：MockAiServiceTest 使用 `CompletableFuture<AiResult<?>>` 作为变量类型接收具体泛型的 CompletableFuture，Java 编译器因泛型不变性拒绝赋值
- 修正方向：将 13 处 `CompletableFuture<AiResult<?>> future = service.xxx(...)` 全部替换为 `var future = service.xxx(...)`
