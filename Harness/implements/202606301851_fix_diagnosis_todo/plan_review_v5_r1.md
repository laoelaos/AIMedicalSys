# 计划审查报告（v5 r1）

## 审查结果
REJECTED

## 发现

### **[严重]** M03: `findVisitIdByEncounterId` 参数错误——使用 `record.getPatientId()` 而非 `record.getVisitId()`

task_v5.md:46 写道：
> 通过 `visitFacade.findVisitIdByEncounterId(record.getPatientId())` 反查正确 visitId

当前生产代码 `VisitIdReconciledTask.java:32` 同样使用 `record.getPatientId()` 作为 encounterId，这是错误的。

OOD §4.3（07_ood_phase2_C_3_DE.md:1525）明确要求：
> 扫描 MedicalRecord 表中 visitIdFallback=true **且 visitId 与 encounterId 一致**的记录（表明上次降级写入时未找到正确 visitId），通过 VisitFacade.findVisitIdByEncounterId(encounterId) 反查正确 visitId

降级写入逻辑（MedicalRecordServiceImpl.java:138-141）在 VisitFacade 失败时将 `encounterId` 直接作为 `visitId` fallback 写入：
```java
return new VisitResolveResult(encounterId, true);  // encounterId → visitId
```

因此，当 `visitIdFallback=true` 时，`record.getVisitId()` 的值就是当初的 `encounterId`。反查应使用 `record.getVisitId()` 而非 `record.getPatientId()`。`patientId` 是患者标识，与 encounterId 完全无关。

**影响**：使用 `patientId` 调用 `findVisitIdByEncounterId` 将返回错误结果或 null，导致 reconciliation 逻辑完全失效。

**期望修正**：修复方案中 `findVisitIdByEncounterId` 的参数应为 `record.getVisitId()`（即 fallback 写入的 encounterId 值），而非 `record.getPatientId()`。

### **[严重]** P11: DosageStandardRepository 新增查询方法名称在 task_v5.md 内部不一致

task_v5.md:15 写道：
> 新增 `findByDrugCodeAndRouteOfAdministrationAndAgeRangeNotNull` 查询方法

task_v5.md:69 写道：
> 新增 `findByDrugCodeAndRouteOfAdministrationAndAgeRangeStartNotNullAndAgeRangeEndNotNull()` 查询含年龄分级的标准

两个位置给出的方法名不同：
- 涉及文件列表中：`findByDrugCodeAndRouteOfAdministrationAndAgeRangeNotNull`（仅判断 ageRange 非 null，语义模糊——ageRange 是哪个字段？）
- 任务上下文中：`findByDrugCodeAndRouteOfAdministrationAndAgeRangeStartNotNullAndAgeRangeEndNotNull`（明确判断 ageRangeStart 和 ageRangeEnd 均非 null）

**影响**：实现者将无法确定应使用哪个方法名，且两个方法名的 Spring Data JPA 派生查询语义不同——前者 `AgeRangeNotNull` 无法正确派生（DosageStandard 没有 `ageRange` 单一字段），后者 `AgeRangeStartNotNullAndAgeRangeEndNotNull` 可正确派生。

**期望修正**：统一为 `findByDrugCodeAndRouteOfAdministrationAndAgeRangeStartNotNullAndAgeRangeEndNotNull`，与 OOD §8.4 的"ageRange 均非 null"先决条件一致，且能正确派生 JPA 查询。

### **[严重]** P11: SpecialPopulationDosageRule 年龄阈值硬编码问题未纳入修复范围

OOD §3.2（07_ood_phase2_C_3_DE.md:611）明确要求：
> 儿童年龄上限可配置，默认 ≤ 14 岁；老年年龄下限可配置，默认 ≥ 65 岁；对应配置项为 `special-population.child-age-max=14`、`special-population.elderly-age-min=65`，通过 @Value 或 @ConfigurationProperties 注入配置值

当前代码 `SpecialPopulationDosageRule.java:31` 硬编码 `age > 14 && age < 65`，task_v5.md 未提及将硬编码改为 `@Value` 注入。

**影响**：修复后 SpecialPopulationDosageRule 仍与 OOD 不一致——年龄阈值不可配置，违反 OOD 明确要求。

**期望修正**：在 task_v5 的 P11 修复方案中增加：将 `14` 和 `65` 硬编码改为 `@Value("${special-population.child-age-max:14}")` 和 `@Value("${special-population.elderly-age-min:65}")` 注入。

### **[一般]** P11: DosageLimitRule findBestMatch 六级优先级实现方案不完整

task_v5.md:71 仅写道"按六级优先级匹配（完善 findBestMatch）"，未说明如何处理 OOD §8.4 的部分 null 边界规则。

当前 `findBestMatch`（DosageLimitRule.java:54-85）存在以下缺陷：
1. `exactAgeMatch` 变量名误导——实际匹配的是"年龄范围+体重范围同时命中"（对应 Level 2），而非"精确匹配"（Level 1: ageRangeStart=ageRangeEnd=患者年龄 AND weightRangeStart=weightRangeEnd=患者体重）
2. 未实现 Level 1（精确匹配）
3. 未处理部分 null 场景（OOD §8.4 决策表 #5-#8: ageRange 部分 null → 直接降级至 Level 5）
4. `patientWeight` 始终为 null（line 27），即使 PatientInfo 新增 weight 字段后，DosageLimitRule.check() 也未从 PatientInfo 获取 weight

**影响**：实现者缺乏明确的六级优先级实现指导，可能延续当前 findBestMatch 的错误逻辑结构。

**期望修正**：task_v5 应明确 findBestMatch 重写方案，至少包括：
- Level 1 精确匹配条件（ageRangeStart=ageRangeEnd=age AND weightRangeStart=weightRangeEnd=weight）
- 部分 null 边界处理（ageRange 部分 null → 跳过 Level 1-3，仅尝试 Level 4-5）
- 从 PatientInfo 获取 weight 赋值给 patientWeight
- Level 5（全 null 默认阈值）和 Level 6（标准不存在）的降级逻辑

### **[一般]** M03: 缺少 visitIdFallback 数据库索引和并发安全措施

OOD §4.3（07_ood_phase2_C_3_DE.md:1525）要求：
1. `visitIdFallback` 字段"建立数据库索引以加速扫描查询"
2. 并发安全："每次读取一条待修复记录后立即锁定（通过 `@Lock(LockModeType.PESSIMISTIC_WRITE)` 或 SQL `SELECT ... FOR UPDATE`）"

task_v5.md 未提及：
- MedicalRecord 实体上为 `visitIdFallback` 字段添加 `@Index` 或在 `@Table` indexes 中添加索引
- `findByVisitIdFallbackTrue` 查询方法添加 `@Lock(LockModeType.PESSIMISTIC_WRITE)` 注解
- MedicalRecordRepository 新增带锁查询方法

**影响**：
- 无索引：`findByVisitIdFallbackTrue()` 全表扫描，数据量增长后性能劣化
- 无悲观锁：多实例或并发调度场景下同一记录可能被重复修复

**期望修正**：task_v5 应补充：
1. MedicalRecord 实体 `@Table` 注解增加 `visitIdFallback` 索引
2. MedicalRecordRepository 新增带 `@Lock` 注解的查询方法，或在 `findByVisitIdFallbackTrue` 上添加 `@Lock`
3. 涉及文件列表中补充 MedicalRecord.java（修改）

### **[轻微]** P11: PatientInfo.weight 字段缺少 OOD §8.2 引用

task_v5.md:18 写道"新增 weight 字段（OOD §8.2 要求）"，但 OOD 文档中 §8.2 是 DosageStandard 实体定义（line 1660-1676），PatientInfo 定义在 §7.4（line 619-623）。OOD §7.4 AuditRequest.patientInfo 描述中未显式列出 weight 字段，但 §8.4 六级优先级隐含需要 weight 数据。

**影响**：引用错误不影响实现正确性，但会误导后续查阅者。

**期望修正**：将"OOD §8.2 要求"改为"OOD §8.4 六级匹配优先级隐含要求"或直接引用 AuditRequest.patientInfo 相关段落。

## 修改要求（仅 REJECTED 时）

### 严重问题 1: M03 encounterId 参数错误
- **问题**：task_v5.md 使用 `record.getPatientId()` 作为 `findVisitIdByEncounterId` 参数，与 OOD 语义矛盾
- **为什么是问题**：降级写入时 encounterId 被存为 visitId，patientId 是患者标识，二者无关。使用 patientId 将导致 reconciliation 完全失效
- **期望修正方向**：将 `record.getPatientId()` 改为 `record.getVisitId()`，并在涉及文件列表和修复后逻辑描述中同步修正

### 严重问题 2: P11 查询方法名不一致
- **问题**：task_v5.md 两个位置给出不同的 DosageStandardRepository 新增方法名
- **为什么是问题**：实现者无法确定正确方法名，且 `AgeRangeNotNull` 无法正确派生 JPA 查询
- **期望修正方向**：统一为 `findByDrugCodeAndRouteOfAdministrationAndAgeRangeStartNotNullAndAgeRangeEndNotNull`

### 严重问题 3: P11 年龄阈值硬编码未修复
- **问题**：OOD 明确要求年龄阈值通过 @Value 可配置，task_v5 未纳入
- **为什么是问题**：修复后仍与 OOD 不一致
- **期望修正方向**：在 P11 修复方案中增加 @Value 注入 special-population.child-age-max 和 special-population.elderly-age-min

### 一般问题 1: P11 findBestMatch 实现方案不完整
- **问题**：task_v5 仅说"完善 findBestMatch"，未给出六级优先级的具体实现指导
- **为什么是问题**：当前 findBestMatch 存在结构性缺陷（缺少 Level 1、部分 null 处理错误、patientWeight 未获取），无明确指导可能延续错误
- **期望修正方向**：补充 findBestMatch 重写方案的关键逻辑点

### 一般问题 2: M03 缺少索引和并发安全
- **问题**：OOD 要求 visitIdFallback 索引和悲观锁，task_v5 未提及
- **为什么是问题**：无索引性能劣化，无锁并发修复风险
- **期望修正方向**：补充索引和 @Lock 注解要求，涉及文件列表增加 MedicalRecord.java
