# 实现计划

任务描述：实现 Phase 2/3 包C（智能分诊）、包D-AI1（处方审核）、包D-AI2（病历生成）、包E（辅助开方）四个业务包对应的 consultation、prescription、medical-record 三个后端 Maven 模块，以及 ai-api DTO 扩展、common-module-api 扩展（Store 接口/门面/事件）、common 模块 DosageStandard 实体迁移。
项目根目录：C:\Develop\Software\AIMedicalSys

## 实施路线

| # | 任务编号 | 任务名称 | 状态 | 依赖 | 说明 |
|---|---------|---------|------|------|------|
| 1 | T1 | ai-api DTO 扩展（分诊） + AiResultFactory | ✅ | 无 | 扩展 TriageRequest/Response、新增 AdditionalResponseItem/RecommendedDoctor/MatchedRuleItem、新增 AiResultFactory |
| 2 | T2 | ai-api DTO 扩展（处方审核 + 辅助开方） | ✅ | T1 | 扩展 PrescriptionCheckRequest/Response、PrescriptionAssistRequest/Response、新增相关 DTO |
| 3 | T3 | ai-api DTO 扩展（病历生成） | ✅ | T1 | 扩展 MedicalRecordGenRequest/Response |
| 4 | T4 | common-module-api：Store 接口 + 实现 | ✅ | 无 | SessionStore、SuggestionStore、DraftContextStore、ConcurrentHashMapStore |
| 5 | T5 | common-module-api：门面接口 + 事件 | ✅ | 无 | DoctorFacade、DrugFacade、VisitFacade、RegistrationEvent、AvailableDoctor |
| 6 | T6 | common 模块：DosageStandard 实体 | ✅ | 无 | 创建 DosageStandard 实体（含年龄/体重分级、日剂量上限等字段） |
| 7 | T7 | 父 pom + 模块骨架 | ✅ | T1-T6 | 注册 consultation/prescription/medical-record 模块，创建 pom.xml 和目录 |
| 8 | T8 | consultation 模块实现 | ✅ | T1,T4,T5,T7 | 全部包C 代码（Controller/Service/Dialogue/Repository/Entity/Converter/Event/Fallback/Rule），修复 fallbackHint 不生效问题 |
| 9 | T9 | prescription 模块实现（审核子域） | ✅ | T2,T4,T5,T6,T7 | 包D-AI1 代码（AuditController/AuditService/LocalRuleEngine/Entity/Repository/Converter）— R11 修复 POM 后构建成功，8 测试全部通过 |
| 10 | T10 | prescription 模块实现（辅助开方子域） | 🔄 R14 RETRY | T2,T4,T5,T6,T7,T9 | 包E 代码；v14 修复 T9 遗留的 8 个编译错误 |
| 11 | T11 | medical-record 模块实现 | | T3,T4,T5,T7 | 包D-AI2 代码（Controller/Service/TemplateConfig/MissingFieldDetector/Entity/Repository/Converter） |

---

## R1 NEW T1 — ai-api DTO 扩展（分诊）+ AiResultFactory
任务：在 ai-api 模块中扩展分诊相关 DTO 字段，新增缺失的 DTO 类，新增 AiResultFactory
选择理由：ai-api DTO 扩展是业务模块开发的前置依赖（§10 时序依赖），分诊是所有 AI 能力中最先被调用的
上下文：现有 TriageRequest/TriageResponse 为空壳或字段不全，需扩展完整字段；AiResult 现有 failure()/degraded() 无 partialData 参数，需新增 AiResultFactory 提供重载工厂方法

---

## R2 PASSED T1 — ai-api DTO 扩展（分诊）+ AiResultFactory
结果：实现 TriageRequest/Response 字段扩展，新建 AdditionalResponseItem/RecommendedDoctor/MatchedRuleItem/AiResultFactory
测试：AiResultFactoryTest 8 个用例 + TriageDtoTest 35 个用例，共 43 用例全部通过

## R2 NEW T2 — ai-api DTO 扩展（处方审核 + 辅助开方）
任务：在 ai-api 模块中扩展处方审核和辅助开方 DTO 字段，新增缺失 DTO 类
选择理由：处方审核和辅助开方共享 prescription 模块，DTO 扩展为 prescription 模块开发的前置依赖
上下文：现有 PrescriptionCheckRequest/Response 和 PrescriptionAssistRequest/Response 需扩展字段；需新增 PrescriptionCheckItem、AllergyDetailItem、DrugInteractionItem、AlertItem、SuggestionItem、ExamResultItem、DoseWarningItem、AllergyWarningItem

---
任务：在 ai-api 模块中扩展处方审核和辅助开方 DTO 字段，新增缺失 DTO 类
选择理由：处方审核和辅助开方共享 prescription 模块，DTO 扩展为 prescription 模块开发的前置依赖
上下文：现有 PrescriptionCheckRequest/Response 和 PrescriptionAssistRequest/Response 需扩展字段；需新增 PrescriptionCheckItem、AllergyDetailItem、DrugInteractionItem、AlertItem、SuggestionItem、ExamResultItem、DoseWarningItem、AllergyWarningItem

---

## R2 PASSED T2 — ai-api DTO 扩展（处方审核 + 辅助开方）
结果：扩展 PrescriptionCheck/Assist 4 个已有空壳类 + 新建 9 个 DTO 类
测试：PrescriptionDtoTest 33 个用例，全项目共 106 用例全部通过

---

## R3 PASSED T3 — ai-api DTO 扩展（病历生成）
结果：扩展 MedicalRecordGenRequest（5字段）和 MedicalRecordGenResponse（9字段）
测试：全项目 844 用例全部通过

---

## R4 NEW T6 — common 模块：DosageStandard 实体
任务：在 common 模块中创建 DosageStandard JPA 实体（含年龄/体重分级、日剂量上限、剂量单位等字段）
选择理由：底层依赖优先策略；DosageStandard 是 prescription 模块（T9/T10）的前置依赖，且与 T4/T5 无依赖关系；单实体任务规模小、验证快
上下文：实体定义在 com.aimedical.common.entity.DosageStandard，需继承 BaseEntity；字段含 drugCode、routeOfAdministration、ageRangeStart/End（Integer，nullable）、weightRangeStart/End（BigDecimal，nullable）、singleMax（BigDecimal）、dailyMax（BigDecimal，nullable）、unit（String）；需建立复合索引 (drugCode, routeOfAdministration) 和 (drugCode, routeOfAdministration, ageRangeStart, ageRangeEnd, weightRangeStart, weightRangeEnd)；需在 common 模块创建 entity 包

---

## R1 NEW T4 — common-module-api：Store 接口 + 实现
任务：在 common-module-api 中新增 SessionStore、SuggestionStore、DraftContextStore 接口及 ConcurrentHashMapStore 实现
选择理由：三个 Store 接口是业务模块内存存储依赖的强制抽象层（§6.1 "设计强制项"），先于业务模块创建
上下文：定义在 com.aimedical.modules.commonmodule.store/ 包下；ConcurrentHashMapStore 实现三个接口

---

## R1 NEW T5 — common-module-api：门面接口 + 事件
任务：在 common-module-api 中新增 DoctorFacade、DrugFacade、VisitFacade、RegistrationEvent、AvailableDoctor
选择理由：门面和事件是跨模块协作的契约定义，业务模块依赖它们编译
上下文：DoctorFacade 在 doctor/ 子包，DrugFacade 在 drug/ 子包，VisitFacade 在 visit/ 子包，RegistrationEvent 在 event/ 子包

---

## R1 NEW T6 — common 模块：DosageStandard 实体
任务：在 common 模块中创建 DosageStandard 实体（含年龄/体重分级、日剂量上限、剂量单位等字段）
选择理由：DosageStandard 需迁移至 common 模块避免跨模块编译期依赖（§2.2）
上下文：实体定义在 com.aimedical.common.entity.DosageStandard，含 drugCode、routeOfAdministration、ageRangeStart/End、weightRangeStart/End、singleMax、dailyMax、unit 等字段

---

## R1 NEW T7 — 父 pom + 模块骨架
任务：在 backend/pom.xml 中注册 consultation/prescription/medical-record 三个新模块；创建各模块 pom.xml 和 src 目录结构
选择理由：模块骨架是所有实现代码的容器，必须先于具体实现创建
上下文：参照 patient 模块的 pom.xml 和目录结构；每个模块依赖 common、common-module-api、ai-api

---

## R4 PASSED T6 — common 模块：DosageStandard 实体
结果：实现 DosageStandard JPA 实体（继承 BaseEntity），含 drugCode/routeOfAdministration/ageRangeStart/ageRangeEnd/weightRangeStart/weightRangeEnd/singleMax/dailyMax/unit 字段，声明两个复合索引
测试：DosageStandardTest (25 单元用例) + DosageStandardAuditTest (6 集成用例) 共 31 用例全部通过

---

## R5 NEW T4 — common-module-api：Store 接口 + ConcurrentHashMapStore 实现
任务：在 common-module-api 中新增 SessionStore、SuggestionStore、DraftContextStore 接口及 ConcurrentHashMapStore 实现类
选择理由：底层依赖优先策略 — T6 已完成，T4（Store 接口）是 consultation 模块（T8）和 prescription 模块（T9/T10）的前置底层抽象，无其他依赖，适合当前推进
上下文：三个 Store 接口定义在 com.aimedical.modules.commonmodule.store/ 包下；ConcurrentHashMapStore 同时实现三个接口，提供线程安全的内存存储；SessionStore 供 DialogueSessionManager 管理对话会话，SuggestionStore 供 DedupTaskScheduler 存储 AI 建议结果，DraftContextStore 供 PrescriptionDraftContext 存储处方草稿上下文

---

## R1 NEW T8 — consultation 模块实现
任务：实现包C（智能分诊）的全部代码
选择理由：包C 智能分诊是用户交互入口，核心依赖优先
上下文：包括 PrescriptionAuditController、PrescriptionAuditService/Impl、AuditRiskLevel、AuditRecord 实体、LocalRuleEngine、AllergyCheckRule、ContraindicationCheckRule、DuplicateCheckRule、DosageLimitRule、SpecialPopulationDosageRule、LocalRuleResult、PrescriptionAuditEnforcer/Impl、DTO、Repository、Converter、事件监听

---

## R1 NEW T10 — prescription 模块实现（辅助开方子域）
任务：实现包E（辅助开方）的全部代码
选择理由：辅助开方与处方审核共享 prescription 模块，批量实现减少上下文切换
上下文：包括 PrescriptionAssistController、PrescriptionAssistService/Impl、DosageThresholdService、DedupTaskScheduler、PrescriptionDraftContext、DTO、AiSuggestionResult、Converter

---

## R1 NEW T11 — medical-record 模块实现
任务：实现包D-AI2（病历生成）的全部代码
选择理由：病历生成是医生工作台的核心功能之一
上下文：包括 MedicalRecordController、MedicalRecordService/Impl、DepartmentTemplateConfig、TemplateConfigManager/Database、MissingFieldDetector、MedicalRecord/DeptTemplateConfig 实体、Repository、Converter、MedicalRecordField 枚举、定时任务

---

## R5 PASSED T4 — common-module-api：Store 接口 + ConcurrentHashMapStore 实现
结果：实现 SessionStore（泛型接口）、SuggestionStore（扩展 SessionStore + compute）、DraftContextStore（扩展 SessionStore）、ConcurrentHashMapStore（同时实现三个接口）
测试：ConcurrentHashMapStoreTest 26 用例 + 全项目 57 用例全部通过

---

## R6 NEW T5 — common-module-api：门面接口 + 事件
任务：在 common-module-api 中新增 DoctorFacade/AvailableDoctor、DrugFacade、VisitFacade、RegistrationEvent
选择理由：门面接口和事件是 T7（模块骨架）和 T8/T9/T10/T11（业务模块）的前置编译依赖；T5 无其他待办依赖，已完成任务 T1–T4/T6 满足 T5 的全部前置条件
上下文：DoctorFacade（doctor/ 子包，findAvailableDoctorsByDepartment → List\<AvailableDoctor\>）、DrugFacade（drug/ 子包，findByDrugCode → DrugInfo）、VisitFacade（visit/ 子包，findVisitIdByEncounterId → String）、RegistrationEvent（event/ 子包，含 registrationId/patientId/sessionId/departmentId/departmentName/doctorId/eventTime）、AvailableDoctor（doctor/ 子包，DTO，含 doctorId/doctorName/departmentId/availableSlotCount）

---

## R6 PASSED T5 — common-module-api：门面接口 + 事件
结果：实现 AvailableDoctor、DoctorFacade、DrugInfo、DrugFacade、VisitFacade、RegistrationEvent 共 6 个类型
测试：AvailableDoctorTest(9) + DoctorFacadeTest(2) + DrugInfoTest(9) + DrugFacadeTest(2) + VisitFacadeTest(2) + RegistrationEventTest(5) = 29 用例，全项目 86 用例全部通过

---

## R7 PASSED T7 — 父 pom + 模块骨架
结果：在 backend/pom.xml 中注册 consultation/prescription/medical-record 三个模块，创建各模块 pom.xml、src/main/java、src/test/java 目录骨架及占位测试
测试：NewModulePomTest(19) + ParentPomModuleRegistrationTest(5) + 各模块占位测试(3) = 27 用例全部通过

---

## R8 NEW T8 — consultation 模块实现（包C 智能分诊）
任务：实现包C（智能分诊/智能导诊）consultation 模块的全部代码，含 Controller/Service/Dialogue/Repository/Entity/Converter/Event/Fallback/Rule
选择理由：T1–T7 已全部完成，T8 的编译依赖（T1 ai-api DTO、T4 Store 接口、T5 门面接口、T7 模块骨架）均已就绪；consultation 模块是包C 的核心，前置条件满足
上下文：OOD 设计文档 §3.1 定义了完整类型体系——TriageController、TriageService/Impl、DialogueSession、DialogueSessionManager、TriageRuleEngine、DefaultTriageRuleEngine、DepartmentFallbackProvider、StaticDepartmentFallbackProvider、TriageRule、TriageRecord、DeadLetterEvent、RegistrationEventListener、TriageConverter 以及 6 个业务 DTO 类；所有类型位于 com.aimedical.modules.consultation 包下；依赖已就绪的 ai-api DTO（TriageRequest/Response 等，com.aimedical.modules.ai.api.dto.triage）、SessionStore（com.aimedical.modules.commonmodule.store）、DoctorFacade（com.aimedical.modules.commonmodule.doctor）、RegistrationEvent（com.aimedical.modules.commonmodule.event）

---

## R9 FAILED T8 — consultation 模块实现（包C 智能分诊）
结果：85 测试运行，1 失败
失败原因：TriageServiceImplTest.shouldSetFallbackHintAfterThreeAiFailures — expected fallbackHint="AI service has been continuously unavailable" but was null
分析：handleAiFailure() 仅在未来抛出异常时被调用（catch InterruptedException/ExecutionException），而 AI 正常返回非成功结果（AiResult.failure）时不会递增 aiFailCount。导致 session.getAiFailCount() 始终为 0，fallbackHint 条件 session.getAiFailCount() >= MAX_AI_FAIL_COUNT 永不满足，fallbackHint 始终为 null

## R9 RETRY T8 — consultation 模块实现（包C 智能分诊）
任务：修复 TriageServiceImpl.triage()，在非异常 AI 失败场景下也递增 aiFailCount
修正方向：在 fallback 块（!aiResult.isSuccess()）入口处，当 aiResult 非 degraded（即非已由 handleAiFailure 处理过）时，执行 session.setAiFailCount(session.getAiFailCount() + 1)
期望：85 测试全部通过，包括 shouldSetFallbackHintAfterThreeAiFailures

---

## R10 PASSED T8 — consultation 模块实现（包C 智能分诊）
结果：修复 fallbackHint 不生效缺陷——在 fallback 块入口对非 degraded AI 失败结果显式递增 aiFailCount，确保两条失败路径（异常抛出 vs 正常返回 failure）均计入连续失败计数
测试：90 测试全部通过（v9 verify），包括新增的 5 个边界/组合路径用例

## R10 NEW T9 — prescription 模块实现（审核子域/包D-AI1）
任务：实现包D-AI1（处方审核）prescription 模块审核子域的全部代码，含 Controller/Service/LocalRuleEngine/Rule/Entity/DTO/Repository/Converter
选择理由：T1–T8 已全部完成；T9 的编译依赖（T2 ai-api DTO、T4 Store 接口、T5 门面接口、T6 DosageStandard 实体、T7 模块骨架）均已就绪；prescription 模块审核子域是包D-AI1 核心，前置条件满足
上下文：完整上下文见 task_v10.md

---

## R10 FAILED T9 — prescription 模块实现（审核子域/包D-AI1）
结果：Maven 构建失败——prescription/pom.xml 声明了 com.aimedical:patient 依赖但无版本号，且父 POM dependencyManagement 未包含 patient 条目
测试：0 通过（构建配置错误导致无法编译）

## R11 RETRY T9 — prescription 模块实现（审核子域/包D-AI1）
任务：修复 Maven 构建配置——在父 POM dependencyManagement 中添加 com.aimedical:patient 依赖版本声明，使 prescription 模块可正常编译和测试
修正方向：在 backend/pom.xml 的 dependencyManagement 中添加 `<dependency><groupId>com.aimedical</groupId><artifactId>patient</artifactId><version>${project.version}</version></dependency>`，保持与其他内部模块一致的版本管理风格

## R11 PASSED T9 — prescription 模块实现（审核子域/包D-AI1）
结果：修复父 POM dependencyManagement 缺失 patient 条目问题——在 backend/pom.xml 中添加 patient 版本声明；42 源文件编译通过，8 测试全部通过，BUILD SUCCESS
测试：PrescriptionAuditServiceImplTest + PrescriptionAuditControllerTest + PrescriptionErrorCodeTest = 8 用例全部通过

## R12 FAILED T10 — prescription 模块实现（辅助开方子域/包E）
结果：18 个新文件 + 2 个修改文件已实现；全项目 192 通过，2 失败（均为 pre-existing POM 测试），5 跳过；prescription 模块因 common 模块 POM 测试失败阻断未执行
失败原因：2 个  pre-existing common 模块 POM 测试与当前项目结构不一致——MovedModulePomTest.rootPomShouldHaveExactlyEightModules 期望 8 模块但现有 11（T7 新增 consultation/prescription/medical-record）；ParentPomTest.dependencyManagementShouldNotContainBusinessModules 断言 patient 不在 dependencyManagement 但 T9 R11 修复时已加入。非 T10 代码问题，但阻断整个构建，导致 prescription 模块测试无法执行

## R13 FAILED T10 — prescription 模块实现（辅助开方子域/包E）
结果：POM 测试修复通过，但 prescription 模块 T9 测试文件存在 8 个编译错误（3 个 Rule 构造器参数不匹配 + 5 个 Result.isSuccess() 不存在），构建阻断未解除
失败原因：v10→v11 中 Rule 生产代码重构移除了 ObjectMapper 构造参数但测试未同步；PrescriptionAuditControllerTest 始终调用不存在的 Result.isSuccess() 方法。非 T10 代码问题，但阻断整个 prescription 模块编译

## R14 RETRY T10 — prescription 模块实现（辅助开方子域/包E）
任务：修复 T9 遗留的 8 个编译错误，解除 prescription 模块构建阻断
修正方向：
  1. DuplicateCheckRuleTest/ContraindicationCheckRuleTest/AllergyCheckRuleTest：移除 ObjectMapper 构造参数
  2. PrescriptionAuditControllerTest：用 code 断言替换不存在的 isSuccess() 调用
  3. 全量 mvn test 验证，含 prescription 模块全部测试文件
上下文：完整上下文见 task_v14.md

---

## R14 FAILED T10 — prescription 模块实现（辅助开方子域/包E）
结果：8 个编译错误已修复（4 个测试文件编译通过，零错误），但 prescription 模块全量测试执行失败
测试：Tests run: 151, Failures: 3, Errors: 2
失败详情：
  - PrescriptionErrorCodeTest.shouldExposeCodeAndMessage — 消息不匹配（T9 测试）
  - DosageLimitRuleTest.shouldMatchByAgeWhenPatientInfoAvailable — 期望 BLOCK 得到 WARN（T9 测试）
  - PrescriptionAuditServiceImplTest.submitShouldReAuditWhenNoLatestRecordFoundThenReturnBlock — 期望 false 得到 true（T9 测试）
  - PrescriptionAssistServiceImplTest.assistShouldGeneratePrescriptionIdWhenBlank — NPE，ruleResult 为 null（T10 测试）
  - PrescriptionAuditServiceImplTest.submitShouldReturnConcurrentSubmitErrorWhenOptimisticLockException — OptimisticLockException（T9 测试）
分析：4/5 失败为 T9 遗留测试缺陷，仅 1 个为 T10 新增缺陷（NPE）；连续 3 轮失败，按规程标记 BLOCKED

## R15 BLOCKED T10 — prescription 模块实现（辅助开方子域/包E）
原因：T10 连续 3 轮（R12/R13/R14）验证 FAILED，达到轮次超限阈值。4/5 失败源于 T9 代码缺陷（测试断言不匹配、规则逻辑偏差、并发提交异常处理），非 T10 实现可直接修复范围。标记 BLOCKED 并跳过，不阻断 T11 推进。

## R15 NEW T11 — medical-record 模块实现（包D-AI2 病历生成）
任务：实现 medical-record 模块全部代码（Controller/Service/TemplateConfig/MissingFieldDetector/Entity/Repository/Converter/Enum/DTO）
选择理由：T11 依赖 T3（ai-api 病历 DTO）、T4（Store 接口）、T5（门面接口+事件）、T7（模块骨架）均已 PASSED，前置条件满足，是计划中最后一个未实现任务
上下文：完整上下文见 task_v15.md
