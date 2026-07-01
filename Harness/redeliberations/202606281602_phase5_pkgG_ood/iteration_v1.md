# 再审议判定报告（v1）

## 判定结果

RETRY

## 判定理由

组件B诊断报告经第2轮审查（实际轮次2/最大12轮）确认存在9个质量问题，质询结果为LOCATED确认审查结论。其中严重等级问题3个（问题1、2、6），一般等级问题4个（问题3、4、7、8），轻微等级问题2个（问题5、9）。诊断报告包含严重和一般等级的问题，符合RETRY条件，需重新运行组件A对设计产出进行修复。

## 需要解决的问题（仅 RETRY 时存在）

- **问题描述**：`doExecuteInternal()` 伪代码中 `LlmChatService` 方法调用的返回类型错误——`structuredChat()` 和 `chat()` 返回 `CompletableFuture<AiResult<X>>`，但伪代码直接将返回值作为裸对象使用，缺少两层解包
- **所在位置**：§4.1 `doExecuteInternal()` 伪代码
- **严重程度**：严重
- **改进建议**：将伪代码修正为显式解包模式

- **问题描述**：`LlmChatResponse` 缺少 `retryCount` 字段，但伪代码调用了 `chatResponse.getRetryCount()`
- **所在位置**：§2.3 类图、§3.2 字段契约、§4.1 chat() 回退路径
- **严重程度**：严重
- **改进建议**：在 `LlmChatResponse` 中增加 `retryCount: int` 字段或删除此调用硬编码为0

- **问题描述**：6 项 Phase 4 薄适配器能力的 `thinAdapterTimeout` 统一 30s，无差异化依据、无按能力覆盖机制、无配置指导
- **所在位置**：§3.1 `thinAdapterTimeout`、§9.5 YAML 配置
- **严重程度**：一般
- **改进建议**：提供按能力标识覆盖的机制，标注各能力 P99 参考值

- **问题描述**：未定义 Phase 4 业务异常的标准化映射策略，异常以裸字符串 `e.getMessage()` 传递
- **所在位置**：§3.1 薄适配器伪代码、§4.2 特化管线伪代码
- **严重程度**：一般
- **改进建议**：定义 Phase 4 异常分类表，扩展 `AiResult` 或 `DegradationReason` 支持结构化错误码

- **问题描述**：`CredentialProvider` 故障恢复状态机缺少外部调用方可调用的凭据有效性反馈方法，恢复速度仅由定时器驱动
- **所在位置**：§3.2 `CredentialProvider` Vault 降级状态模型
- **严重程度**：一般
- **改进建议**：新增 `reportCredentialResult(endpointId, boolean valid)` 方法

- **问题描述**：`DegradationStrategy` 接口实际代码中不存在 `getOrder()` 方法，设计 §4.1 伪代码中策略链排序逻辑引用不存在的 API；同时未验证新增 `default int getOrder()` 对现有实现及降级原因字符串稳定性的影响
- **所在位置**：§4.1 伪代码、§3.1 降级策略注入机制段、§7 设计决策表；实际代码 `DegradationStrategy.java`
- **严重程度**：严重
- **改进建议**：补充 `default int getOrder() { return 0; }`；列出所有需验证模块；在排序逻辑注释中说明 `getOrder()` 的作用边界

- **问题描述**：`FallbackAiService` 现有实现与设计描述的迁移起点存在系统性偏差（构造器注入方式、委托选择逻辑、`@Primary` 标注、`applyStrategies()` 调用方式、降级策略管理、上下文构造），且未分析过渡期内双降级判定的影响
- **所在位置**：§9.2 `FallbackAiService.applyStrategies()` 迁移路径、§9.3 构造器迁移路径；实际代码 `FallbackAiService.java`
- **严重程度**：一般
- **改进建议**：补充过渡期内双降级判定影响分析；补充 FallbackAiService 现有代码完整现状记录；建议先完成两个重构再进行 CapabilityExecutor 管线实现

- **问题描述**：`AiRequestBase` 基类不存在但管线伪代码已假设其存在，且未评估创建基类对 13 个已有 DTO 的 Jackson 序列化兼容性及业务模块的影响
- **所在位置**：§3.5 `AiRequestBase` 定义段、§3.1 默认实现、§4.1 `AiOrchestrator.handle()` catch 块；实际代码 `AiRequestBase.java` 不存在
- **严重程度**：一般
- **改进建议**：补充 `AiRequestBase` 具体类定义；补充 13 个 DTO 字段名兼容性核查；增加 DTO 基类继承后的序列化兼容性测试用例

- **问题描述**：4 项 Phase 5 首次落地能力（AI 知识库问答、AI 辅助开方、AI 医生排班、AI 综合讨论结论）缺少能力级别的特化设计，无 DTO 定义、Prompt 模板结构、结构化解析目标类型等具体指引
- **所在位置**：§9.1 迁移路径表、§2.1 目录结构、§4.1 通用管线伪代码
- **严重程度**：一般
- **改进建议**：补充能力级管线特化描述（DTO 字段、Prompt 模板结构、解析目标类型）；明确各能力的 LLM 管线后端关系；在 YAML 中补充路由条目
