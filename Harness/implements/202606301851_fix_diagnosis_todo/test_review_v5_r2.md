# 测试审查报告（v5 r2）

## 审查结果
APPROVED

## 发现

- **[轻微]** `VisitIdReconciledTaskTest.java` — `shouldContinueOnExceptionAndKeepFallbackTrueForFailedRecord` 测试未验证 record1 的 `saved` 状态。当 visitFacade 对 ENC-FAIL 抛异常后，StubMedicalRecordRepository.saved 可能因 record2 的成功 reconcile 被设为 true，无法单独验证 record1 未被 save。当前测试通过 `assertTrue(record1.getVisitIdFallback())` 间接确认 record1 未被 save（因为 save 只在 setVisitIdFallback(false) 之后调用），逻辑上充分但不够直观。不影响正确性。

- **[轻微]** `DosageLimitRuleTest.java` — `shouldDemoteWeightPartialWithAgeCompleteToLevel3AndLevel5` 测试名暗示验证 Level 3 和 Level 5 两条路径，但实际只验证了 WARN 结果。age=10 匹配 Level 3（ageInRange + weightNull），singleMax=80 < dose=100 → WARN，测试确实走的是 Level 3 路径，但测试名中的 "AndLevel5" 容易误导为同时验证两条路径。不影响正确性。

- **[轻微]** `SpecialPopulationDosageRuleTest.java` — `shouldCheckMultipleItemsAndBlockOnFirstExceeding` 测试名暗示"在第一个超限时 BLOCK"，但实际 item1（dose=80 < singleMax=100）未超限，item2（dose=300 > singleMax=200）超限，返回的是 item2 的 BLOCK。测试名应更准确地反映"首个合规第二个超限"的语义（与测试报告矩阵描述一致），但代码行为正确。不影响正确性。

## 逐契约验证总结

### M03: reconcileVisitIds() — 11/11 契约覆盖
- 扫描契约（仅 visitIdFallback=true）：✓ `shouldOnlyScanFallbackTrueRecords` + `shouldNotScanRecordsWithFallbackFalse`（含 Stub 过滤逻辑）
- 反查参数（record.getVisitId()）：✓ `shouldUseGetVisitIdAsEncounterIdForLookup`（验证 lastQueriedEncounterId）
- 反查成功（更新 visitId + 重置 fallback + save）：✓ 三个独立测试分别验证
- 反查失败（null/blank/empty）：✓ 三个测试覆盖
- 异常处理（保持 fallback + 继续下一条）：✓ `shouldContinueOnExceptionAndKeepFallbackTrueForFailedRecord`
- 空列表：✓ `shouldHandleEmptyFallbackList`
- 批量处理：✓ `shouldReconcileMultipleFallbackRecords`

### P11: SpecialPopulationDosageRule — 23/23 契约覆盖
- patientInfo null / age null → PASS：✓
- 普通成人跳过：✓ `shouldReturnPassForNormalAdultBetweenThresholds`
- 边界年龄（== childAgeMax / == elderlyAgeMin）：✓ r1 修订后 mock 标准并 verify 专用查询调用
- 儿童/老年超限 BLOCK：✓
- 专用查询（非通用查询）：✓ `shouldUseSpecializedQueryNotGeneralQuery`（verify + never）
- weightRange null 视为匹配 / 体重范围内匹配 / 越界跳过 / weight null 跳过 / 部分 null 跳过：✓ 五项测试
- @Value 可配置性：✓ r1 修订后改为 BLOCK 验证（阈值修改后确实进入检查）
- 边界匹配（体重/年龄 4 项）：✓
- 多药品项：✓

### P11: DosageLimitRule — 33/33 契约覆盖
- 基础 WARN/BLOCK/PASS：✓ 含 dose==singleMax 边界、dose==2×singleMax 边界
- Level 1-5 独立匹配：✓ 每级一个测试
- Level 6 fallback：✓ `shouldFallbackToFirstStandardWhenNoMatch`（r1 修订后可区分 fallback 目标）
- 优先级（L1>L2, L2>L3, L3>L5, L4>L5）：✓ 四项测试
- 部分 null 降级（agePartial→L5, weightPartial+ageComplete→L3+L5, weightPartial+ageNull→L5, bothPartial→L5）：✓
- weight 从 PatientInfo 获取 / patientInfo null / weight null + ageOnly / age null + weightOnly：✓
- Level 1 不可命中（weight null / age null）：✓ r1 修订后 exactMatch singleMax=10 < dose=100，若命中则 BLOCK
- 边界匹配（4 项）：✓
- 同级保留第一个：✓ `shouldKeepFirstMatchAtEachLevel`
- 通用查询（非专用查询）：✓ verify + never
- singleMax null → PASS：✓

### P11: PatientInfo.weight — 3/3 契约覆盖
- getter/setter：✓ 含 weight 在 shouldSetAndGetFields 中
- 默认 null：✓
- 可设为 null：✓

## 修订验证

r1 审查提出的所有严重和一般问题均已正确修正：
1. `shouldNotMatchLevel1WhenWeightNull`/`shouldNotMatchLevel1WhenAgeNull`：exactMatch singleMax 从 30 降为 10，低优先级 singleMax 设为 500，精确验证 Level 1 不可命中 ✓
2. `shouldReturnPassForAgeExactlyAtChildAgeMax`/`shouldReturnPassForAgeExactlyAtElderlyAgeMin`：mock 标准并 verify 专用查询调用 ✓
3. `shouldUseConfigurableChildAgeMax`/`shouldUseConfigurableElderlyAgeMin`：重构为 BLOCK 验证 ✓
4. `shouldNotScanRecordsWithFallbackFalse`：nonFallbackRecord 加入 fallbackRecords，Stub 增加过滤 ✓
5. `shouldFallbackToFirstStandardWhenNoMatch`：ageOnly singleMax=40（2×40=80 < 100 → BLOCK），weightOnly singleMax=200 → PASS ✓

所有 70 个测试用例执行通过，与测试报告一致。
