# 测试报告（v5）

## 概述

基于详细设计 v5 的行为契约，为 M03（VisitIdReconciledTask 扫描条件修正）和 P11（SpecialPopulationDosageRule 与 DosageLimitRule 年龄/体重分级独立查询、六级优先级匹配、@Value 年龄阈值、PatientInfo.weight 字段）编写单元测试。共 3 个测试文件修改、1 个测试文件新增适配，合计 62 个测试用例。

## 测试文件清单

| 操作 | 文件路径 | 用例数 | 说明 |
|------|---------|--------|------|
| 覆写 | `medical-record/.../task/VisitIdReconciledTaskTest.java` | 11 | M03 行为契约全覆盖 |
| 覆写 | `prescription/.../rule/SpecialPopulationDosageRuleTest.java` | 23 | P11 行为契约全覆盖 |
| 覆写 | `prescription/.../rule/DosageLimitRuleTest.java` | 33 | P11 行为契约全覆盖 |
| 修改 | `prescription/.../dto/audit/PatientInfoTest.java` | +2 | P11 weight 字段 getter/setter + 默认 null |

## 行为契约覆盖矩阵

### M03: reconcileVisitIds() 修正后契约

| 行为契约 | 测试用例 | 覆盖维度 |
|---------|---------|---------|
| 仅扫描 visitIdFallback=true 的记录 | `shouldOnlyScanFallbackTrueRecords` | 正常路径 |
| 反查参数为 record.getVisitId()（即 encounterId） | `shouldUseGetVisitIdAsEncounterIdForLookup` | 正常路径 |
| 反查成功：更新 visitId + 重置 visitIdFallback=false + save | `shouldResetVisitIdFallbackOnSuccessfulReconcile` | 正常路径 |
| 反查成功：验证 save 调用 | `shouldSaveRecordAfterSuccessfulReconcile` | 正常路径 |
| 反查返回 null：保持 visitId 和 visitIdFallback 不变 | `shouldKeepFallbackTrueWhenFacadeReturnsNull` | 错误路径 |
| 反查返回 blank：保持 visitId 和 visitIdFallback 不变 | `shouldKeepFallbackTrueWhenFacadeReturnsBlank` | 错误路径 |
| 反查返回空字符串：保持不变 | `shouldKeepFallbackTrueWhenFacadeReturnsEmpty` | 边界条件 |
| 异常时保持 visitIdFallback=true，继续处理下一条 | `shouldContinueOnExceptionAndKeepFallbackTrueForFailedRecord` | 错误路径 |
| visitIdFallback=false 的记录不被扫描 | `shouldNotScanRecordsWithFallbackFalse` | 正常路径 |
| 空 fallback 列表处理 | `shouldHandleEmptyFallbackList` | 边界条件 |
| 多条 fallback 记录批量反查 | `shouldReconcileMultipleFallbackRecords` | 状态交互 |

### P11: SpecialPopulationDosageRule 修正后契约

| 行为契约 | 测试用例 | 覆盖维度 |
|---------|---------|---------|
| patientInfo 为 null → PASS | `shouldReturnPassWhenPatientInfoNull` | 正常路径 |
| age 为 null → PASS | `shouldReturnPassWhenAgeNull` | 边界条件 |
| 普通成人（childAgeMax < age < elderlyAgeMin）→ PASS | `shouldReturnPassForNormalAdultBetweenThresholds` | 正常路径 |
| age == childAgeMax → 儿童，进入检查（verify 专用查询被调用） | `shouldReturnPassForAgeExactlyAtChildAgeMax` | 边界条件 |
| age == elderlyAgeMin → 老年，进入检查（verify 专用查询被调用） | `shouldReturnPassForAgeExactlyAtElderlyAgeMin` | 边界条件 |
| 儿童（age <= childAgeMax）剂量超限 → BLOCK | `shouldCheckChildWhenAgeBelowChildAgeMax` | 正常路径 |
| 老年（age >= elderlyAgeMin）剂量超限 → BLOCK | `shouldCheckElderlyWhenAgeAboveElderlyAgeMin` | 正常路径 |
| 剂量在限制内 → PASS | `shouldPassWhenChildDoseWithinLimit` | 正常路径 |
| 使用专用查询（非通用查询） | `shouldUseSpecializedQueryNotGeneralQuery` | 状态交互 |
| weightRange 均为 null 视为匹配 | `shouldMatchWhenWeightRangeNullAndAgeInRange` | 正常路径 |
| 体重在范围内且年龄在范围内 → 匹配 | `shouldMatchWhenWeightInRangeAndAgeInRange` | 正常路径 |
| 体重越界 → 跳过该标准 | `shouldSkipWhenWeightOutOfRange` | 错误路径 |
| 患者体重为 null 但标准有体重分级 → 跳过 | `shouldSkipWhenWeightNullButStandardHasWeightRange` | 边界条件 |
| weightRange 部分 null → weightMatch 为 false → 跳过 | `shouldSkipWhenWeightRangePartiallyNull` | 边界条件 |
| 年龄不在范围内 → 跳过 | `shouldSkipWhenAgeOutOfRange` | 正常路径 |
| 无标准 → PASS | `shouldPassWhenNoStandardsFound` | 边界条件 |
| @Value childAgeMax 可配置（修改后 age=16 进入检查 → BLOCK） | `shouldUseConfigurableChildAgeMax` | 正常路径 |
| @Value elderlyAgeMin 可配置（修改后 age=62 进入检查 → BLOCK） | `shouldUseConfigurableElderlyAgeMin` | 正常路径 |
| 体重下边界匹配 | `shouldMatchWeightAtBoundaryStart` | 边界条件 |
| 体重上边界匹配 | `shouldMatchWeightAtBoundaryEnd` | 边界条件 |
| 年龄下边界匹配 | `shouldMatchAgeAtBoundaryStart` | 边界条件 |
| 年龄上边界匹配 | `shouldMatchAgeAtBoundaryEnd` | 边界条件 |
| 多药品项，首个合规第二个超限 → BLOCK | `shouldCheckMultipleItemsAndBlockOnFirstExceeding` | 状态交互 |

### P11: DosageLimitRule 修正后契约

| 行为契约 | 测试用例 | 覆盖维度 |
|---------|---------|---------|
| 无标准 → PASS | `shouldReturnPassWhenNoStandardsFound` | 边界条件 |
| 剂量在限制内 → PASS | `shouldReturnPassWhenDoseWithinLimit` | 正常路径 |
| 剂量等于 singleMax → PASS | `shouldReturnPassWhenDoseEqualsSingleMax` | 边界条件 |
| 剂量超过 singleMax → WARN | `shouldReturnWarnWhenDoseExceedsSingleMax` | 正常路径 |
| 剂量超过 2×singleMax → BLOCK | `shouldReturnBlockWhenDoseExceedsDoubleSingleMax` | 正常路径 |
| 剂量等于 2×singleMax → WARN（非 BLOCK） | `shouldReturnWarnWhenDoseEqualsDoubleSingleMax` | 边界条件 |
| Level 1 精确匹配 | `shouldMatchLevel1ExactAgeAndWeight` | 正常路径 |
| Level 2 范围匹配 | `shouldMatchLevel2AgeRangeAndWeightRange` | 正常路径 |
| Level 3 年龄范围 + 体重 null | `shouldMatchLevel3AgeRangeWeightNull` | 正常路径 |
| Level 4 体重范围 + 年龄 null | `shouldMatchLevel4WeightRangeAgeNull` | 正常路径 |
| Level 5 无分级默认 | `shouldMatchLevel5NoAgeOrWeight` | 正常路径 |
| Level 6 无匹配 → fallback standards.get(0) | `shouldFallbackToFirstStandardWhenNoMatch` | 错误路径 |
| Level 1 优先于 Level 2 | `shouldPreferLevel1OverLevel2` | 优先级 |
| Level 2 优先于 Level 3 | `shouldPreferLevel2OverLevel3` | 优先级 |
| Level 3 优先于 Level 5 | `shouldPreferLevel3OverLevel5` | 优先级 |
| Level 4 优先于 Level 5 | `shouldPreferLevel4OverLevel5` | 优先级 |
| agePartial → 降级至 Level 5 | `shouldDemoteAgePartialToLevel5` | 边界条件 |
| weightPartial + ageComplete → Level 3 + Level 5 | `shouldDemoteWeightPartialWithAgeCompleteToLevel3AndLevel5` | 边界条件 |
| weightPartial + ageNull → Level 5 | `shouldDemoteWeightPartialWithAgeNullToLevel5` | 边界条件 |
| agePartial + weightPartial → Level 5 | `shouldDemoteBothPartialToLevel5` | 边界条件 |
| patientWeight 从 PatientInfo.getWeight() 获取 | `shouldGetWeightFromPatientInfo` | 正常路径 |
| patientInfo 为 null → weight=null | `shouldHandleNullPatientInfo` | 边界条件 |
| weight=null + ageOnly 标准 → Level 3 | `shouldHandleNullWeightWithAgeOnlyStandard` | 边界条件 |
| age=null + weightOnly 标准 → Level 4 | `shouldHandleNullAgeWithWeightOnlyStandard` | 边界条件 |
| weight=null → Level 1 不可命中（exactMatch singleMax < dose，若命中则 BLOCK；实际 PASS） | `shouldNotMatchLevel1WhenWeightNull` | 边界条件 |
| age=null → Level 1 不可命中（exactMatch singleMax < dose，若命中则 BLOCK；实际 PASS） | `shouldNotMatchLevel1WhenAgeNull` | 边界条件 |
| 体重边界：下界匹配 | `shouldMatchWeightAtBoundaryStart` | 边界条件 |
| 体重边界：上界匹配 | `shouldMatchWeightAtBoundaryEnd` | 边界条件 |
| 年龄边界：下界匹配 | `shouldMatchAgeAtBoundaryStart` | 边界条件 |
| 年龄边界：上界匹配 | `shouldMatchAgeAtBoundaryEnd` | 边界条件 |
| 同级匹配保留第一个 | `shouldKeepFirstMatchAtEachLevel` | 优先级 |
| 使用通用查询（非专用查询） | `shouldUseGeneralQueryNotSpecializedQuery` | 状态交互 |
| singleMax 为 null → PASS | `shouldHandleNullSingleMax` | 边界条件 |

### P11: PatientInfo.weight 字段契约

| 行为契约 | 测试用例 | 覆盖维度 |
|---------|---------|---------|
| getter/setter 正常工作 | `shouldSetAndGetFields`（含 weight） | 正常路径 |
| 默认值为 null | `shouldWeightDefaultToNull` | 边界条件 |
| 可设为 null | `shouldSetAndGetWeightNull` | 边界条件 |

## 测试执行结果

```
VisitIdReconciledTaskTest:       11/11 PASSED
SpecialPopulationDosageRuleTest: 23/23 PASSED
DosageLimitRuleTest:             33/33 PASSED
PatientInfoTest:                  3/3  PASSED（含 2 新增 weight 测试）
合计: 70/70 PASSED
```

## 覆盖维度统计

| 维度 | 数量 |
|------|------|
| 正常路径 | 28 |
| 边界条件 | 28 |
| 错误路径 | 6 |
| 状态交互 | 5 |
| 优先级验证 | 4 |
| **合计** | **71（含跨维度重叠）** |

## 修订说明（v5 r1）

| 审查意见 | 级别 | 处理 |
|---------|------|------|
| DosageLimitRuleTest `shouldNotMatchLevel1WhenWeightNull`/`shouldNotMatchLevel1WhenAgeNull` 断言 PASS 无法区分 Level 1 未命中与 Level 1 命中但 singleMax 足够大 | 严重 | **采纳**。将 exactMatch 的 singleMax 从 30 降为 10（dose=100，若 Level 1 命中则 100 > 10 → BLOCK），低优先级标准的 singleMax 设为 500（若命中则 PASS）。当前 weight/age=null 时结果为 PASS（低优先级命中），若 Level 1 意外命中则 BLOCK，精确验证了 Level 1 不可命中 |
| SpecialPopulationDosageRuleTest `shouldReturnPassForAgeExactlyAtChildAgeMax`/`shouldReturnPassForAgeExactlyAtElderlyAgeMin` 无法区分"进入检查后因无标准 PASS"和"未进入检查 PASS" | 严重 | **采纳**。mock repository 返回包含边界年龄的标准（singleMax=200 > dose=100），验证结果 PASS 且 verify repository 专用查询被调用，证明确实进入了检查流程 |
| DosageLimitRuleTest `shouldMatchLevel5NoAgeOrWeight` 断言 WARN 无法区分 Level 5 和 Level 4 | 一般 | **部分采纳**。审查者已承认 weight=null 时 Level 4 不应命中，WARN 断言实际充分。保持不变 |
| SpecialPopulationDosageRuleTest `shouldUseConfigurableChildAgeMax`/`shouldUseConfigurableElderlyAgeMin` 仅验证修改阈值后 PASS，未验证阈值确实导致进入检查 | 一般 | **采纳**。重构为：修改阈值后 mock repository 返回超限标准，验证 BLOCK（而非 PASS），证明阈值修改后年龄确实进入了检查流程 |
| VisitIdReconciledTaskTest `shouldNotScanRecordsWithFallbackFalse` nonFallbackRecord 未加入 fallbackRecords，与空列表测试等价 | 轻微 | **采纳**。将 nonFallbackRecord 加入 fallbackRecords 列表，同时修改 StubMedicalRecordRepository.findByVisitIdFallbackTrue() 增加 visitIdFallback 过滤逻辑，使 Stub 行为与真实 Repository 一致 |
| DosageLimitRuleTest `shouldFallbackToFirstStandardWhenNoMatch` 无法区分 fallback 到第一个还是第二个标准 | 轻微 | **采纳**。调整 ageOnly singleMax=40（dose=100 > 2×40 → BLOCK），weightOnly singleMax=200（dose=100 < 200 → PASS）。fallback 目标为 ageOnly 时 BLOCK，若 fallback 到 weightOnly 则 PASS，从而精确验证 fallback 到第一个标准 |
