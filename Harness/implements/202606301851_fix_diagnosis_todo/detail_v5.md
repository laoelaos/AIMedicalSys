# 详细设计（v5）

## 概述
修复 2 项遗留问题：M03（VisitIdReconciledTask 扫描条件修正，扫描 `visitIdFallback=true` 的记录并通过 `record.getVisitId()` 反查正确 visitId）和 P11（SpecialPopulationDosageRule 与 DosageLimitRule 年龄/体重分级独立查询，含六级优先级匹配、@Value 年龄阈值、PatientInfo.weight 字段）。涉及 6 个源码文件修改、2 个测试文件修改。

## 文件规划
| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `medical-record/.../task/VisitIdReconciledTask.java` | 修改 | M03 扫描条件改为 `findByVisitIdFallbackTrue` + 反查参数改为 `record.getVisitId()` + 重置 visitIdFallback |
| `medical-record/.../repository/MedicalRecordRepository.java` | 修改 | M03 新增 `findByVisitIdFallbackTrue` 查询方法（带 @Lock PESSIMISTIC_WRITE） |
| `medical-record/.../entity/MedicalRecord.java` | 修改 | M03 @Table 注解增加 visitIdFallback 索引 |
| `prescription/.../repository/DosageStandardRepository.java` | 修改 | P11 新增 `findByDrugCodeAndRouteOfAdministrationAndAgeRangeStartNotNullAndAgeRangeEndNotNull` |
| `prescription/.../rule/SpecialPopulationDosageRule.java` | 修改 | P11 专用查询 + @Value 年龄阈值 + 体重分级支持 |
| `prescription/.../rule/DosageLimitRule.java` | 修改 | P11 通用查询 + 重写 findBestMatch 六级优先级 + 从 PatientInfo 获取 weight |
| `prescription/.../dto/audit/PatientInfo.java` | 修改 | P11 新增 weight 字段 |
| `medical-record/.../task/VisitIdReconciledTaskTest.java` | 修改 | M03 测试适配 |
| `prescription/.../rule/SpecialPopulationDosageRuleTest.java` | 修改 | P11 测试适配 |
| `prescription/.../rule/DosageLimitRuleTest.java` | 修改 | P11 测试适配 |

## 类型定义

### M03: MedicalRecordRepository 修改
**形态**：interface（已有）
**包路径**：`com.aimedical.modules.medicalrecord.repository`
**变更**：新增查询方法

**新增方法签名**：
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
List<MedicalRecord> findByVisitIdFallbackTrue();
```

**需新增 import**：
- `jakarta.persistence.LockModeType`
- `org.springframework.data.jpa.repository.Lock`
- `java.util.List`

### M03: MedicalRecord 修改
**形态**：class（已有）
**包路径**：`com.aimedical.modules.medicalrecord.entity`
**变更**：@Table 注解增加 visitIdFallback 索引

**修改前**（line 21）：
```java
@Table(name = "medical_record")
```

**修改后**：
```java
@Table(name = "medical_record", indexes = {
    @Index(name = "idx_visit_id_fallback", columnList = "visitIdFallback")
})
```

**需新增 import**：
- `jakarta.persistence.Index`

### M03: VisitIdReconciledTask 修改
**形态**：class（已有）
**包路径**：`com.aimedical.modules.medicalrecord.task`
**变更**：reconcileVisitIds() 方法重写

**修改后 reconcileVisitIds() 方法**：
```java
@Scheduled(cron = "0 */30 * * * ?")
public void reconcileVisitIds() {
    List<MedicalRecord> records = medicalRecordRepository.findByVisitIdFallbackTrue();
    for (MedicalRecord record : records) {
        try {
            String encounterId = record.getVisitId();
            String visitId = visitFacade.findVisitIdByEncounterId(encounterId);
            if (visitId != null && !visitId.isBlank()) {
                record.setVisitId(visitId);
                record.setVisitIdFallback(false);
                medicalRecordRepository.save(record);
                log.info("Reconciled visitId for record {}: {}", record.getRecordId(), visitId);
            }
        } catch (Exception e) {
            log.warn("Failed to reconcile visitId for record {}: {}", record.getRecordId(), e.getMessage());
        }
    }
}
```

**变更点**：
1. `medicalRecordRepository.findAll()` → `medicalRecordRepository.findByVisitIdFallbackTrue()`：仅扫描 visitIdFallback=true 的记录（带悲观锁）
2. `record.getVisitId() == null || record.getVisitId().isBlank()` 条件移除：findByVisitIdFallbackTrue 已隐含筛选
3. `record.getPatientId()` → `record.getVisitId()`：降级写入时 encounterId 被存为 visitId，因此 record.getVisitId() 的值就是当初的 encounterId
4. 新增 `record.setVisitIdFallback(false)`：反查成功后重置 fallback 标记
5. 反查失败/异常时保持 visitIdFallback=true 不变，下次轮继续尝试

**需移除 import**：无新增/移除

### P11: DosageStandardRepository 修改
**形态**：interface（已有）
**包路径**：`com.aimedical.modules.prescription.repository`
**变更**：新增专用查询方法

**新增方法签名**：
```java
List<DosageStandard> findByDrugCodeAndRouteOfAdministrationAndAgeRangeStartNotNullAndAgeRangeEndNotNull(
    String drugCode, String routeOfAdministration);
```

### P11: PatientInfo 修改
**形态**：class（已有）
**包路径**：`com.aimedical.modules.prescription.dto.audit`
**变更**：新增 weight 字段

**新增字段**（在 `comorbidities` 字段之后）：
```java
private Double weight;
```

**新增 getter/setter**：
```java
public Double getWeight() {
    return weight;
}

public void setWeight(Double weight) {
    this.weight = weight;
}
```

### P11: SpecialPopulationDosageRule 修改
**形态**：class（已有）
**包路径**：`com.aimedical.modules.prescription.rule`
**变更**：专用查询 + @Value 年龄阈值 + 体重分级支持

**修改后完整类**：

**新增字段**：
```java
@Value("${special-population.child-age-max:14}")
private int childAgeMax;

@Value("${special-population.elderly-age-min:65}")
private int elderlyAgeMin;
```

**修改后构造器**：
```java
public SpecialPopulationDosageRule(DosageStandardRepository dosageStandardRepository) {
    this.dosageStandardRepository = dosageStandardRepository;
}
```
（构造器不变，@Value 字段通过 Spring setter 注入，不通过构造器传入）

**修改后 check() 方法**：
```java
public LocalRuleResult check(AuditRequest request) {
    PatientInfo patientInfo = request.getPatientInfo();
    if (patientInfo == null || patientInfo.getAge() == null) {
        return new LocalRuleResult(RULE_ID, true, null, AuditRiskLevel.PASS);
    }

    int age = patientInfo.getAge();
    if (age > childAgeMax && age < elderlyAgeMin) {
        return new LocalRuleResult(RULE_ID, true, null, AuditRiskLevel.PASS);
    }

    Double patientWeight = patientInfo.getWeight();

    for (PrescriptionItem item : request.getPrescriptionItems()) {
        List<DosageStandard> standards = dosageStandardRepository
            .findByDrugCodeAndRouteOfAdministrationAndAgeRangeStartNotNullAndAgeRangeEndNotNull(
                item.getDrugId(), item.getRoute());
        if (standards.isEmpty()) {
            continue;
        }

        for (DosageStandard ds : standards) {
            boolean ageMatch = age >= ds.getAgeRangeStart() && age <= ds.getAgeRangeEnd();
            boolean weightMatch = (ds.getWeightRangeStart() == null && ds.getWeightRangeEnd() == null)
                || (patientWeight != null
                    && ds.getWeightRangeStart() != null && ds.getWeightRangeEnd() != null
                    && BigDecimal.valueOf(patientWeight).compareTo(ds.getWeightRangeStart()) >= 0
                    && BigDecimal.valueOf(patientWeight).compareTo(ds.getWeightRangeEnd()) <= 0);

            if (ageMatch && weightMatch) {
                BigDecimal dose = BigDecimal.valueOf(item.getDose());
                BigDecimal singleMax = ds.getSingleMax();
                if (singleMax != null && dose.compareTo(singleMax) > 0) {
                    return new LocalRuleResult(RULE_ID, false,
                        "Special population dose " + dose + " exceeds limit " + singleMax
                            + " for drug " + item.getDrugId(),
                        AuditRiskLevel.BLOCK);
                }
            }
        }
    }

    return new LocalRuleResult(RULE_ID, true, null, AuditRiskLevel.PASS);
}
```

**变更点**：
1. `age > 14 && age < 65` → `age > childAgeMax && age < elderlyAgeMin`：年龄阈值改为 @Value 可配置
2. `findByDrugCodeAndRouteOfAdministration` → `findByDrugCodeAndRouteOfAdministrationAndAgeRangeStartNotNullAndAgeRangeEndNotNull`：仅查询含年龄分级的标准
3. 新增 `patientWeight` 获取：`patientInfo.getWeight()`
4. 体重匹配逻辑：weightRange 均为 null 视为匹配（无体重分级），否则需患者体重非 null 且在范围内
5. 匹配条件从纯年龄 → 年龄 + 体重联合匹配

**需新增 import**：
- `org.springframework.beans.factory.annotation.Value`

### P11: DosageLimitRule 修改
**形态**：class（已有）
**包路径**：`com.aimedical.modules.prescription.rule`
**变更**：通用查询 + 重写 findBestMatch 六级优先级 + 从 PatientInfo 获取 weight

**修改后 check() 方法**：
```java
public LocalRuleResult check(AuditRequest request) {
    PatientInfo patientInfo = request.getPatientInfo();
    Integer patientAge = (patientInfo != null) ? patientInfo.getAge() : null;
    Double patientWeight = (patientInfo != null) ? patientInfo.getWeight() : null;

    for (PrescriptionItem item : request.getPrescriptionItems()) {
        List<DosageStandard> standards = dosageStandardRepository
            .findByDrugCodeAndRouteOfAdministration(item.getDrugId(), item.getRoute());
        if (standards.isEmpty()) {
            continue;
        }

        DosageStandard matched = findBestMatch(standards, patientAge, patientWeight);
        if (matched == null) {
            matched = standards.get(0);
        }

        BigDecimal dose = BigDecimal.valueOf(item.getDose());
        BigDecimal singleMax = matched.getSingleMax();

        if (singleMax != null && dose.compareTo(singleMax.multiply(BigDecimal.valueOf(2))) > 0) {
            return new LocalRuleResult(RULE_ID, false, "Dose " + dose + " exceeds double the max limit " + singleMax + " for drug " + item.getDrugId(), AuditRiskLevel.BLOCK);
        }
        if (singleMax != null && dose.compareTo(singleMax) > 0) {
            return new LocalRuleResult(RULE_ID, false, "Dose " + dose + " exceeds max limit " + singleMax + " for drug " + item.getDrugId(), AuditRiskLevel.WARN);
        }
    }

    return new LocalRuleResult(RULE_ID, true, null, AuditRiskLevel.PASS);
}
```

**变更点**：`Double patientWeight = null` → `Double patientWeight = (patientInfo != null) ? patientInfo.getWeight() : null`

**修改后 findBestMatch() 方法**（完整重写）：
```java
private DosageStandard findBestMatch(List<DosageStandard> standards, Integer age, Double weight) {
    DosageStandard level1 = null;
    DosageStandard level2 = null;
    DosageStandard level3 = null;
    DosageStandard level4 = null;
    DosageStandard level5 = null;

    for (DosageStandard ds : standards) {
        Integer as = ds.getAgeRangeStart();
        Integer ae = ds.getAgeRangeEnd();
        BigDecimal ws = ds.getWeightRangeStart();
        BigDecimal we = ds.getWeightRangeEnd();

        boolean ageNull = (as == null && ae == null);
        boolean ageComplete = (as != null && ae != null);
        boolean agePartial = (as != null) != (ae != null);
        boolean weightNull = (ws == null && we == null);
        boolean weightComplete = (ws != null && we != null);
        boolean weightPartial = (ws != null) != (we != null);

        if (agePartial || weightPartial) {
            if (agePartial && !weightPartial) {
                if (level5 == null) {
                    level5 = ds;
                }
            } else if (weightPartial && !agePartial) {
                if (ageComplete && age != null
                        && age >= as && age <= ae && level3 == null) {
                    level3 = ds;
                }
                if (ageNull && level5 == null) {
                    level5 = ds;
                }
            } else {
                if (level5 == null) {
                    level5 = ds;
                }
            }
            continue;
        }

        boolean ageInRange = ageComplete && age != null && age >= as && age <= ae;
        boolean ageExact = ageComplete && age != null && as.equals(ae) && as.equals(age);
        boolean weightInRange = weightComplete && weight != null
                && BigDecimal.valueOf(weight).compareTo(ws) >= 0
                && BigDecimal.valueOf(weight).compareTo(we) <= 0;
        boolean weightExact = weightComplete && weight != null
                && ws.compareTo(we) == 0
                && ws.compareTo(BigDecimal.valueOf(weight)) == 0;

        if (ageExact && weightExact && level1 == null) {
            level1 = ds;
        }
        if (ageInRange && weightInRange && level2 == null) {
            level2 = ds;
        }
        if (ageInRange && weightNull && level3 == null) {
            level3 = ds;
        }
        if (ageNull && weightInRange && level4 == null) {
            level4 = ds;
        }
        if (ageNull && weightNull && level5 == null) {
            level5 = ds;
        }
    }

    if (level1 != null) return level1;
    if (level2 != null) return level2;
    if (level3 != null) return level3;
    if (level4 != null) return level4;
    if (level5 != null) return level5;
    return null;
}
```

**六级优先级匹配规则**：
- **Level 1 精确匹配**：ageRangeStart != null && ageRangeEnd != null && ageRangeStart.equals(ageRangeEnd) && ageRangeStart.equals(patientAge) AND weightRangeStart != null && weightRangeEnd != null && weightRangeStart.equals(weightRangeEnd) && weightRangeStart.compareTo(patientWeight) == 0
- **Level 2 同时范围匹配**：ageRangeStart ≤ patientAge ≤ ageRangeEnd AND weightRangeStart ≤ patientWeight ≤ weightRangeEnd，ageRange 和 weightRange 均完整非 null
- **Level 3 年龄范围匹配**：ageRangeStart ≤ patientAge ≤ ageRangeEnd，weightRange 均为 null
- **Level 4 体重范围匹配**：weightRangeStart ≤ patientWeight ≤ weightRangeEnd，ageRange 均为 null
- **Level 5 无分级默认阈值**：ageRangeStart == null && ageRangeEnd == null && weightRangeStart == null && weightRangeEnd == null
- **Level 6 标准不存在**：五级均未命中，返回 null

**部分 null 边界处理**（ageRangeStart/ageRangeEnd 仅一个非 null 或 weightRangeStart/weightRangeEnd 仅一个非 null）：
- ageRange 部分 null + weightRange 任意（完整/null/部分 null）→ 仅尝试 Level 5（无分级默认）：ageRange 部分 null 不满足 Level 1-3（需 ageRange 均非 null）和 Level 4（需 ageRange 均为 null）的先决条件，OOD §8.4 决策表 #5/#7 规定此类标准降级至 Level 5
- weightRange 部分 null + ageRange 完整 → 仅尝试 Level 3（年龄范围匹配）和 Level 5（无分级默认）
- ageRange 部分 null + weightRange 部分 null → 仅尝试 Level 5（无分级默认）
- ageRange null + weightRange 部分 null → 仅尝试 Level 5（无分级默认）

**设计决策**：
- 每个 Level 仅保留第一个匹配的标准（高优先级先匹配到即返回，避免低优先级覆盖高优先级）
- 部分 null 的标准降级处理：无法参与需要该维度完整信息的 Level 匹配
- findBestMatch 返回 null 时，check() 方法 fallback 到 `standards.get(0)` 保持向后兼容

## 错误处理

### M03: VisitIdReconciledTask
- `findByVisitIdFallbackTrue()` 查询带 `@Lock(LockModeType.PESSIMISTIC_WRITE)` 悲观锁，防止多实例并发 reconcile 同一条记录
- `visitFacade.findVisitIdByEncounterId()` 调用包裹在 try-catch 中，异常时仅 log.warn，保持 visitIdFallback=true 不变
- `medicalRecordRepository.save()` 未额外包裹 try-catch（与原代码一致，save 异常会中断当前循环迭代，但 @Scheduled 下一轮仍会重试）

### P11: SpecialPopulationDosageRule
- `patientWeight` 为 null 时，weightMatch 条件：`weightRangeStart == null && weightRangeEnd == null` 视为匹配（无体重分级的标准对任何体重都适用），否则不匹配（有体重分级但患者体重未知）
- `weightRangeStart` 和 `weightRangeEnd` 仅一个非 null 时（部分 null），weightMatch 条件 `ds.getWeightRangeStart() != null && ds.getWeightRangeEnd() != null` 为 false，该标准被静默跳过——此行为与 OOD §8.4"部分 null 视为无效"语义一致，不做特殊处理
- `@Value` 字段通过 Spring setter 注入，构造器不变，测试中需通过反射设置或使用 Spring Test 上下文

### P11: DosageLimitRule
- `patientWeight` 为 null 时，Level 1/2/4 不可命中（需 weight 非 null），仅尝试 Level 3/5
- `patientAge` 为 null 时，Level 1/2/3 不可命中（需 age 非 null），仅尝试 Level 4/5
- findBestMatch 返回 null 时 fallback 到 `standards.get(0)` 保持向后兼容

## 行为契约

### M03: reconcileVisitIds() 修正后契约

**前置条件**：
- `visitFacade` 非 null（构造器注入保证）
- `medicalRecordRepository` 非 null（构造器注入保证）

**扫描契约**：
- 仅扫描 `visitIdFallback = true` 的记录（不再扫描 visitId 为 null/blank 的记录）
- 扫描时获取悲观锁，防止并发修改

**反查契约**：
- 反查参数为 `record.getVisitId()`（即降级写入的 encounterId 值）
- 反查成功（返回非 null 非 blank visitId）：更新 visitId + 重置 visitIdFallback=false + save
- 反查失败/异常：保持 visitIdFallback=true，下次轮继续尝试

**后置条件**：
- 反查成功的记录：visitId 为正确值，visitIdFallback=false
- 反查失败的记录：visitId 和 visitIdFallback 不变

### P11: SpecialPopulationDosageRule 修正后契约

**前置条件**：
- `dosageStandardRepository` 非 null（构造器注入保证）
- `childAgeMax` 和 `elderlyAgeMin` 由 Spring @Value 注入，默认值分别为 14 和 65

**年龄判断契约**：
- `age <= childAgeMax`：儿童
- `age >= elderlyAgeMin`：老年人
- `childAgeMax < age < elderlyAgeMin`：普通成人，直接返回 PASS

**查询契约**：
- 使用专用查询 `findByDrugCodeAndRouteOfAdministrationAndAgeRangeStartNotNullAndAgeRangeEndNotNull`，仅获取含年龄分级的标准

**匹配契约**：
- 年龄必须在 [ageRangeStart, ageRangeEnd] 范围内
- 体重匹配：weightRange 均为 null 视为匹配，否则需患者体重非 null 且在 [weightRangeStart, weightRangeEnd] 范围内
- 年龄和体重必须同时匹配才触发剂量检查

### P11: DosageLimitRule 修正后契约

**前置条件**：
- `dosageStandardRepository` 非 null（构造器注入保证）

**查询契约**：
- 使用通用查询 `findByDrugCodeAndRouteOfAdministration`，获取所有标准

**findBestMatch 六级优先级契约**：

| 优先级 | 条件 | 要求 |
|--------|------|------|
| Level 1 | ageExact && weightExact | age/weight 均非 null，ageRange 精确=age，weightRange 精确=weight |
| Level 2 | ageInRange && weightInRange | age/weight 均非 null，ageRange 包含 age，weightRange 包含 weight |
| Level 3 | ageInRange && weightNull | age 非 null，ageRange 包含 age，weightRange 均为 null |
| Level 4 | ageNull && weightInRange | weight 非 null，ageRange 均为 null，weightRange 包含 weight |
| Level 5 | ageNull && weightNull | ageRange 和 weightRange 均为 null |
| Level 6 | null | 五级均未命中 |

**部分 null 边界契约**：
- ageRange 部分 null（仅一个非 null）：跳过 Level 1-4，仅尝试 Level 5（ageRange 部分 null 不满足 Level 1-3 的"均非 null"先决条件，也不满足 Level 4 的"均为 null"先决条件，OOD §8.4 决策表 #5/#7 规定降级至 Level 5）
- weightRange 部分 null（仅一个非 null）：跳过 Level 1-2-4，仅尝试 Level 3-5
- ageRange 部分 null + weightRange 部分 null：仅尝试 Level 5

**patientWeight 获取契约**：
- `patientInfo != null` 时：`patientWeight = patientInfo.getWeight()`（可能为 null）
- `patientInfo == null` 时：`patientWeight = null`

### P11: PatientInfo.weight 字段契约
- 类型：`Double`，nullable
- 默认值：null（未设置体重时不参与体重匹配）
- getter/setter 遵循已有字段的 JavaBean 命名约定

## 依赖关系

### M03 新增依赖
- `MedicalRecordRepository.findByVisitIdFallbackTrue()` → `jakarta.persistence.LockModeType`、`org.springframework.data.jpa.repository.Lock`
- `MedicalRecord` @Table → `jakarta.persistence.Index`

### M03 依赖变更
- `VisitIdReconciledTask.reconcileVisitIds()` 不再调用 `medicalRecordRepository.findAll()`，改为 `findByVisitIdFallbackTrue()`
- `VisitIdReconciledTask.reconcileVisitIds()` 反查参数从 `record.getPatientId()` 改为 `record.getVisitId()`

### P11 新增依赖
- `DosageStandardRepository.findByDrugCodeAndRouteOfAdministrationAndAgeRangeStartNotNullAndAgeRangeEndNotNull()` — Spring Data JPA 方法名派生查询
- `SpecialPopulationDosageRule` → `org.springframework.beans.factory.annotation.Value`
- `PatientInfo.weight` 字段 — 无外部依赖

### P11 依赖变更
- `SpecialPopulationDosageRule.check()` 查询从 `findByDrugCodeAndRouteOfAdministration` 改为 `findByDrugCodeAndRouteOfAdministrationAndAgeRangeStartNotNullAndAgeRangeEndNotNull`
- `SpecialPopulationDosageRule.check()` 从 `PatientInfo.getWeight()` 获取体重
- `DosageLimitRule.check()` 从 `PatientInfo.getWeight()` 获取体重（替代硬编码 null）

### 暴露给后续任务的接口
- `MedicalRecordRepository.findByVisitIdFallbackTrue()` — 新增公开查询方法
- `DosageStandardRepository.findByDrugCodeAndRouteOfAdministrationAndAgeRangeStartNotNullAndAgeRangeEndNotNull()` — 新增公开查询方法
- `PatientInfo.getWeight()` / `PatientInfo.setWeight(Double)` — 新增公开字段访问器
- `SpecialPopulationDosageRule` 新增 `@Value` 配置项：`special-population.child-age-max`、`special-population.elderly-age-min`

## 修订说明（v5 r1)
| 审查意见 | 修改措施 |
|---------|---------|
| [严重] P11: agePartial+weightComplete 被错误路由至 Level 4 | 删除 `agePartial && !weightPartial` 分支中的 level4 赋值逻辑（含 weightComplete 匹配），该分支内所有标准统一降级至 level5。同步修正"部分 null 边界处理"描述和行为契约表：ageRange 部分 null → 跳过 Level 1-4，仅尝试 Level 5 |
| [一般] P11: 部分 null 分支体重比较使用 `weight.compareTo(ws.doubleValue())` 存在精度损失 | 该分支中 weightComplete 匹配逻辑已随严重问题修正一并删除（agePartial+weightComplete 不再路由至 Level 4），因此此精度问题不再存在。主匹配逻辑中 `BigDecimal.valueOf(weight).compareTo(ws)` 保持不变 |
| [轻微] P11: SpecialPopulationDosageRule.check() 未显式处理 weightRange 部分 null 场景 | 在错误处理章节补充显式说明：weightRange 部分 null 时 weightMatch 为 false，标准被静默跳过，与 OOD §8.4"部分 null 视为无效"语义一致 |
