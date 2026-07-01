# 任务指令（v5）

## 动作
NEW

## 任务描述
修复 2 项遗留问题：
1. **M03**：VisitIdReconciledTask 扫描条件修正——将 `record.getVisitId() == null || isBlank()` 改为扫描 `record.getVisitIdFallback() == true` 的记录，通过 `visitFacade.findVisitIdByEncounterId(record.getVisitId())` 反查正确 visitId（注意：降级写入时 encounterId 被存为 visitId，因此 `record.getVisitId()` 的值就是当初的 encounterId），反查成功后重置 visitIdFallback=false
2. **P11**：SpecialPopulationDosageRule 与 DosageLimitRule 年龄/体重分级独立查询——SpecialPopulationDosageRule 使用专用查询方法 `findByDrugCodeAndRouteOfAdministrationAndAgeRangeStartNotNullAndAgeRangeEndNotNull()` 查询含年龄分级的剂量标准，年龄阈值改为 @Value 注入（`special-population.child-age-max:14`、`special-population.elderly-age-min:65`），体重分级支持；DosageLimitRule 使用 `findByDrugCodeAndRouteOfAdministration()` 查询通用标准后按六级优先级匹配（重写 findBestMatch）；PatientInfo 新增 weight 字段

涉及文件：
- `medical-record/.../task/VisitIdReconciledTask.java` — M03 扫描条件修正 + findVisitIdByEncounterId 参数修正
- `medical-record/.../repository/MedicalRecordRepository.java` — 新增 findByVisitIdFallbackTrue 查询方法（带 @Lock PESSIMISTIC_WRITE）
- `medical-record/.../entity/MedicalRecord.java` — @Table 注解增加 visitIdFallback 索引
- `medical-record/.../task/VisitIdReconciledTaskTest.java` — M03 测试适配
- `prescription/.../repository/DosageStandardRepository.java` — 新增 findByDrugCodeAndRouteOfAdministrationAndAgeRangeStartNotNullAndAgeRangeEndNotNull 查询方法
- `prescription/.../rule/SpecialPopulationDosageRule.java` — P11 使用专用查询 + @Value 年龄阈值 + 体重分级支持
- `prescription/.../rule/DosageLimitRule.java` — P11 使用通用查询 + 重写 findBestMatch 六级优先级 + 从 PatientInfo 获取 weight
- `prescription/.../dto/audit/PatientInfo.java` — 新增 weight 字段（OOD §8.4 六级匹配优先级隐含要求）
- `prescription/.../rule/SpecialPopulationDosageRuleTest.java` — P11 测试适配
- `prescription/.../rule/DosageLimitRuleTest.java` — P11 测试适配

## 选择理由
T4 prescription 核心服务修复已完成，T5 为剩余 2 项缺陷。M03 影响数据一致性（扫描条件错误导致 visitIdFallback=true 但 visitId 非空的记录被遗漏），P11 为 CRITICAL 级别（特殊人群年龄/体重分级查询与通用查询耦合，导致重复告警和匹配不精确）。两项均为最后未完成任务。

## 任务上下文

### M03: VisitIdReconciledTask 扫描条件修正

**OOD §4.3 要求**（Docs/07_ood_phase2_C_3_DE.md:1525）：
> 扫描 MedicalRecord 表中 visitIdFallback=true 且 visitId 与 encounterId 一致的记录（表明上次降级写入时未找到正确 visitId），通过 VisitFacade.findVisitIdByEncounterId(encounterId) 反查正确 visitId。若反查成功（返回非空 visitId），更新 MedicalRecord.visitId 为正确值并将 visitIdFallback 重置为 false；若反查仍为空，保持 visitIdFallback=true 不变下次轮任务继续尝试。

**当前代码问题**（VisitIdReconciledTask.java:30）：
```java
if (record.getVisitId() == null || record.getVisitId().isBlank())
```
- 遗漏 visitIdFallback=true 但 visitId 非空的记录（降级写入时 encounterId 被作为 visitId fallback 写入，visitId 非空但值不正确）
- 修复后应扫描 `visitIdFallback == true` 的记录

**降级写入逻辑**（MedicalRecordServiceImpl.java:138-141）：
```java
return new VisitResolveResult(encounterId, true);  // encounterId → visitId
```
当 `visitIdFallback=true` 时，`record.getVisitId()` 的值就是当初的 `encounterId`。因此反查应使用 `record.getVisitId()` 而非 `record.getPatientId()`。`patientId` 是患者标识，与 encounterId 完全无关。

**MedicalRecord 实体已有 visitIdFallback 字段**（Boolean, nullable, line 42-43, getter/setter line 98-104）

**MedicalRecordRepository 需新增查询方法**：
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
List<MedicalRecord> findByVisitIdFallbackTrue();
```

**MedicalRecord 实体需增加索引**：
```java
@Table(name = "medical_record", indexes = {
    @Index(name = "idx_visit_id_fallback", columnList = "visitIdFallback")
})
```

**修复后 reconcileVisitIds() 逻辑**：
1. 调用 `medicalRecordRepository.findByVisitIdFallbackTrue()` 获取待修复记录（带悲观锁）
2. 对每条记录，通过 `visitFacade.findVisitIdByEncounterId(record.getVisitId())` 反查正确 visitId（注意参数是 `record.getVisitId()`，即 fallback 写入的 encounterId 值）
3. 反查成功：更新 visitId + 重置 visitIdFallback=false + save
4. 反查失败/异常：保持 visitIdFallback=true，下次轮继续尝试

### P11: SpecialPopulationDosageRule/DosageLimitRule 年龄/体重分级独立查询

**OOD §3.2 要求**（Docs/07_ood_phase2_C_3_DE.md:611）：
> SpecialPopulationDosageRule：若是特殊人群则查询 DosageStandard 中对应年龄/体重分级的剂量标准
> 儿童年龄上限可配置，默认 ≤ 14 岁；老年年龄下限可配置，默认 ≥ 65 岁；对应配置项为 `special-population.child-age-max=14`、`special-population.elderly-age-min=65`，通过 @Value 或 @ConfigurationProperties 注入配置值

**OOD §8.4 六级匹配优先级**（Docs/07_ood_phase2_C_3_DE.md:1678-1708）：
1. 精确匹配：ageRangeStart=ageRangeEnd=患者年龄 AND weightRangeStart=weightRangeEnd=患者体重
2. 同时范围匹配：ageRange 包含患者年龄 AND weightRange 包含患者体重，均非 null
3. 年龄范围匹配：ageRangeStart ≤ 患者年龄 ≤ ageRangeEnd，weightRange 均为 null
4. 体重范围匹配：weightRangeStart ≤ 患者体重 ≤ weightRangeEnd，ageRange 均为 null
5. 无分级默认阈值：ageRange 和 weightRange 均为 null
6. 标准不存在：五级均未命中

**当前代码问题**：
- SpecialPopulationDosageRule 和 DosageLimitRule 均使用 `findByDrugCodeAndRouteOfAdministration()` 查询所有标准，然后在内存中过滤
- SpecialPopulationDosageRule 仅按年龄范围过滤（line 42-43），未考虑体重分级
- SpecialPopulationDosageRule 年龄阈值硬编码 `age > 14 && age < 65`（line 31），OOD 要求 @Value 可配置
- DosageLimitRule 的 findBestMatch 方法存在结构性缺陷：
  - `exactAgeMatch` 变量名误导——实际匹配的是"年龄范围+体重范围同时命中"（对应 Level 2），而非"精确匹配"（Level 1）
  - 未实现 Level 1（精确匹配：ageRangeStart=ageRangeEnd=age AND weightRangeStart=weightRangeEnd=weight）
  - 未处理部分 null 场景（OOD §8.4 决策表 #5-#8: ageRange 部分 null → 直接降级至 Level 5）
  - `patientWeight` 始终为 null（line 27），即使 PatientInfo 新增 weight 字段后，DosageLimitRule.check() 也未从 PatientInfo 获取 weight
- PatientInfo 缺少 weight 字段（OOD §8.4 六级匹配优先级隐含要求 weight: Double）

**修复方案**：
1. DosageStandardRepository 新增 `findByDrugCodeAndRouteOfAdministrationAndAgeRangeStartNotNullAndAgeRangeEndNotNull()` 查询含年龄分级的标准
2. SpecialPopulationDosageRule 使用专用查询获取含年龄分级的标准，按年龄+体重范围匹配
3. SpecialPopulationDosageRule 年龄阈值改为 @Value 注入：
   ```java
   @Value("${special-population.child-age-max:14}")
   private int childAgeMax;
   @Value("${special-population.elderly-age-min:65}")
   private int elderlyAgeMin;
   ```
4. DosageLimitRule 使用通用查询，重写 findBestMatch 实现六级优先级匹配：
   - Level 1 精确匹配：ageRangeStart != null && ageRangeEnd != null && ageRangeStart.equals(ageRangeEnd) && ageRangeStart.equals(patientAge) && weightRangeStart != null && weightRangeEnd != null && weightRangeStart.equals(weightRangeEnd) && weightRangeStart.compareTo(patientWeight) == 0
   - Level 2 同时范围匹配：ageRangeStart ≤ patientAge ≤ ageRangeEnd AND weightRangeStart ≤ patientWeight ≤ weightRangeEnd，ageRange 和 weightRange 均非 null 且非部分 null
   - Level 3 年龄范围匹配：ageRangeStart ≤ patientAge ≤ ageRangeEnd，weightRangeStart == null && weightRangeEnd == null
   - Level 4 体重范围匹配：weightRangeStart ≤ patientWeight ≤ weightRangeEnd，ageRangeStart == null && ageRangeEnd == null
   - 部分 null 边界处理：ageRangeStart/ageRangeEnd 仅一个非 null → 跳过 Level 1-3，仅尝试 Level 4-5；weightRangeStart/weightRangeEnd 仅一个非 null → 跳过 Level 1-2-4，仅尝试 Level 3-5
   - Level 5 无分级默认阈值：ageRangeStart == null && ageRangeEnd == null && weightRangeStart == null && weightRangeEnd == null
   - Level 6 标准不存在：五级均未命中，返回 null
5. DosageLimitRule.check() 从 PatientInfo 获取 weight 赋值给 patientWeight
6. PatientInfo 新增 weight 字段（Double, nullable）

## 已有代码上下文

### VisitIdReconciledTask.java
- 当前扫描条件：`record.getVisitId() == null || record.getVisitId().isBlank()`（line 30）
- 当前反查参数：`record.getPatientId()`（line 32）——错误，应为 `record.getVisitId()`
- 使用 `medicalRecordRepository.findAll()` 获取所有记录（line 28）
- MedicalRecord 实体已有 `visitIdFallback` 字段（Boolean, nullable）
- MedicalRecordRepository 当前仅有 `findByVisitId` 和 `findByPatientId`，需新增 `findByVisitIdFallbackTrue`（带 @Lock）

### MedicalRecord.java
- 当前 `@Table` 注解无 visitIdFallback 索引
- 需在 `@Table` indexes 中增加 `@Index(name = "idx_visit_id_fallback", columnList = "visitIdFallback")`

### SpecialPopulationDosageRule.java
- 使用 `dosageStandardRepository.findByDrugCodeAndRouteOfAdministration()` 通用查询（line 36）
- 仅按年龄范围过滤（line 42-43），未考虑体重
- 年龄阈值硬编码 `age > 14 && age < 65`（line 31），需改为 @Value 注入

### DosageLimitRule.java
- 使用 `dosageStandardRepository.findByDrugCodeAndRouteOfAdministration()` 通用查询（line 30）
- findBestMatch 方法存在结构性缺陷（缺少 Level 1、部分 null 处理错误、patientWeight 未获取）
- patientWeight 始终为 null（line 27），需从 PatientInfo 获取 weight

### PatientInfo.java
- 缺少 weight 字段（OOD §8.4 六级匹配优先级隐含要求 weight: Double, 可选）

### DosageStandardRepository.java
- 仅有 `findByDrugCodeAndRouteOfAdministration(String, String)` 方法
- 需新增 `findByDrugCodeAndRouteOfAdministrationAndAgeRangeStartNotNullAndAgeRangeEndNotNull(String drugCode, String routeOfAdministration)` 查询方法

### DosageStandard.java
- 已有 ageRangeStart/ageRangeEnd (Integer, nullable) 和 weightRangeStart/weightRangeEnd (BigDecimal, nullable) 字段
- 已有复合索引 idx_dosage_drug_route_age_weight

## 修订说明（v5 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| [严重] M03: findVisitIdByEncounterId 参数错误——使用 record.getPatientId() 而非 record.getVisitId() | 将所有 `record.getPatientId()` 改为 `record.getVisitId()`，并在涉及文件列表和修复后逻辑描述中同步修正，补充降级写入逻辑说明解释为何 record.getVisitId() 就是 encounterId |
| [严重] P11: DosageStandardRepository 新增查询方法名称在 task_v5.md 内部不一致 | 统一为 `findByDrugCodeAndRouteOfAdministrationAndAgeRangeStartNotNullAndAgeRangeEndNotNull`，涉及文件列表和任务上下文中同步修正 |
| [严重] P11: SpecialPopulationDosageRule 年龄阈值硬编码问题未纳入修复范围 | 在 P11 修复方案中增加 @Value 注入 `special-population.child-age-max:14` 和 `special-population.elderly-age-min:65`，涉及文件列表中 SpecialPopulationDosageRule.java 说明补充 @Value 年龄阈值 |
| [一般] P11: DosageLimitRule findBestMatch 六级优先级实现方案不完整 | 补充 findBestMatch 重写方案的关键逻辑点：Level 1 精确匹配条件、部分 null 边界处理规则、从 PatientInfo 获取 weight 赋值给 patientWeight、Level 5/6 降级逻辑 |
| [一般] M03: 缺少 visitIdFallback 数据库索引和并发安全措施 | 补充 MedicalRecord.java @Table 注解增加 visitIdFallback 索引、findByVisitIdFallbackTrue 查询方法添加 @Lock(LockModeType.PESSIMISTIC_WRITE) 注解、涉及文件列表增加 MedicalRecord.java |
| [轻微] P11: PatientInfo.weight 字段 OOD 引用错误 | 将"OOD §8.2 要求"改为"OOD §8.4 六级匹配优先级隐含要求" |
