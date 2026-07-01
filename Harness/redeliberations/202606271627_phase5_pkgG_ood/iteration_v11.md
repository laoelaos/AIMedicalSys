# 再审议判定报告（v11）

## 判定结果

RETRY

## 判定理由

组件B诊断报告（v2）共识别出 **8 个问题**，其中严重等级 2 个（问题 1：`LlmResponse` 缺少 `retryCount` 字段；问题 2：`extractParsedSummary`/`extractOutputSummary` 未定义），一般/重要等级 6 个（问题 3–8 涉及线程池嵌套、类型占位符、章节矛盾、闭包语法不可行、配置示例不完整、测试策略缺失）。质询报告确认全部问题证据充分、逻辑自洽、覆盖完备，质询结果为 LOCATED。实际轮次（2）未达最大轮次（12），表明审查组提前确认问题定位有效。根据判定标准——审查报告包含严重或一般等级的问题——判定为 RETRY。

## 需要解决的问题（仅 RETRY 时存在）

- **问题描述**：`LlmResponse` 缺少 `retryCount` 字段定义，但管线伪代码中直接调用 `getRetryCount()`
- **所在位置**：§2.3 类图第 290-294 行、§3.2 文本定义第 964-966 行、§4.1 伪代码第 1473 行
- **严重程度**：严重
- **改进建议**：在 `LlmResponse` 中补充 `retryCount: int` 字段，说明由 `LlmClient.invoke()` 内部填充；或将 `retryCount` 移至 `AiCallRecord` 构建器参数

- **问题描述**：`doExecuteInternal()` 中调用 `extractParsedSummary()` 和 `extractOutputSummary()` 但两方法均未定义
- **所在位置**：§4.1 伪代码第 1477 行（`extractParsedSummary`）、第 1497 行（`extractOutputSummary`）
- **严重程度**：严重
- **改进建议**：统一命名并在 `AbstractCapabilityExecutor` 中定义默认实现，或直接使用 `StringUtils.truncate` 内联

- **问题描述**：薄适配器型 `CapabilityExecutor` 在同一线程池中嵌套 `supplyAsync()` 调用
- **所在位置**：§3.1 第 730-731 行、§4.1 第 1424-1426 行
- **严重程度**：一般
- **改进建议**：薄适配器使用公共 `ForkJoinPool` 而非 `llmCallExecutor`，或在文档中显式说明嵌套模式的设计理由及 `CallerRunsPolicy` 的前提条件

- **问题描述**：`ModelRoute` 字段扩展表中 `authentication` 类型标注为"(设计占位)"
- **所在位置**：§3.2 第 991 行
- **严重程度**：一般
- **改进建议**：改为具体类型声明（如 `AuthType` 枚举），或删除字段并在 `LlmClient` 说明中描述认证凭据获取机制

- **问题描述**：§3.1 与 §4.1 对 `AbstractCapabilityExecutor.execute()` 模板方法的描述不一致——降级预检位置相互矛盾
- **所在位置**：§3.1 第 765-793 行 vs §4.1 第 1395-1438 行
- **严重程度**：一般
- **改进建议**：同步 §3.1 伪代码使其与 §4.1 一致，两处指向同一份权威伪代码

- **问题描述**：`inputSummary` 在 `execute()` 中定义为局部变量，但 `doDegrade()` 作为独立方法依赖闭包捕获，Java 语法上不可行
- **所在位置**：§4.1 第 1413 行、第 1421 行、第 1492 行注释
- **严重程度**：一般
- **改进建议**：将 `inputSummary` 作为参数加入 `doDegrade()` 和 `doExecuteInternal()` 的方法签名，或改为在方法体内重新定义

- **问题描述**：YAML 配置示例中 7 项底座能力的超时配置只覆盖了 3 项
- **所在位置**：§9.5 第 1822-1832 行
- **严重程度**：一般
- **改进建议**：完整填充全部 13 项能力的超时配置，或在注释中为每种能力给出超时值选择依据

- **问题描述**：未提供测试策略或可验证性指导
- **所在位置**：全文
- **严重程度**：一般
- **改进建议**：新增"测试策略"章节，覆盖单元测试模式、集成测试模式、管线收敛验证
