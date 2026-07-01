# 实现计划

任务描述：修复 `Docs/Diagnosis/impl/06_todo.md` 中列出的所有问题，包含 R29 虚标 PASSED 的测试修复、5 项 P1/P2 未修复问题（C05/C12/A08/C21/C10）、DEFERRED Store 群修复（S01/S03/S06/S07）、P14 CRITICAL 写入、P11 特殊人群剂量查询、DraftContextCleanupTask 迁移、M03 扫描条件修正、enrichWithDrugInfo 死代码处理。
项目根目录：C:\Develop\Software\AIMedicalSys

---

## 实施路线表格

| 任务编号 | 任务描述 | 涉及文件 | 状态 |
|---------|---------|---------|------|
| T1 | 修复 R29 虚标 PASSED（ObjectMapperJavaTimeModuleTest + consultation pom.xml） | consultation/pom.xml, ObjectMapperJavaTimeModuleTest.java | ✅ |
| T2 | 修复 consultation 模块 5 项问题（C05 业务错误码+字段互斥校验, C10 UUID v4 校验, C12 3000字符截断+TRUNCATED标记, A08 降级文案中文, C21 session快照切回） | TriageServiceImpl.java, TriageConverter.java, DialogueSessionManager.java, TriageErrorCode.java | ✅ |
| T3 | 修复 Store 群问题（S01 原子createIfNotExists, S03 compute跨key写入, S06 类型转换, S07 混合存储）+ SuggestionCleanupTask 类型适配 | SuggestionStore.java, DedupTaskScheduler.java, ConcurrentHashMapStore.java, SuggestionCleanupTask.java, AiSuggestionResult.java | ✅ |
| T4 | 修复 PrescriptionAssistServiceImpl CRITICAL 告警清除（P14）+ DraftContextCleanupTask 模块迁移 + 移除 enrichWithDrugInfo 死代码 | PrescriptionAssistServiceImpl.java, PrescriptionAuditServiceImpl.java, DraftContextCleanupTask.java, PrescriptionAssistServiceImplTest.java, PrescriptionAuditServiceImplTest.java | ✅ |
| T5 | 修复 VisitIdReconciledTask 扫描条件（M03）+ SpecialPopulationDosageRule/DosageLimitRule 年龄/体重分级独立查询（P11） | VisitIdReconciledTask.java, MedicalRecordRepository.java, MedicalRecord.java, DosageStandardRepository.java, SpecialPopulationDosageRule.java, DosageLimitRule.java, PatientInfo.java | ✅ |

---

## R1 NEW T1: 修复 R29 虚标 PASSED（ObjectMapperJavaTimeModuleTest + consultation pom.xml）
任务：在 consultation/pom.xml 显式声明 jackson-datatype-jsr310 依赖，修复 ObjectMapperJavaTimeModuleTest 中 LocalDateTime 序列化退化为数组的问题（添加 disable(WRITE_DATES_AS_TIMESTAMPS)）。
选择理由：R29 验收轮次的直接障碍，测试基础设施问题优先级最高，阻断后续所有修复的验证。
上下文：ObjectMapperJavaTimeModuleTest 直接依赖 JavaTimeModule，但 consultation 模块未在 pom.xml 显式声明 jackson-datatype-jsr310（仅靠传递依赖）。当前 classpath 下 JavaTimeModule 注册后未实际生效，LocalDateTime 序列化退化为数组形式。该测试在 `mvn clean test` 全量构建中失败。

---

## R2 PASSED T1: 修复 R29 虚标 PASSED
结果：consultation/pom.xml 新增 jackson-datatype-jsr310 依赖，ObjectMapperJavaTimeModuleTest 及新增 ObjectMapperJavaTimeModuleEdgeCaseTest 共 9 个测试全部通过
测试：ObjectMapperJavaTimeModuleTest (3个) + ObjectMapperJavaTimeModuleEdgeCaseTest (9个)，verify_v1.md: BUILD SUCCESS

## R2 NEW T2: 修复 consultation 模块 5 项问题（C05/C12/A08/C21/C10）
任务：集中修复 TriageServiceImpl/TriageConverter/DialogueSessionManager/TriageErrorCode 中 5 项遗留问题
选择理由：T1 基础设施修复完成，T2 为 consultation 模块业务逻辑缺陷，优先级最高，覆盖业务错误码、字段校验、字符截断、国际化文案、快照引用
上下文：详见 task_v2.md

---

## R3 PASSED T2: 修复 consultation 模块 5 项问题（C05/C12/A08/C21/C10）
结果：所有 5 项缺陷修复，code/design/test/plan 审查全部 APPROVED。verify_v2.md 首轮因 ai-api 未先 install 失败（NoSuchMethodError: setAdditionalResponsesText），install 后重跑 BUILD SUCCESS（84 tests, 0 failures, 0 errors）
测试：DialogueSessionManagerTest (18) + TriageConverterTest (17) + TriageServiceImplTest (49) = 84 tests all passed；ai-api TriageDtoTest (38) all passed

## R3 NEW T3: 修复 Store 群问题（S01/S03/S06/S07）+ SuggestionCleanupTask 类型适配
任务：修复 common-module 和 prescription 模块中 Store 相关 4 项问题 + SuggestionCleanupTask 失效
选择理由：T2 consultation 业务逻辑修复完成，T3 为模块间共享基础设施修复，阻断后续 T4（DraftContextCleanupTask 迁移）和 SuggestionCleanupTask 正常运行
上下文：详见 task_v3.md

---

## R4 PASSED T3: 修复 Store 群问题（S01/S03/S06/S07）+ SuggestionCleanupTask 类型适配
结果：SuggestionStore 新增 createIfNotExists、SuggestionStoreEntry 独立接口、DraftContextStoreImpl 独立实现、AiSuggestionResult 实现 SuggestionStoreEntry、DedupTaskScheduler 重构为 get+createIfNotExists+compute 混合策略、SuggestionCleanupTask 适配。共 7 个源码文件（2 新建、5 修改）+ 3 个测试文件修改。
测试：ConcurrentHashMapStoreTest (22) + DraftContextStoreImplTest (16) + AiSuggestionResultTest (7) + DedupTaskSchedulerTest (12) + SuggestionCleanupTaskTest (8) = 65 tests all passed；verify_v3.md: BUILD SUCCESS (79 tests total including common-module-api 52 + prescription 27)

## R4 NEW T4: 修复 PrescriptionAssistServiceImpl CRITICAL 告警清除（P14）+ DraftContextCleanupTask 模块迁移 + 移除 enrichWithDrugInfo 死代码
任务：修复 prescription 模块中 P14 CRITICAL 告警清除、DraftContextCleanupTask 从 consultation 迁移到 prescription、移除 enrichWithDrugInfo 死代码
选择理由：T3 Store 基础设施修复完成，DraftContextStoreImpl 已独立实现。T4 中的 P14 为 CRITICAL 级别缺陷（assist() 失败路径未清除旧 CRITICAL 告警，可能导致过期告警残留）；DraftContextCleanupTask 迁移依赖 T3 的 DraftContextStoreImpl 独立实现；enrichWithDrugInfo 死代码清理为独立任务可一并完成
上下文：P14 修复方案经审查修订：不再写入"AI不可用"伪CRITICAL告警（违反OOD语义），改为在失败路径清除旧CRITICAL告警；DraftContextCleanupTask 迁移不做键命名变更（当前清理逻辑对所有键一视同仁，无需特殊处理）；enrichWithDrugInfo 移除需配套修改两个测试文件（8个DrugFacade测试用例删除 + 构造器签名变更）

---

## R5 PASSED T4: 修复 PrescriptionAssistServiceImpl CRITICAL 告警清除（P14）+ DraftContextCleanupTask 模块迁移 + 移除 enrichWithDrugInfo 死代码
结果：P14 5个AI失败/降级路径插入clearCriticalAlerts调用、DraftContextCleanupTask从consultation迁移到prescription（包路径变更）、enrichWithDrugInfo死代码及DrugFacade注入从PrescriptionAssistServiceImpl和PrescriptionAuditServiceImpl移除。经3轮审查修订（r1: never()→正向验证+新增aiResultNotSuccess测试; r2: 正常路径ArgumentCaptor+幂等性times(2)+固定时间→未来时间; r3: dedupTaskScheduler mock补全）
测试：PrescriptionAssistServiceImplTest (32) + PrescriptionAuditServiceImplTest (42) + DraftContextCleanupTaskTest (12) + SuggestionCleanupTaskTest (8) = 94 prescription tests; common-module-api 111 tests; verify_v4.md: BUILD SUCCESS (364 total, 0 failures)

## R5 NEW T5: 修复 VisitIdReconciledTask 扫描条件（M03）+ SpecialPopulationDosageRule/DosageLimitRule 年龄/体重分级独立查询（P11）
任务：修复 prescription 模块中 M03（VisitIdReconciledTask 扫描条件修正 + findVisitIdByEncounterId 参数修正 + visitIdFallback 索引 + 悲观锁）和 P11（SpecialPopulationDosageRule/DosageLimitRule 年龄/体重分级独立查询 + 年龄阈值 @Value 可配置 + findBestMatch 六级优先级重写 + PatientInfo.weight）
选择理由：T4 prescription 核心服务修复完成，T5 为剩余 prescription 模块规则层缺陷，M03 影响数据一致性（扫描条件错误导致遗漏或重复），P11 为 CRITICAL 级别（年龄/体重分级查询耦合导致特殊人群剂量校验不准确）
上下文：M03 和 P11 均为 prescription 模块规则层问题，与 T4 的服务层修复无依赖关系，但需在 T4 完成后执行以确保全量测试通过

### 修订说明（v5 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| [严重] M03: findVisitIdByEncounterId 参数错误——使用 record.getPatientId() 而非 record.getVisitId() | 修正反查参数为 record.getVisitId()，补充降级写入逻辑说明 |
| [严重] P11: DosageStandardRepository 新增查询方法名称不一致 | 统一为 findByDrugCodeAndRouteOfAdministrationAndAgeRangeStartNotNullAndAgeRangeEndNotNull |
| [严重] P11: SpecialPopulationDosageRule 年龄阈值硬编码未修复 | 增加 @Value 注入 special-population.child-age-max:14 和 special-population.elderly-age-min:65 |
| [一般] P11: findBestMatch 六级优先级实现方案不完整 | 补充六级优先级关键逻辑点：Level 1 精确匹配、部分 null 边界处理、从 PatientInfo 获取 weight |
| [一般] M03: 缺少 visitIdFallback 索引和并发安全措施 | 补充 MedicalRecord @Table 索引 + findByVisitIdFallbackTrue @Lock(PESSIMISTIC_WRITE) |
| [轻微] P11: PatientInfo.weight OOD 引用错误 | 改为"OOD §8.4 六级匹配优先级隐含要求" |

---

## R6 PASSED T5: 修复 VisitIdReconciledTask 扫描条件（M03）+ SpecialPopulationDosageRule/DosageLimitRule 年龄/体重分级独立查询（P11）
结果：M03 VisitIdReconciledTask 扫描条件改为 findByVisitIdFallbackTrue（带悲观锁），反查参数改为 record.getVisitId()，反查成功后重置 visitIdFallback=false；MedicalRecord @Table 增加 visitIdFallback 索引。P11 SpecialPopulationDosageRule 新增 @Value 年龄阈值（childAgeMax/elderlyAgeMin）+ 专用查询 + 体重匹配；DosageLimitRule findBestMatch 重写为六级优先级匹配含部分 null 边界处理；PatientInfo 新增 weight 字段。经 1 轮审查修订（r1: agePartial+weightComplete 错误路由至 Level 4 修正、精度损失问题随严重问题一并删除、weightRange 部分 null 显式说明）。7 个源码文件修改 + 3 个测试文件修改 + 1 个额外测试文件适配。
测试：VisitIdReconciledTaskTest (11) + SpecialPopulationDosageRuleTest (23) + DosageLimitRuleTest (33) + PatientInfoTest (3) = 70 tests all passed；verify_v5.md: BUILD SUCCESS (891 total, 0 failures)
