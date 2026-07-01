# 计划审查报告（v5 r2）

## 审查结果
APPROVED

## 发现

### M03: VisitIdReconciledTask 扫描条件修正

- **[轻微]** task_v5.md 涉及文件列表中 `MedicalRecordRepository.java` 描述为"新增 findByVisitIdFallbackTrue 查询方法（带 @Lock PESSIMISTIC_WRITE）"，但 MedicalRecordRepository 当前扩展 JpaRepository（非 Repository），JpaRepository 支持 `@Lock` 注解，技术可行。但 `findByVisitIdFallbackTrue` 返回 `List<MedicalRecord>`，而 OOD §4.3 描述"每次读取一条待修复记录后立即锁定"——批量加悲观锁在并发场景下可能导致锁范围过大。不过当前为单实例部署场景，且定时任务串行执行，此问题不影响正确性，仅在高并发部署时需关注。

### P11: SpecialPopulationDosageRule/DosageLimitRule 年龄/体重分级独立查询

- **[轻微]** DosageStandardRepository 当前扩展 `Repository<DosageStandard, Long>`（非 JpaRepository），新增的 `findByDrugCodeAndRouteOfAdministrationAndAgeRangeStartNotNullAndAgeRangeEndNotNull` 方法依赖 Spring Data 查询派生。`Repository` 接口支持查询方法派生，技术可行。但方法名极长（约 80 字符），可读性差。不过 Spring Data 的 `NotNull` 关键字是标准派生语法，功能正确，不影响编译或运行。

- **[轻微]** task_v5.md P11 修复方案第 4 点 findBestMatch 六级优先级中，Level 2 "同时范围匹配"的条件描述为"ageRange 和 weightRange 均非 null 且非部分 null"，与 OOD §8.4 决策表 #1 一致。但 task_v5.md 第 107 行"部分 null 边界处理"描述"ageRangeStart/ageRangeEnd 仅一个非 null → 跳过 Level 1-3，仅尝试 Level 4-5"——此处"仅尝试 Level 4-5"不完全准确：若 weightRange 也部分 null，则 Level 4 也不满足，应直接降级至 Level 5（对应 OOD 决策表 #8）。不过 task_v5.md 第 109 行 Level 5 条件已正确描述"均完全 null"，实现时按 OOD 决策表逐行判断即可，此描述偏差不影响最终实现正确性。

- **[轻微]** PatientInfo 新增 weight 字段（Double, nullable）仅涉及 prescription 模块的 `dto/audit/PatientInfo.java`，但 ai-api 模块也有同名 `PatientInfo` 类（`com.aimedical.modules.ai.api.dto.prescription.PatientInfo`），同样缺少 weight 字段。task_v5.md 未提及 ai-api 模块的 PatientInfo 是否需要同步新增 weight。若 AI 请求路径需要传递 weight 信息，则 ai-api 的 PatientInfo 也需同步修改。但当前 task_v5.md 聚焦于本地规则引擎的 DosageLimitRule.check() 从 PatientInfo 获取 weight，而 DosageLimitRule 使用的是 `prescription.dto.audit.PatientInfo`，ai-api 的 PatientInfo 用于 AI 请求 DTO，两者职责不同。若 OOD 未要求 AI 请求传递 weight，则无需修改 ai-api 版本。此点不影响当前修复范围，但值得后续关注。

### 整体评估

- M03 修复方案与 OOD §4.3 完全对齐：扫描条件改为 `visitIdFallback=true`、反查参数使用 `record.getVisitId()`（即 fallback 写入的 encounterId）、反查成功后重置 `visitIdFallback=false`、悲观锁 + 索引。r1 审查提出的 3 个严重问题（参数错误、方法名不一致、年龄阈值硬编码）均已修正。
- P11 修复方案与 OOD §3.2 和 §8.4 对齐：专用查询方法、@Value 可配置年龄阈值、六级优先级重写、PatientInfo.weight 新增。r1 审查提出的 3 个严重 + 2 个一般问题均已修正。
- 涉及文件列表完整，覆盖了源码和测试文件的修改需求。
- 上一轮（v4）产出已通过全量测试验证（364 tests, 0 failures），T1-T4 均已 PASSED，T5 为最后一项未完成任务。
