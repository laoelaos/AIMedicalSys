# 再审议判定报告（v6）

## 判定结果

RETRY

## 判定理由

组件B质询结果为 LOCATED（审查被确认），实际轮次 1 < 最大轮次 12，提前终止。诊断报告共发现 10 个质量问题，其中严重等级 2 个（1.1 A/B 实验 Prompt 版本分流失效、2.1 ModelRouter 存储模型描述前后矛盾），一般（重要/中等）等级 7 个（1.2、2.2、2.3、3.1、3.2、3.3、3.4、3.5），轻微等级 1 个（1.3）。因包含严重和一般等级问题，判定为 RETRY。

## 需要解决的问题（仅 RETRY 时存在）

- **问题描述**：A/B 实验的 Prompt 版本分流失效 — 模板版本覆盖未接入执行管线
- **所在位置**：§4.1 标准管线伪代码第 1076 行 `promptTemplateManager.render(capabilityId, request.getDepartmentId(), variables)`
- **严重程度**：严重
- **改进建议**：将 `targetPromptVersion` 传入 `PromptTemplateManager.render()`，在 `render()` 方法签名中增加 `Integer promptVersion` 参数，并在 `DatabasePromptTemplateManager` 实现中按能力+科室+版本号检索模板；或若 Phase 5 对实验的 Prompt 版本分流无实际需求，将 `targetPromptVersion` 从 `ExperimentAssignment` 中移除或标记为预留字段

- **问题描述**：ModelRouter 存储模型描述前后矛盾 — ConcurrentHashMap 与 AtomicReference 不能共存于同一字段类型
- **所在位置**：§6.1 线程模型 ModelRouter 段
- **严重程度**：严重
- **改进建议**：统一表述为 `AtomicReference<Map<String, ModelRoute>>`，启动时通过 `AtomicReference.set()` 初始化，运行时按全量替换模式刷新；或明确为 `AtomicReference<ConcurrentHashMap<String, ModelRoute>>` 组合形态

- **问题描述**：Phase 4 薄适配器管线缺少 departmentId 的获取定义
- **所在位置**：§3.1 薄适配器伪代码 vs §3.5 过渡策略
- **严重程度**：一般
- **改进建议**：在薄适配器管线中显式说明 departmentId 的提取方式，提供独立的提取方法或通过 SecurityContext/RequestContext 统一提取

- **问题描述**：EnvironmentPostProcessor 配置转发方向未定义
- **所在位置**：§3.1 Bean 装配策略
- **严重程度**：一般
- **改进建议**：显式写明转发逻辑：`ai.platform.enabled=true` → `ai.mock.enabled=false`，反之亦然；说明 EnvironmentPostProcessor 与 YAML 属性源的优先级关系

- **问题描述**：@Qualifier Bean name 推导规则不明确 — 全大写 capabilityId 与小写驼峰示例不一致
- **所在位置**：§3.1 降级策略注入机制段
- **严重程度**：一般
- **改进建议**：统一约定 — 要么 capabilityId 调整为小写驼峰并更新能力标识映射表，要么将注入模式改为全大写一致拼接，使示例与推导规则一一对应

- **问题描述**：CallerRunsPolicy 导致 LlmCallExecutor 线程池饥饿风险
- **所在位置**：§3.5 异步队列溢出策略 + §4.1 伪代码第 1093 行、第 1103 行
- **严重程度**：一般
- **改进建议**：指标记录改为在 `.whenComplete()` / `.thenAccept()` 回调中进行，或指标采集使用独立线程池

- **问题描述**：DegradationContext 反序列化默认值的缓解措施不充分 — `>0` 判据不适用于百分比阈值场景
- **所在位置**：§3.8 DegradationContext 二进制兼容性分析段
- **严重程度**：一般
- **改进建议**：补充数据新鲜度标记、反序列化后处理校验、或丢弃旧序列化缓存等措施

- **问题描述**：伪代码中 AiCallRecord 工厂方法参数类型（long epoch ms）与字段类型（LocalDateTime）不匹配
- **所在位置**：§4.1 伪代码第 1057、1093、1103、1107 行
- **严重程度**：一般
- **改进建议**：为 AiCallRecord 显式定义工厂方法或 Builder 模式的方法签名，确保调用方式与定义一致

- **问题描述**：StandardCapabilityExecutor 与 ThinAdapterCapabilityExecutor 的复用度评估不足
- **所在位置**：§3.1 CapabilityExecutor 职责定义
- **严重程度**：一般
- **改进建议**：增加 AbstractCapabilityExecutor 抽象骨架类，提供 execute() 的默认模板方法实现

- **问题描述**：CapabilityExecutor 的线程安全性依赖于隐式 DTO 线程安全
- **所在位置**：§6.1 CapabilityExecutor 线程安全段 + §3.1 变量提取约定
- **严重程度**：一般
- **改进建议**：明确约定 request 对象在管线执行过程中视为只读，推荐 DTO 设计为不可变对象；若方式 B 的 extractVariables() 需要修改请求数据，实现者必须在方法内部进行防御性拷贝
