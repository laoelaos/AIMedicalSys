根据以下审查结果，迭代上一轮的产出，形成新版的文件，从而更好地满足用户需求。

## 当前审查结果

v7 产出存在 7 个质量问题（质询结论：LOCATED，全部确认），按严重程度排列如下：

### 1. [严重] §2.2 引用不存在的 §1.4 章节，事实性错误
§2.2 line 277 声明"变更范围严格限定为 §1.4 变更总结表中列出的 4 项"，但文档中不存在 §1.4 节（§1.3 直接跳至 §1.5）。属版本清理遗留，实施者无法找到被引用的"变更总结表"。
- 改进建议：方案 A——在 §1.3 之后恢复 §1.4「ai-api 变更范围总结表」；方案 B——将 line 277 引用改为指向 §1.6，并确保 §1.6 中的 ai-api 变更条目完整列出所有 4 项变更。

### 2. [重要] LlmChatRequest 类图缺失 `tools` 字段，图-文不一致
§2.3 类图 LlmChatRequest 仅显示 3 个字段（messages、options、clientType），缺少 tools 字段；§3.2 line 1443 文本明确描述包含 tools 字段，§4.1 line 2718 也通过 chatRequest.setTools(...) 实际使用。
- 改进建议：在 §2.3 类图的 LlmChatRequest 中补充 `+List<ChatToolDefinition> tools` 字段，并添加 `LlmChatRequest → "0..*" ChatToolDefinition : has` 关联线。

### 3. [重要] 薄适配器 per-capability 超时覆盖机制的伪代码未实现，设计缺口
§3.1 文本 line 1204–1205 承诺 per-capability 超时覆盖，但 AbstractCapabilityExecutor 构造器伪代码（line 1219–1236）未注入 `Map<String, Duration> thinAdapterPerCapabilityConfig`；薄适配器 doExecuteInternal()（line 2884）直接使用全局 thinAdapterTimeout，无按 capabilityId 查找覆盖值的逻辑。
- 改进建议：在构造器中新增 `@Qualifier("thinAdapterPerCapabilityConfig") Map<String, Duration>` 参数；在 doExecuteInternal() 中增加按 capabilityId 解析覆盖值的逻辑；在 AiPlatformConfig 中补充对应 `@Bean` 方法。

### 4. [重要] 降级策略解析逻辑与文本描述矛盾——`this.degradationStrategies` 字段未定义
文本 line 863–880 描述构造器注入 Map 后在 execute() 中按 capabilityId 查找，但 execute() 伪代码 line 1093 直接引用未定义的 `this.degradationStrategies`；构造器 line 1228 注入的字段名为 `degradationStrategyMap`。两段伪代码之间缺少"从 Map 中按 capabilityId 解析并排序"的中间步骤。
- 改进建议：方案 A——在 execute() 中将 `this.degradationStrategies` 替换为 `this.degradationStrategyMap.getOrDefault(capabilityId, Collections.emptyList())`；方案 B——在构造器或 @PostConstruct 中增加从 Map 解析的步骤。

### 5. [中等] `extractDepartmentIdFromDto()` / `extractVisitIdFromDto()` 等辅助方法未定义
薄适配器伪代码 line 983/990/997 调用 extractDepartmentIdFromDto/ExtractVisitIdFromDto/ExtractPatientIdFromDto 等方法，但它们在 AbstractCapabilityExecutor 中未定义（因属薄适配器特有逻辑），在薄适配器子类中也无方法签名。
- 改进建议：在薄适配器伪代码前新增辅助方法契约定义段，明确各方法的签名和行为（使用反射/instanceof 判断 DTO 是否有对应 getter，无则返回 null）；或简化设计直接返回 null 并通过注释说明等 Phase 4 DTO 改造后补充。

### 6. [中等] `structuredChat()` 路径中 `outputSummary` 在降级前未提取导致部分降级场景丢失输出摘要
§4.1 伪代码 line 2746–2752 结构化调用成功但 AiResult 非成功状态的降级路径中，outputSummary 传入 null；line 2794–2807 LlmInfrastructureException 降级路径中虽有 e.getMessage() 截断逻辑但未传入 doDegrade()。降级场景下 AiCallRecord 的 outputSummary 字段为空，影响运维可观测性。
- 改进建议：在上述两个降级路径中，调用 doDegrade() 之前添加 outputSummary 提取逻辑（如从 chatResponse 或异常消息中提取）。

### 7. [中等] `ModelEndpointHealthManager` 的 DEGRADED→CONNECTED 恢复路径缺少失败计数清零定义
§3.2 状态转换表 line 1358 定义 DEGRADED→CONNECTED 触发条件为"1 次调用正常（耗时 < 阈值）"，但未说明失败计数的清除策略。可能导致状态抖动。
- 改进建议：在状态转换表下方补充计数器重置策略说明——建议连续 3 次正常调用才回退 CONNECTED，或明确采用"单次正常即回退"并接受状态抖动风险。

## 历史迭代回顾

综合分析历史迭代反馈（第 2~6 轮）与当前审查结果的关系：

### 已解决的问题（出现在历史反馈但当前反馈中不再提及）
- **§1.2 不变的声明矛盾**（第2轮，严重）→ 已通过引入 §1.4 变更总结表和精确约束声明解决
- **多实例部署跨实例行为**（第2轮，严重）→ 已通过 §1.5 多实例行为约束章节解决
- **API Surface 状态表缺失**（第2轮，重要）→ 已通过 §1.6 状态表及优先级排序解决
- **Jackson 兼容性验证**（第2轮，重要）→ 已通过补充 @JsonCreator/@ConstructorProperties 说明解决
- **FallbackAiService 条件开关**（第2轮，重要）→ 已通过 @Value 构造器注入方案解决
- **降级策略注入机制两套方案矛盾**（第3轮，严重/一般）→ 已统一为 Map 方案
- **§3.11 节标题及覆盖范围**（第3轮，一般）→ 已修正
- **修订说明与设计正文混合**（第4轮，严重）→ 已剥离
- **文档内部迭代标记残留**（第4轮，重要）→ 已清除
- **§3.11 节编号不连续**（第4轮，重要）→ 已重排序
- **缺少实施拓扑顺序**（第4轮，重要）→ 已通过 §1.7 补充
- **风格一致性缺乏具体对照**（第4轮，中等）→ 已列出具体规则
- **structuredChat 返回值异常传播**（第4轮，中等）→ 已补充 CompletionException 捕获说明
- **AiService 线程安全契约**（第4轮，中等）→ 已补充
- **DTO 字段与代码现状脱节**（第5轮，严重/重要）→ 已通过过渡策略说明解决
- **TriageResponse 字段定义不一致**（第5轮，严重）→ 已修正
- **薄适配器 DTO 定义归属分歧**（第5轮，重要）→ 已明确职责归属
- **Database 类型跨数据库兼容性**（第5轮，中等）→ 已补充方言说明
- **§7 决策表与 §3.5/§6.1 矛盾**（第5轮，中等）→ 已修正
- **DegradationContext binary 兼容性验证**（第5轮，中等）→ 已补充 Jackson 防御措施
- **§3.11 共同约束不完整**（第5轮，中等）→ 已补充
- **超时配置自相矛盾/层级约束违反**（第6轮，严重）→ 已修正 YAML 示例
- **类图与正文契约字段不一致**（第6轮，严重）→ 已补充（但当前 Issue 2 表明仍有遗漏）
- **YAML 缺少 transcript-summary 超时项**（第6轮，严重）→ 已补充
- **CredentialProvider 状态恢复验证遗漏**（第6轮，一般）→ 已补充
- **薄适配器非 HTTP 场景 DTO 字段提取**（第6轮，一般）→ 已定义临时 fallback 方案

### 持续存在的问题（在多轮反馈中反复出现，需重点解决）
- **§1.4/§2.2 引用断裂**（第2轮→第7轮 Issue 1）：最初在 v4 引入 §1.4 后，v6/v7 清理修订说明时被移除或引用未同步。需彻底修复引用关系，确保 §1.4 存在或引用正确指向 §1.6。
- **类图字段遗漏**（第2轮→第6轮→第7轮 Issue 2）：LlmChatRequest tools 字段在类图中持续遗漏，v6 修复了 LlmChatOptions 和 ModelRoute 的字段，但 LlmChatRequest 的工具字段仍有缺失。需在本次彻底补齐。
- **降级策略注入伪代码**（第3轮→第7轮 Issue 4）：v7 修复了构造器注入 Map 的签名但不完整——execute() 仍引用未定义的 `this.degradationStrategies`。需确保伪代码路径前后一致。
- **薄适配器辅助方法未定义**（第3轮→第7轮 Issue 5）：extractDepartmentIdFromDto 等方法从第3轮即被指出，至今未在伪代码中补充方法契约定义。

### 新发现的问题（本轮新识别）
- **薄适配器 per-capability 超时覆盖伪代码未实现**（Issue 3）：上轮修复了 YAML 配置的层级约束，但构造器和执行伪代码中仍未体现 per-capability 覆盖逻辑
- **structuredChat 降级路径 outputSummary 丢失**（Issue 6）：成功路径非成功状态降级时 outputSummary 传入 null，影响可观测性
- **ModelEndpointHealthManager DEGRADED→CONNECTED 恢复路径计数器清零未定义**（Issue 7）：状态转换定义缺少计数器重置策略，存在状态抖动风险

## 上一轮产出路径
C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606281602_phase5_pkgG_ood\a_v7_copy_from_v6.md

## 用户需求
C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606281602_phase5_pkgG_ood\requirement.md
