# 质量审查诊断报告

**审查对象**: Phase 2/3 包C/D-AI1/D-AI2/E — 架构级 OOD 设计方案（a_v12_copy_from_v11.md）
**审查视角**: 需求响应充分度、事实/逻辑正确性、深度与完整性、实际落地可行性
**审查日期**: 2026-06-28

---

## 审查结论

经过对 1149 行设计文档的逐项审查，发现 **2 个严重问题、4 个一般问题**。整体设计经 11 轮迭代审议后，需求覆盖度较好，核心行为契约和异常边界处理较为完整。但存在的 2 个严重问题直接影响文档独立指导编码的能力。

---

## 问题清单

### 问题 1（严重）—「与前一版一致」文档参照缺陷，设计不可独立使用

**问题描述**：文档中 5 处引用了"与前一版一致"的表述，注明内容与"前一版"一致但未在当前文档中定义，导致下游读者无法从当前文档独立理解该节内容、直接指导编码。

**所在位置**：
- §8.1：`同优先级多条记录检测与前一版一致。`
- §8.2：`与药品基础信息实体的关系与前一版一致。`
- §8.3：`DosageUnitGroup 枚举（MASS_GROUP / VOLUME_GROUP / IU_GROUP）与前一版一致。`
- §8.4：`DosageStandard 实体分级字段与匹配优先级策略与前一版一致。`
- §9.4：`ConfigChangeLog 实体与前一版一致。`

**严重程度**：严重

**改进建议**：逐项展开定义以下缺失内容：
- §8.1：说明"同优先级多条记录检测"的具体语义——当多个 DosageStandard 记录同时命中同一优先级（如同为精确匹配）时的处理策略（如取最小值/最大值/拒绝），并给出示例。
- §8.2：明确 DosageStandard.drugCode 与药品基础信息实体（若有则指出其全限定类名）的关联关系（1:1 / N:1），以及该实体所属模块和字段定义。
- §8.3：明确每个 DosageUnitGroup 包含的具体单位列表及其组内换算系数（如 MASS_GROUP 包含 mcg/mg/g/kg，换算系数表）。
- §8.4：展开 DosageStandard 实体的年龄/体重分级字段完整定义（如 minAge/maxAge/minWeight/maxWeight 的类型和精度），并给出五级匹配优先级的完整流程图或伪代码。
- §9.4：补充 ConfigChangeLog 实体的完整字段定义（entityType、entityId、oldValue、newValue、operatorId、operateTime 等）。

---

### 问题 2（严重）—AiService 接口方法缺少正式定义，ai-api 模块契约来源不完整

**问题描述**：文档在 §3.1–§3.4 中分散提及 AiService 的 4 个方法（triage/prescriptionCheck/generateMedicalRecord/prescriptionAssist），在 §4.5 描述了各方法的 DTO 映射关系，但缺少一份集中的 AiService 接口成员方法签名定义。ai-api 模块的下游实现者需要从文档中自行拼凑出接口方法签名（返回类型为 AiResult\<T\>，但 T 的具体类型、方法参数列表、异常声明、不同方法是否同步/异步等均未在一处统一定义）。

**所在位置**：全文（§3.1 TriageService → `AiService.triage()`；§3.2 PrescriptionAuditService → `AiService.prescriptionCheck()`；§3.3 MedicalRecordService → `AiService.generateMedicalRecord()`；§3.4 PrescriptionAssistService → `AiService.prescriptionAssist()`；§4.5 各 Converter 映射；§6.2 `CompletableFuture<AiResult<T>>`；§6.3 @Async / CompletableFuture.runAsync()）

**严重程度**：严重

**改进建议**：在 §2.2 或新增独立章节中，给出 AiService 接口的正式方法签名定义（含方法名、参数类型、返回类型、泛型约束、异常声明）。至少应包括：

```java
public interface AiService {
    CompletableFuture<AiResult<TriageResponse>> triage(TriageRequest request);
    CompletableFuture<AiResult<PrescriptionCheckResponse>> prescriptionCheck(PrescriptionCheckRequest request);
    CompletableFuture<AiResult<MedicalRecordGenResponse>> generateMedicalRecord(MedicalRecordGenRequest request);
    CompletableFuture<AiResult<PrescriptionAssistResponse>> prescriptionAssist(PrescriptionAssistRequest request);
}
```

并说明 AiResult\<T\> 的泛型参数在各方法中的具体类型绑定。

---

### 问题 3（一般）—DosageUnitGroup 缺少组内单位清单和换算系数表

**问题描述**：§8.3 定义了 DosageUnitGroup 枚举的三个值（MASS_GROUP/VOLUME_GROUP/IU_GROUP），但未定义每个分组包含哪些具体单位及其组内换算系数。剂量阈值校验的核心逻辑之一是单位一致性校验和换算，缺少换算系数表意味着实现端只能自行假设（如假设 1g = 1000mg、1L = 1000ml），存在因假设不一致导致跨模块/跨团队实现偏差的风险。

**所在位置**：§8.3 `DosageUnitGroup 枚举（MASS_GROUP / VOLUME_GROUP / IU_GROUP）与前一版一致。`

**严重程度**：一般

**改进建议**：补充 DosageUnitGroup 的单位映射表，格式如下：

| 枚举值 | 包含单位 | 基准单位 | 换算系数（→基准单位） |
|--------|---------|---------|---------------------|
| MASS_GROUP | mcg, mg, g, kg | mg | mcg→mg: 0.001; g→mg: 1000; kg→mg: 1,000,000 |
| VOLUME_GROUP | ml, L | ml | L→ml: 1000 |
| IU_GROUP | IU | IU | 不可换算（1 IU = 1 IU） |

并说明跨组单位比较时统一输出 RX_ASSIST_UNIT_MISMATCH 错误码。

---

### 问题 4（一般）—CRITICAL 剂量告警与 BLOCK 审核阻断的隔离边界缺少一条执行路径说明

**问题描述**：§4.2 处方提交流程的第 3 步（检查 BLOCK）和第 4 步（检查 CRITICAL）为串行执行，但文档未说明当第 3 步 BLOCK 命中后是否跳过第 4 步。理论上 BLOCK 阻断已足够阻止提交，CRITICAL 检查在此场景下冗余但不应该导致 BlockResponse 中遗漏 CRITICAL 信息。当前设计未明确此路径下的阻断原因聚合策略。

**所在位置**：§4.2 处方提交端点行为——第 3 条（BLOCK 阻断）与第 4 条（CRITICAL 阻断）

**严重程度**：一般

**改进建议**：明确以下行为之一并在文档中记录：
- **路径 A（先完整收集后阻断）**：提交前同时检查 BLOCK 和 CRITICAL，将两个维度的阻断原因合并为单一 BlockResponse 返回，确保前端能看到全部阻断原因。
- **路径 B（短路优先）**：BLOCK 命中后直接阻断，不再检查 CRITICAL，BlockResponse 不包含 CRITICAL 信息。加注说明：CRITICAL 属于剂量违规，BLOCK 属于处方审核违规，BLOCK 已覆盖阻断语义，CRITICAL 在此路径下不影响阻断结果。

推荐路径 A，因为 BLOCK 审核和 CRITICAL 剂量违规是不同维度的安全问题，合并展示有利于医生定位问题。

---

### 问题 5（一般）—特殊人群剂量规则（SpecialPopulationDosageRule）未纳入核心抽象一览表

**问题描述**：§1.3 包D-AI1 核心抽象一览表列出了 ContraindicationCheckRule，但未列出 DuplicateCheckRule、DosageLimitRule、SpecialPopulationDosageRule 等本地规则类。在 §3.2 的实现范围表中这些规则被列为"完整实现"，但读者很难从核心抽象层面快速辨识规则全貌。需翻阅至 §3.2 才能看到完整的 5 条规则列表。

**所在位置**：§1.3 包D-AI1 核心抽象一览表；§3.2 LocalRuleEngine 实现范围表

**严重程度**：一般

**改进建议**：在 §1.3 核心抽象一览表的 LocalRuleEngine 条目下，以列表形式补充说明 Phase 2/3 覆盖的 5 条本地规则（DosageLimitRule、AllergyCheckRule、ContraindicationCheckRule、DuplicateCheckRule、SpecialPopulationDosageRule），或新增一行汇总说明。此调整确保核心抽象一览表能够独立呈现模块的全貌，不依赖读者翻阅至 §3.2。

---

### 问题 6（一般）—文档缺失 ai-api 层实现前提说明：当前 ai-api DTO 为空壳类的状态与扩展时序

**问题描述**：§10 开头注明了"ai-api 层 DTO 当前为空壳类（仅含默认构造器），需扩展完整字段定义以对齐需求文档 3.4.x 契约"，但未说明 ai-api 层 DTO 扩展与业务模块开发的时序依赖关系。如果业务模块和 ai-api 并行开发，Converter 需要依赖 ai-api 层 DTO 的完整字段，存在工期耦合风险。

**所在位置**：§10 ai-api 层 DTO 扩展规格——开篇说明

**严重程度**：一般

**改进建议**：补充以下内容：
- 说明 ai-api 层 DTO 扩展的完成状态（是 Phase 2/3 范围内还是外部依赖）
- 标注业务模块开发与 ai-api DTO 扩展的依赖关系：Converter 编译期依赖 ai-api DTO，ai-api 层 DTO 扩展应先于或与业务模块开发同步完成
- 若 ai-api 模块由独立团队维护，建议在文档中标注接口冻结时间点

---

## 整体质量评价

文档经过 11 轮迭代审议，核心设计质量较高。需求覆盖度充分（4 个业务包的关架构、核心抽象、行为契约、异常场景均有清晰定义），迭代历史中的 83 个审查发现点绝大多数已在当前版本中妥善修复。上述 6 个问题中，问题 1 和问题 2 是文档完整性和契约完备性的硬伤，建议修复后再交付编码阶段。
