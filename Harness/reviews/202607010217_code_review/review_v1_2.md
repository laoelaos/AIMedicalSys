# R1.2: Prescription 模块代码审查（包D-AI1 处方审核 + 包E 辅助开方）

审查时间：2026-07-01

### 审查范围

依据文档：`Docs/07_ood_phase2_C_3_DE.md` 第 1.3 节和第 2 节

源码路径前缀：`AIMedical/backend/modules/prescription/src/main/java/com/aimedical/modules/prescription/`
测试路径前缀：`AIMedical/backend/modules/prescription/src/test/java/com/aimedical/modules/prescription/`

审查文件清单：
- `api/PrescriptionAuditController.java`, `api/PrescriptionAssistController.java`
- `service/audit/PrescriptionAuditService.java`, `service/audit/impl/PrescriptionAuditServiceImpl.java`
- `service/audit/PrescriptionAuditEnforcer.java`, `service/audit/impl/PrescriptionAuditEnforcerImpl.java`
- `service/audit/AuditRiskLevel.java`
- `service/assist/PrescriptionAssistService.java`, `service/assist/impl/PrescriptionAssistServiceImpl.java`
- `service/assist/DosageThresholdService.java`, `service/assist/DedupTaskScheduler.java`
- `rule/LocalRuleEngine.java`, `rule/DefaultLocalRuleEngine.java`
- `rule/AllergyCheckRule.java`, `rule/ContraindicationCheckRule.java`, `rule/DuplicateCheckRule.java`
- `rule/DosageLimitRule.java`, `rule/SpecialPopulationDosageRule.java`, `rule/DrugInteractionRule.java`
- `rule/LocalRuleResult.java`
- `context/PrescriptionDraftContext.java`, `context/DosageAlert.java`
- `converter/AuditConverter.java`, `converter/AssistConverter.java`
- `dto/audit/` 全部 DTO, `dto/assist/` 全部 DTO
- `entity/AuditRecord.java`
- `event/DrugDictChangeEvent.java`, `event/DrugDictChangeEventListener.java`
- `event/DrugContraindicationChangeEvent.java`, `event/DrugAllergyMappingChangeEvent.java`, `event/DrugCompositionDictChangeEvent.java`
- `PrescriptionErrorCode.java`
- `repository/AuditRecordRepository.java`
- 全部测试文件（覆盖 rule/service/api/converter/context/event/DTO 共 52 个测试类）
- `common-module-api/store/DraftContextStore.java`, `SessionStore.java`
- `common-module-api/drug/DrugFacade.java`

### 发现

#### [严重] AllergyCheckRule 跨模块依赖违规

- **位置**：`rule/AllergyCheckRule.java:3`
- **描述**：`AllergyCheckRule` 直接 import `com.aimedical.modules.patient.entity.AllergySeverity`，创建了对 patient 模块的编译期依赖。设计文档 §2.2 明确约定三个新模块的依赖范围仅为 `common, common-module-api, ai-api`，模块间不允许互相依赖。此违规可能导致 Maven 构建循环依赖，且 Phase 5 迁移时需额外适配。
- **建议**：将 `AllergySeverity` 枚举迁移至 `common-module-api` 或 `common` 模块的共享实体包中，或在该模块内定义独立的 AllergySeverity 枚举避免跨模块引用。引用方改为使用共享枚举类型。

#### [严重] DosageThresholdService 频率解析导致实际业务数据静默吞异常

- **位置**：`service/assist/DosageThresholdService.java:76-89`
- **描述**：第 76 行 `Integer.parseInt(request.getFrequency())` 尝试将 frequency 字段（如 `"tid"`=每日3次、`"bid"`=每日2次、`"q12h"`=每12小时）解析为整数。临床实际用药频率值几乎全部为非数字字符串，该解析会在所有日剂量校验路径上抛出 `NumberFormatException`，被第 88 行的空 catch 块吞掉，导致每日剂量校验（`OVER_DAILY_DOSE`）对所有含非数字频率的药品条目静默不执行，造成漏报。
- **建议**：实现频率字符串到每日次数的映射表（如 `"tid"→3, "bid"→2, "qd"→1, "q12h"→2, "q8h"→3` 等），在解析前通过映射转换；或要求前端/上游在传入时规范化 frequency 字段为整数每日次数。

#### [严重] PrescriptionDraftContext 未检查 unchecked cast 类型安全性

- **位置**：`context/PrescriptionDraftContext.java:28-29`
- **描述**：`getCriticalAlerts()` 方法从 `DraftContextStore.get(key)` 取到 `Object`，仅检查 `value instanceof List` 后即做 unchecked cast 为 `List<DosageAlert>`。若其他代码路径误将不同类型 List 存入相同 key，或存储实现跨进程反序列化类型擦除时，`ClassCastException` 将在调用方（如 submit() 流程）意外抛出，导致 500 内部错误。
- **建议**：增强类型安全：检查 List 内元素类型是否为 `DosageAlert`（遍历首个元素 instanceof 检查），或使用泛型包装类替代直接在 Store 中存储裸 List。

#### [一般] DrugFacade 在 prescription 模块中未被引用

- **位置**：`service/assist/impl/PrescriptionAssistServiceImpl.java`, `service/audit/impl/PrescriptionAuditServiceImpl.java`
- **描述**：设计文档 §2.2 明确要求 `prescription` 模块通过 `DrugFacade`（定义在 `common-module-api/drug/`）查询药品名称和规格信息，配置独立超时阈值（默认 2s），调用超时时不阻断主流程。当前实现中 `DrugFacade` 未被任何 prescription 代码注入或引用，处方审核和辅助开方流程缺失该跨模块门面调用。AI 返回的药品信息若无本地校验对照，可能出现药品名称/规格不匹配。
- **建议**：在 `PrescriptionAssistServiceImpl` 和 `PrescriptionAuditServiceImpl`（或下游规则类）中注入 `DrugFacade`，在调用 AI 前后用于药品信息核对与补充；遵循设计文档的降级策略（超时返回空 + WARN 日志）。

#### [一般] 三个规则类重复创建 ObjectMapper 实例

- **位置**：`rule/AllergyCheckRule.java:72`, `rule/ContraindicationCheckRule.java:61`, `rule/DuplicateCheckRule.java:60`
- **描述**：三个规则类的 JSON 解析方法中均在每次调用时 `new ObjectMapper()`，违反 ObjectMapper 线程安全可复用的最佳实践。高频审核场景下创建大量 ObjectMapper 实例增加 GC 压力和解析开销。
- **建议**：在类中声明 `private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();` 复用实例。

#### [一般] DosageThresholdService.matchByPriority 优先级逻辑存在重复遍历分支

- **位置**：`service/assist/DosageThresholdService.java:95-147`
- **描述**：`matchByPriority` 方法包含 5 个独立的 for 循环遍历同一 candidates 列表。循环 1（103-113）和循环 2（115-121）的条件高度重叠——循环 1 要求 `exactAge && exactWeight`，循环 2 仅要求 `age && weight` 都在范围内（不要求精确匹配）。实际上循环 1 返回的候选集是循环 2 的子集。此设计导致多轮遍历效率损失，且匹配优先级不易维护。
- **建议**：合并为单次遍历 + 优先级多级记录（如 DosageLimitRule.findBestMatch 的模式）。

#### [一般] PrescriptionItem 的 dose 字段使用 double 存在精度风险

- **位置**：`dto/audit/PrescriptionItem.java:7`
- **描述**：`dose` 字段声明为 `double`，剂量计算涉及 `BigDecimal` 比较时（如 DosageLimitRule:41, DosageThresholdService:52-54），`double→BigDecimal` 转换可能导致精度丢失（如 `BigDecimal.valueOf(0.1)` vs `new BigDecimal(0.1)` 差异）。当前代码各处使用 `BigDecimal.valueOf(double)` 相对安全，但 DTO 层面已丢失精度字段的原始语义。
- **建议**：将 DTO 中 `dose` 改为 `BigDecimal` 类型，或至少在转换处统一使用 `BigDecimal.valueOf()` 确保一致。

#### [一般] prescrptionOrderId 生成方式为 System.currentTimeMillis()，不适合分布式

- **位置**：`service/audit/impl/PrescriptionAuditServiceImpl.java:218, 259, 298, 328`
- **描述**：处方单号生成使用 `"RX-" + System.currentTimeMillis()`，在多实例部署或高并发场景下可能产生重复单号（相同毫秒）。设计文档未规定单号生成策略，但分布式环境下应使用 UUID 或数据库序列。
- **建议**：使用 `UUID.randomUUID().toString().replace("-", "").substring(0, ...)` 或数据库序列/雪花算法生成唯一单号。

#### [一般] PrescriptionDraftContext 的 DraftContextStore 缺少原子 check-then-act 保证

- **位置**：`context/PrescriptionDraftContext.java:19-22`
- **描述**：`hasCriticalAlerts()` 先调用 `getCriticalAlerts()`（读），再对返回的 List 判空。在并发场景下，两个操作之间另一线程可能修改 Store 中的值。`hasCriticalAlerts()` 的设计在 submit() 流程中被用于阻断判定，TOCTOU 窗口可能导致漏判。当前 submit 通过后续 `getCriticalAlerts()` 的二次读取 + `hasNewAlerts()` 做了部分补偿，但该补偿路径仅覆盖 step1→step3 窗口，不覆盖其他调用方。
- **建议**：将 `hasCriticalAlerts` 与 `getCriticalAlerts` 合并为单一原子方法（如返回 `Optional<List<DosageAlert>>`），或由 Store 层提供 `computeIfPresent` 原子操作。

#### [轻微] SubmitContext 内部类承载瞬时状态，可改用局部变量

- **位置**：`service/audit/impl/PrescriptionAuditServiceImpl.java:566-569`
- **描述**：`SubmitContext` 内部类仅包含两个字段（`snapshotCriticalAlerts`, `submitStartTime`），且仅在 `submit()` 方法内使用。定义为内部类增加了不必要的类型复杂度。
- **建议**：直接使用两个局部变量替换，简化代码。

#### [轻微] PrescriptionDraftContext 中 DosageAlert 的 severity 使用 String 而非枚举

- **位置**：`context/DosageAlert.java:5`
- **描述**：`context.DosageAlert.severity` 声明为 `String`，而 `dto.assist.DosageAlert` 使用 `DosageAlertLevel` 枚举。上下文告警的严重程度缺乏类型约束，赋值 `"CRITICAL"` vs `"critical"` 大小写不一致可能导致下游校验逻辑静默不匹配。
- **建议**：将 `context.DosageAlert.severity` 改为 `DosageAlertLevel` 枚举类型，或至少在使用处规范化大小写。

### 本轮统计

| 严重程度 | 数量 |
|---------|------|
| 严重 | 3 |
| 一般 | 7 |
| 轻微 | 2 |

### 总评

prescription 模块整体与 OOD 设计文档对齐度良好——API 端点、Service 接口/实现、规则引擎、DTO/Converter、Entity 的职责划分和命名均遵循设计约定。测试覆盖充分（52 个测试类），核心提交链路（SubmitRequest→SubmitResponse）的流程图路径和边界场景均有单元验证。

三个**严重**问题需优先处理：(1) AllergyCheckRule 对 patient 模块的直接 import 违反架构依赖约定；(2) DosageThresholdService 对临床频率字符串解析为 int 导致日剂量校验静默失效；(3) PrescriptionDraftContext 的 unchecked cast 存在运行时类型安全性风险。

**一般**问题中 DrugFacade 的缺失为设计对齐度的主要差距，建议在辅助开方和处方审核流程中引入该门面调用。ObjectMapper 重复创建和 dose double 精度问题属于工程实践改进项。

测试覆盖整体充分但缺少 PrescriptionDraftContext 并发场景的测试和跨模块 DrugFacade 调用路径的验证。
