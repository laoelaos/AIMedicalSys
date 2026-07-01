# 质量审查报告 — Phase 2/3 OOD 设计方案 v13（v2 修订版）

## 审查范围

- **审查对象**：`a_v13_copy_from_v12.md`（Phase 2/3 包C/D-AI1/D-AI2/E 架构级 OOD 设计方案）
- **需求来源**：`requirement.md`（Phase 2/3 OOD 设计需求）
- **审查维度**：需求响应充分度、事实正确性、逻辑一致性、设计深度与完整性
- **审查视角**：面向编码落地的实用性，侧重内部审议未充分覆盖的维度
- **本轮性质**：质询反馈修订（基于 `b_v13_challenge_v1.md` 质询意见）

---

## 问题清单

### 问题 1：[严重] `@DltHandler` 与 `@TransactionalEventListener` 技术栈不匹配，死信处理方案不可执行

**问题描述**：§2.2 "跨模块事件传递机制"中，RegistrationEvent 消费失败补偿策略使用 `@DltHandler`（死信队列处理器）作为 Spring Retry 重试耗尽后的回调。但 `@DltHandler` 是 Spring Kafka/RabbitMQ 的消息中间件注解，与 `@TransactionalEventListener`（Spring 应用内事件机制）不兼容。标准 Spring ApplicationEvent + `@Retryable` 场景下，重试耗尽后应使用 `@Recover` 注解。当前描述引用了不存在于该技术栈中的注解，直接指导编码将导致编译错误。

**所在位置**：§2.2 "跨模块事件传递机制"——RegistrationEvent 消费失败补偿策略

**改进建议**：
1. 将 `@DltHandler` 替换为 `@Recover`（Spring Retry），在 recover 方法中手动写入 dead_letter_event 表
2. 或者如果设计确需死信队列语义，应引入消息中间件（RabbitMQ/Kafka），此时事件传递机制需从 Spring ApplicationEvent 全面升级为消息队列

---

### 问题 2：[严重] LocalRuleEngine 实现范围表中 DuplicateCheckRule 依赖的数据实体 DrugCompositionDict 缺少成分粒度定义，影响重复用药检测精度

**问题描述**：§3.2 DuplicateCheckRule 定义"遍历处方药品列表，对每个药品查询 DrugCompositionDict 获取成分列表，构建 drugCode → ingredientSet 映射后检测成分交集——存在两种及以上药品共享相同成分时产出 WARN 级别 LocalRuleResult"。但 §2.1 目录和 §3.2 数据来源中 DrugCompositionDict 仅定义 drugCode（主键）+ ingredients（JSON TEXT 成分列表），未定义成分的标识规范（如使用通用成分编码 ATCCode 还是中文名匹配）。若 ingredientSet 仅做字符串匹配，"对乙酰氨基酚"和"扑热息痛"同一成分因名称不同会被判为无重复——产生漏报。反之，复方感冒灵（含对乙酰氨基酚、马来酸氯苯那敏）与单独的对乙酰氨基酚片共享成分，会被判为重复——产生假阳性。设计未区分"完全重复"与"部分重叠"，未定义成分标识的归一化方案。

**所在位置**：§3.2 DuplicateCheckRule 逻辑描述 + §2.1 DrugCompositionDict 实体定义 + §3.2 数据来源说明

**改进建议**：
1. 在 DrugCompositionDict 的 ingredients JSON 中为每项增加 ingredientCode 字段（统一编码如 ATCCode 或院内药品成分编码），以编码而非中文名作为去重和交集检测的依据
2. 在 §7 设计决策中补充 DuplicateCheckRule 的检测边界说明——Phase 2/3 实现基础成分编码交集检测（可能有假阳性），或补充"共享成分占比超过 50%"的过滤阈值
3. 在药品成分种子数据脚本中标注编码来源和归一化规则

---

### 问题 3：[一般] PrescriptionDraftContext TTL 清理机制缺少扫描实现方案

**问题描述**：§3.4 定义了 PrescriptionDraftContext 的 TTL 为 60 分钟，清理时机包括"处方提交成功/处方取消/TTL 过期 60 分钟"，但 TTL 过期的具体清理机制未定义。对比而言，DialogueSessionManager 明确有 "ScheduledExecutorService 每 5 分钟扫描清理超时 30 分钟的 session"，AiSuggestionResult 明确有 "TTL 30 分钟，由 ScheduledExecutorService 定期清理过期条目"。PrescriptionDraftContext 缺少类似的 TTL 扫描实现方案，仅依赖提交/取消触发清理，若医生编辑处方后异常退出（浏览器关闭、网络断开），CRITICAL 标记将残留最多 60 分钟无主动回收机制。

**所在位置**：§3.4 PrescriptionDraftContext 生命周期管理

**改进建议**：
1. 为 PrescriptionDraftContext 补充 ScheduledExecutorService 定期扫描机制（如每 10 分钟扫描一次），与 DialogueSessionManager/AiSuggestionResult 保持一致
2. 或采用懒清理策略（每次读取时检查 TTL，过期则惰性删除）并补充说明
3. 建议统一三项 ConcurrentHashMap 存储的 TTL 扫描机制描述格式，保持设计一致性

---

### 问题 4：[一般] dead_letter_event 表和定时补偿任务的模块归属未定义

**问题描述**：§2.2 定义了 RegistrationEvent 消费失败后写入 dead_letter_event 表，以及定时补偿任务（每 30 分钟扫描重试未成功的死信事件），但该表对应的 JPA @Entity 包路径和补偿任务 Service 类所在模块均未指定。dead_letter_event 表的存储字段（eventPayload、failReason、failTime）已列出，但缺少 entity 归属和 Repository 定义，影响编码阶段的模块划分判断。

**所在位置**：§2.2 "跨模块事件传递机制"——死信事件表与补偿任务

**改进建议**：在 §2.1 目录结构中补充 dead_letter_event 表的实体归属（建议 consultation 模块 entity/ 包），明确补偿任务类路径（建议 consultation/service/ 或 common/service/），补充对应的 Repository 接口声明。

---

### 问题 5：[一般] SpecialPopulationDosageRule 年龄阈值硬编码，未暴露为可配置参数

**问题描述**：§3.2 SpecialPopulationDosageRule 定义"儿童 ≤ 14 岁、老年 ≥ 65 岁"作为特殊人群判定条件。两个年龄阈值硬编码在规则逻辑中，未作为可配置参数暴露。不同医疗机构对"儿童"和"老年"的年龄界定存在差异（如儿科通常 ≤ 14 岁但部分机构为 ≤ 12 岁，老年通常 ≥ 65 岁但部分机构为 ≥ 60 岁）。硬编码意味着不同机构需要修改代码以适应各自标准。

**所在位置**：§3.2 SpecialPopulationDosageRule 描述

**改进建议**：
1. 将年龄阈值提取为配置项（如 application.yml: prescription.special-population.child-max-age=14, prescription.special-population.elderly-min-age=65）
2. 如果 Phase 2/3 接受硬编码，应在 §7 设计决策中显式说明此决策及其适用场景限制

---

### 问题 6：[一般] AiResult 超时降级的重载方法签名的泛型约束与 ai-api DTO 空壳类的时序矛盾

**问题描述**：§2.3 AiService 接口定义中，AiResult 新增了 `AiResult.failure(String errorCode, T partialData)` 和 `AiResult.degraded(String fallbackReason, T partialData)` 重载，其中 partialData 类型为 T（泛型参数绑定 ai-api 层 Response DTO）。§10 说明 ai-api 层 DTO 当前为空壳类，需扩展完整字段定义。这意味着 AiResult 接口定义中引用的泛型参数 T 所绑定的类型尚未完成字段扩展——AiResult 接口的完整验证需等待 ai-api DTO 扩展后才能执行。§10 已补充时序依赖说明，但 §2.3 的 AiService 接口定义本身（位于 DTO 扩展之前）未标注此前提条件，编码者可能先实现 AiResult 重载但在 ai-api DTO 扩展完成前无法验证其正确性。

**所在位置**：§2.3 AiService 接口定义（AiResult 超时降级重载）+ §10 ai-api 层 DTO 扩展规格

**改进建议**：
1. 在 §2.3 AiResult 重载的说明中补充约束标记："partialData 类型 T 对应 ai-api 层 Response DTO，该 DTO 的字段扩展需在 AiService 实现前完成"
2. 或将 §10 开篇的时序依赖说明在 §2.3 处做一个交叉引用

---

### 问题 7：[一般] 配置变更事件丢失补偿机制在不同实体上覆盖不一致

**问题描述**：§9.3 和 §7 定义了配置变更审计和事件驱动缓存刷新机制。规则变更事件（TriageRuleChangeEvent、TemplateConfigChangeEvent）通过 @TransactionalEventListener(phase=AFTER_COMMIT) + 定时刷新覆盖补偿（refreshAfterWrite 默认 60 秒）。但 DrugContraindicationMapping、DrugAllergyMapping、DrugCompositionDict 三个实体的管理变更缺少独立的缓存失效事件——§3.2 数据来源说明中这些实体通过 Repository 直接查询，无应用层缓存。但 §9.3 的管理接口定义中包含这些实体的 CRUD 和发布/回滚操作，且说"规则变更后通过 ApplicationEventPublisher 发布对应事件"——但事件类型（如 DrugContraindicationChangeEvent）未定义，消费端也未说明。如果确实需要事件驱动，应补全；如果不需要，应修正 §9.3 的管理接口描述，排除对这些实体的事件发布要求。

**所在位置**：§9.3 规则管理接口描述（"规则变更后通过 ApplicationEventPublisher 发布对应事件"）+ §3.2 数据来源说明

**改进建议**：
1. 明确 DrugContraindicationMapping/DrugAllergyMapping/DrugCompositionDict 的变更是否需要事件驱动缓存失效
2. 若不需要，在 §9.3 中标注"此类实体变更直接持久化至数据库，下一次查询自动获取最新数据（无应用层缓存）"
3. 若需要，补充对应的事件类定义和消费端缓存失效逻辑

---

### 问题 8 (新增)：[一般] DosageAlertLevel / AlertSeverity / AllergyWarningSeverity 三个严重程度枚举命名约定不一致，增加编码阶段类型选择风险

**问题描述**：文档在 prescription 模块（同一 Maven 模块内）定义了三个语义高度相似的严重程度枚举，但枚举值命名和排序方向不一致：

| 枚举 | 值域 | 使用场景 |
|------|------|---------|
| DosageAlertLevel | INFO / **WARN** / CRITICAL（升序） | 辅助开方剂量告警 |
| AlertSeverity | INFO / **WARNING** / CRITICAL（升序） | 处方审核风险提示 |
| AllergyWarningSeverity | **HIGH** / WARNING / INFO（降序） | 辅助开方过敏告警 |

编码阶段的混淆路径：
- 在需要设置 `DosageAlertLevel.WARN` 的位置，开发者可能按 `AlertSeverity` 的习惯误写为 `DosageAlertLevel.WARNING`——语义相似且同在 prescription 模块，此误用不会产生编译错误，只会导致日志/序列化层面的隐蔽逻辑错误
- `AllergyWarningSeverity` 使用 HIGH 而非 CRITICAL，且排序方向与另两个枚举相反——开发者容易在比较操作中产生大小关系误判
- 三个枚举值域的差异需要开发者记忆，增加了类型选择的心智负担

**所在位置**：§1.3 包E DosageAlertLevel 条目、包D-AI1 AlertSeverity 条目、包E AllergyWarningSeverity 条目

**改进建议**：
1. 统一枚举值命名风格——建议将 DosageAlertLevel 的 WARN 改为 WARNING（与 AlertSeverity 一致）；将 AllergyWarningSeverity 的 HIGH 改为 CRITICAL 并调整排序方向为 INFO / WARNING / CRITICAL（升序，与其他两个一致）
2. 在 §7 设计决策中新增一个条目显式说明三个枚举的语义区别和各自的使用上下文，降低类型误用风险
3. 精简类型体系——评估 DosageAlertLevel 和 AlertSeverity 是否可复用同一枚举（两者值域差异仅为 WARN vs WARNING，统一后即可复用），此时 DosageAlertLevel 可直接使用 AlertSeverity

---

## 整体质量评价

该设计文档经过 13 轮审议迭代，在需求覆盖度、接口完整性和异常场景处理方面已达到较高成熟度。需求中列出的四项业务包的核心能力和架构约束均已被充分响应。本次 v2 审查在保留 v1 已确认的 7 个质量问题基础上，经质询反馈移除了 1 个基于未验证假设的问题（问题 7：matched_rules confidence 字段），并新增了 1 个编码层面影响较大的命名一致性问题（问题 8）。

剩余质量问题集中在：

1. **技术栈一致性**：`@DltHandler` 的使用表明对 Spring 事件机制和消息中间件机制的边界认识存在偏差（问题 1）
2. **数据模型精度**：DuplicateCheckRule 的成分检测缺少成分编码归一化（问题 2），可能导致临床场景的漏报和假阳性
3. **机制覆盖完备性**：事件补偿、TTL 清理、模块归属、阈值配置化等方面各有一处缺失（问题 3、4、5、7）
4. **开发时序边界**：AiResult 泛型重载与 ai-api DTO 的类型依赖关系（问题 6）需在接口层面显式标注
5. **编码一致性**：三个严重程度枚举命名冲突在编码阶段会产生隐蔽的类型误用风险（问题 8）

以上问题修复后可支持编码阶段的顺利开展，建议优先处理问题 1 和问题 2。

---

## 修订说明（v2）

| 质询意见 | 回应 |
|---------|------|
| 【问题 7 证据不充分】问题 7 声称"需求文档 3.4.1 matched_rules 子字段包含 confidence 字段"但报告自认未验证该主张，且历史审议记录明确记载需求文档 matched_rules 子结构未定义。建议删除问题 7。 | **已采纳。** 删除原问题 7（matched_rules confidence 子字段对齐评估）。该问题基于未经验证的假设提出，且历史审议记录已确认需求文档无此子结构定义，属于虚警。v2 报告已移除该条目。 |
| 【新增发现】v2 审查过程中识别出三个严重程度枚举（DosageAlertLevel/AlertSeverity/AllergyWarningSeverity）命名约定不一致的问题，已在问题 8 中记录。 | — |
