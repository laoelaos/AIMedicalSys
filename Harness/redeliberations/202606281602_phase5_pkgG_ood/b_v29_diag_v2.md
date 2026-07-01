# 质量审查报告（v2）— Phase 5 包 G OOD 设计文档（v29_copy_from_v28）

## 审查范围

- **待审查产出**：`a_v29_copy_from_v28.md`（Phase 5 包 G AI 进阶底座架构级 OOD 设计方案）
- **用户需求**：完成 Phase 5 包 G 的完整 OOD 设计，覆盖类图、核心职责、协作关系、关键接口、状态模型等要素，保持与 Phase0/Phase1ABD 的风格一致性
- **审查维度**：需求响应充分度、事实错误与逻辑矛盾、深度和完整性（侧重内部审议未充分覆盖的维度）
- **已知上下文**：该产出已完成组件 A 内部审议，已覆盖技术可行性等维度；当前为第 29 轮迭代，经历了 28 轮质量审查与修复

---

## 维度一：需求响应充分度

**评价：充分响应。** 产出完整覆盖了 requirement.md 的所有要求：Phase 5 包 G 的类图（§2.3）、核心职责与协作关系（§3 各抽象定义）、关键接口（§4 行为契约伪代码）、状态模型（CredentialProvider/CircuitBreaker/ModelEndpointHealthManager/PromptTemplate/Experiment 五个状态机）均以正式化的形式给出。设计风格参照 Phase0/Phase1ABD 的四元组抽象描述格式、缩进伪代码风格、设计决策四列表（§7），一致性规则在 §1.2 显式列出。未发现需求遗漏。

---

## 维度二：事实错误与逻辑矛盾

### 问题 1：[重要] §4.2 薄适配器超时 WARN 日志使用全局默认值而非有效超时值

- **问题描述**：薄适配器 `doExecuteInternal()` 伪代码中，委托调用使用 `effectiveThinAdapterTimeout`（支持 per-capability 覆盖）作为实际超时值（第 3927、3932 行），但超时降级的 WARN 日志（第 3934 行）却使用了 `thinAdapterTimeout`（全局默认值）记录超时阈值。当 per-capability 超时覆盖生效时（如 `IMAGE_ANALYSIS` 使用 45s 取代默认 30s），日志将显示默认值 30s 而非实际生效的 45s，严重误导运维排障。
- **所在位置**：§4.2 薄适配器特化管线伪代码，第 3927 行（实际使用 `effectiveThinAdapterTimeout`）vs 第 3934 行（日志使用 `thinAdapterTimeout`）
- **严重程度**：重要
- **改进建议**：将第 3934 行的 `thinAdapterTimeout.toMillis()` 替换为 `effectiveThinAdapterTimeout.toMillis()`，使日志反映实际生效的超时值。此问题曾在 v1 诊断报告中提出（问题 3），v29 修订未涉及此区域，仍需修正。

### 问题 2：[重要] LlmChatOptions 阶段一填充伪代码与参数白名单声明不一致

- **问题描述**：§3.2 第 1845 行声明参数映射白名单包含 six 个 key——`temperature`、`maxTokens`、`stopSequences`、`topP`、`frequencyPenalty`、`presencePenalty`。但 §4.1 伪代码的阶段一填充（第 3538-3544 行）仅映射了前三个（`temperature`、`maxTokens`、`stopSequences`），后三个（`topP`、`frequencyPenalty`、`presencePenalty`）缺失。这意味着运行期 `ModelRoute.parameters` 中配置的 `topP` 等参数值不会被注入 `LlmChatOptions`，`LlmChatService` 将使用 SDK 默认值而非路由配置值，产生运行时行为与设计意图不一致。
- **所在位置**：§3.2 第 1845 行（白名单声明）vs §4.1 第 3538-3544 行（伪代码映射）
- **严重程度**：重要
- **改进建议**：在 §4.1 伪代码的阶段一填充中补充 `topP`、`frequencyPenalty`、`presencePenalty` 三个字段的映射逻辑，使伪代码与 §3.2 白名单声明一致。

### 问题 3：[一般] PrescriptionAssist 变量提取策略与 DTO 嵌套结构不匹配

- **问题描述**：`PrescriptionAssistCapabilityExecutor` 特化设计表（§3.11.4 第 3147 行）声明模板变量提取策略为"方式 A（默认 `ObjectMapper.convertValue`）——从 `PrescriptionAssistRequest` DTO 直接映射为扁平键值对"。但模板变量（第 3145 行）引用了 `{{patientAge}}`、`{{patientWeight}}`、`{{allergyInfo}}` 等扁平字段名，而 `PrescriptionAssistRequest` 将患者信息封装为 `PatientInfo` 内嵌值对象（`patientInfo: PatientInfo`）。`ObjectMapper.convertValue()` 对嵌套对象产生的是嵌套 key（如 `patientInfo.age`）而非扁平 key（如 `patientAge`），导致模板变量无法正确填充。作为对比，`PrescriptionCheckCapabilityExecutor`（第 3119 行）对相同结构使用"方式 B（自定义）"手工展开，两者处理同类结构的方式不一致。
- **所在位置**：§3.11.4 第 3145 行（模板变量）、第 3147 行（变量提取策略）；- **所在位置**：§3.11.2 第 3119 行（PrescriptionCheck 使用方式 B 做对比）
- **严重程度**：一般
- **改进建议**：方案 A：将提取策略从方式 A 改为方式 B（自定义），手工将 `PatientInfo` 展开为扁平变量；方案 B：保持方式 A 但修改模板变量名为嵌套格式（如 `{{patientInfo.age}}`）；方案 C：保持方式 A 并使 `PrescriptionAssistRequest` 的字段为扁平结构（将 `PatientInfo` 字段展开到顶层而非内嵌）。建议统一 `PrescriptionAssistRequest` 和 `PrescriptionCheckRequest` 的患者数据建模方式和变量提取策略，消除不一致。

---

## 维度三：深度和完整性

### 问题 4：[一般] AiCallRecord.capabilityName 字段无填充来源

- **问题描述**：§3.5 `AiCallRecord` 字段定义表（第 2207-2208 行）列出 `capabilityName: String` 字段（能力名称），但三个工厂方法签名（`success()`/`failure()`/`degraded()`，第 2232-2252 行）的入参中均不包含 `capabilityName` 参数。§4.1 管线伪代码中所有 `AiCallRecord.success()`/`failure()`/`degraded()` 调用点均不传入 `capabilityName`（如第 3710-3715 行）。实现者无法从设计文档中获知 `capabilityName` 的填充来源——是通过 `capabilityId` 内部映射自动补全、由调用方在工厂方法调用前自行查询映射表、还是该字段实际上不应存在于当前工厂方法签名中。此缺口持续了 29 轮迭代未被发现，说明该字段存在被静默遗漏的风险——实施者若严格按工厂方法签名编码，`capabilityName` 将始终为 null。
- **所在位置**：§3.5 第 2207-2208 行（字段定义）、第 2232-2252 行（工厂方法签名）；§4.1 全文中 `AiCallRecord` 工厂方法调用点（如第 3710-3715 行、第 3955-3956 行等）
- **严重程度**：一般
- **改进建议**：方案 A（推荐）：在工厂方法签名中补充 `String capabilityName` 参数，同步更新所有调用点；方案 B：在字段定义表注释中说明 `capabilityName` 由工厂方法内部通过 `capabilityId` 从配置映射表自动补全，且需在工厂方法 Javadoc 中显式说明填充规则。

### 问题 5：[一般] 精确 Tokenizer 路径与字符估算回退路径共用同一 3000 阈值判断，v27 修订意图未落实

- **问题描述**：§4.1 `DiscussionConclusionCapabilityExecutor.doExecuteInternal()` 伪代码中，精确 Tokenizer 路径（第 3817-3827 行）和字符估算回退路径（第 3828-3842 行）各自计算 `estimatedTokens` 后，在第 3846 行共用同一 `> 3000` 阈值判断来决定是否触发前置压缩。v27 修订说明第 9 条（第 4704 行）明确要求将估算回退路径的触发阈值提升至 4000（对应中文 1.8 字符/Token 比例下约 ~2200 实际 Token，降低 30%~70% 假阳性率），但伪代码第 3846 行未区分两路径——估算路径和精确路径仍共用 3000 阈值。v27 修订说明中仅添加了第 3840 行的 `> 4000` WARN 日志，未改变第 3846 行的实际触发阈值。
- **所在位置**：§4.1 第 3817-3846 行；v27 修订说明第 9 条（第 4704 行）
- **严重程度**：一般
- **改进建议**：将第 3846 行改为按路径区分阈值——精确路径使用 3000、估算路径使用 4000（或等价于校正后实际 Token 为 3000 的字符数阈值），使伪代码行为与 v27 修订意图一致。同时确保 `estimatedTokens` 变量在各路径中语义明确（精确路径为实际 Token 数，估算路径为字符估算值），避免阈值交叉误用。

---

## 整体质量评价

经过 29 轮迭代审查，文档在绝大多数维度上已高度成熟：13 项能力的迁移路径/管线设计/DTO 改造计划完整、5 个状态机形式化定义完备、测试策略覆盖多层级（单元/集成/并发/热刷新）、非功能性质量分析（冷启动/内存/连接池/启动时延）详尽。对需求响应充分，可直接指导编码实现。

当前遗留的 5 个问题集中在**伪代码-文本声明一致性**（问题 1、2、5）和**次要设计缺口**（问题 3、4）两个维度。建议下一轮（v30）集中修正这 5 个问题，完成本轮质询中修正问题的验证，之后文档可进入实施阶段。

---

## 修订说明（v2）

| 质询意见 | 回应 |
|---------|------|
| 报告将文档版本号类问题标为"严重"且占全部发现的近 30%，任务明确要求回避此类问题 | **已采纳。** v2 报告已完全删除版本号/文档校对类问题（原 v1 的问题 1、2），审查精力集中在影响编码落地的设计质量问题上 |
| 未按任务要求的三个诊断维度组织发现 | **已采纳。** v2 报告按"需求响应充分度 / 事实错误与逻辑矛盾 / 深度和完整性"三个维度组织，每维度附整体评价 |
| 问题 7 的论证自相矛盾（先称"不一致"后承认表达式相同） | **已采纳。** 问题 7 已删除。v2 报告中每个问题的论证均经过仔细复核，避免自相矛盾的表述 |
| 整体评价应更具指导性 | **已采纳。** v2 报告补充了整体质量评价段，指出文档已高度成熟及后续建议 |
