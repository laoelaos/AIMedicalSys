# R1.3: 包D-AI2 病历生成（medical-record 模块）完整实现审查

审查时间：2026-06-29

### 审查范围

主代码：
- `AIMedical/backend/modules/medical-record/src/main/java/com/aimedical/modules/medicalrecord/api/MedicalRecordController.java`
- `AIMedical/backend/modules/medical-record/src/main/java/com/aimedical/modules/medicalrecord/converter/MedicalRecordConverter.java`
- `AIMedical/backend/modules/medical-record/src/main/java/com/aimedical/modules/medicalrecord/converter/MedicalRecordContentConverter.java`
- `AIMedical/backend/modules/medical-record/src/main/java/com/aimedical/modules/medicalrecord/detector/MissingFieldDetector.java`
- `AIMedical/backend/modules/medical-record/src/main/java/com/aimedical/modules/medicalrecord/detector/MissingFieldDetectorImpl.java`
- `AIMedical/backend/modules/medical-record/src/main/java/com/aimedical/modules/medicalrecord/dto/RecordGenerateRequest.java`
- `AIMedical/backend/modules/medical-record/src/main/java/com/aimedical/modules/medicalrecord/dto/RecordGenerateResponse.java`
- `AIMedical/backend/modules/medical-record/src/main/java/com/aimedical/modules/medicalrecord/dto/FieldMissingHint.java`
- `AIMedical/backend/modules/medical-record/src/main/java/com/aimedical/modules/medicalrecord/entity/MedicalRecord.java`
- `AIMedical/backend/modules/medical-record/src/main/java/com/aimedical/modules/medicalrecord/entity/DeptTemplateConfig.java`
- `AIMedical/backend/modules/medical-record/src/main/java/com/aimedical/modules/medicalrecord/enums/MedicalRecordField.java`
- `AIMedical/backend/modules/medical-record/src/main/java/com/aimedical/modules/medicalrecord/exception/MedicalRecordErrorCode.java`
- `AIMedical/backend/modules/medical-record/src/main/java/com/aimedical/modules/medicalrecord/repository/MedicalRecordRepository.java`
- `AIMedical/backend/modules/medical-record/src/main/java/com/aimedical/modules/medicalrecord/repository/DeptTemplateConfigRepository.java`
- `AIMedical/backend/modules/medical-record/src/main/java/com/aimedical/modules/medicalrecord/service/MedicalRecordService.java`
- `AIMedical/backend/modules/medical-record/src/main/java/com/aimedical/modules/medicalrecord/service/impl/MedicalRecordServiceImpl.java`
- `AIMedical/backend/modules/medical-record/src/main/java/com/aimedical/modules/medicalrecord/template/TemplateConfigManager.java`
- `AIMedical/backend/modules/medical-record/src/main/java/com/aimedical/modules/medicalrecord/template/DatabaseTemplateConfigManager.java`
- `AIMedical/backend/modules/medical-record/src/main/java/com/aimedical/modules/medicalrecord/template/DepartmentTemplateConfig.java`

关联引用（仅审阅，不产出评价）：
- `AIMedical/backend/modules/ai/ai-api/src/main/java/com/aimedical/modules/ai/api/AiService.java`
- `AIMedical/backend/modules/ai/ai-api/src/main/java/com/aimedical/modules/ai/api/AiResult.java`
- `AIMedical/backend/modules/ai/ai-api/src/main/java/com/aimedical/modules/ai/api/AiResultFactory.java`
- `AIMedical/backend/modules/ai/ai-api/src/main/java/com/aimedical/modules/ai/api/dto/medicalrecord/MedicalRecordGenRequest.java`
- `AIMedical/backend/modules/ai/ai-api/src/main/java/com/aimedical/modules/ai/api/dto/medicalrecord/MedicalRecordGenResponse.java`
- `AIMedical/backend/modules/common-module/common-module-api/src/main/java/com/aimedical/modules/commonmodule/visit/VisitFacade.java`

测试代码（覆盖度参考）：
- `AIMedical/backend/modules/medical-record/src/test/java/com/aimedical/modules/medicalrecord/**`

### 审查依据

主依据：`Docs/07_ood_phase2_C_3_DE.md` §3.3 包D-AI2 病历生成（v15 终稿）
辅助参考：`Docs/01_requirement.md` §3.4.3 错误码清单与设计文档 §5.1 错误码-HTTP 状态码映射

### 维度速览

| 维度 | 评价 |
|------|------|
| MedicalRecordField 枚举 7 值 | ✅ 与设计文档 §3.3 表格完全一致 |
| MedicalRecord 实体结构 | ⚠️ 列名/字段名偏差；doctorId 字段未被使用 |
| RecordGenerateRequest 字段 | ❌ 缺少 @NotNull/@Size 验证注解（与设计文档 "50–10000 字符" 约束脱节）|
| RecordGenerateResponse 字段 | ✅ 字段齐全（fields / missingFieldHints / fromFallback / degraded / errorCode / success）|
| MedicalRecordServiceImpl 主流程 | ⚠️ 降级路径部分实现；并发保护仅对更新有效；超时配置硬编码 |
| TemplateConfigManager 缓存 | ❌ 缺少 TemplateConfigChangeEvent 事件监听器（仅 Caffeine 兜底）|
| DepartmentTemplateConfig | ✅ 结构正确 |
| MissingFieldDetectorImpl 判定 | ✅ "非空非 null" 判定 + 默认文案占位符替换符合设计 |
| 超时降级路径 | ⚠️ 调用方在 timeout 路径下未捕获 future 中可能的 partialData；AiResultFactory 未使用 |
| 并发写保护 | ❌ 现有流程仅新增记录，不会触发 OptimisticLockException → MR_GEN_CONCURRENT_MODIFICATION 路径形同虚设 |
| DTO 字段对齐 | ⚠️ ai-api 层 DTO（missingFields / partialContent）未被业务层消费 |
| 错误码 MedicalRecordErrorCode | ❌ 4/8 错误码缺失 |
| MedicalRecordContentConverter | ✅ JSON 序列化/反序列化逻辑完备（含异常兜底） |

### 发现

#### [严重] 1. 错误码缺失：MR_GEN_AI_UNAVAILABLE / MR_GEN_AI_INPUT_INVALID / MR_GEN_AI_OUTPUT_INCOMPLETE / MR_GEN_TEMPLATE_LOAD_FAILED 4 个错误码未实现

- **位置**：`AIMedical/backend/modules/medical-record/src/main/java/com/aimedical/modules/medicalrecord/exception/MedicalRecordErrorCode.java:5-9`
- **描述**：设计文档 §5.1 明确列出病历（AI）类错误码 MR_GEN_AI_TIMEOUT、MR_GEN_AI_UNAVAILABLE、MR_GEN_AI_INPUT_INVALID、MR_GEN_AI_OUTPUT_INCOMPLETE，病历（非AI）类错误码 MR_GEN_STREAM_NOT_SUPPORTED、MR_GEN_VISIT_NOT_FOUND、MR_GEN_CONCURRENT_MODIFICATION、MR_GEN_TEMPLATE_LOAD_FAILED，共 8 个。当前 MedicalRecordErrorCode 枚举仅实现 4 个：MR_GEN_VISIT_NOT_FOUND、MR_GEN_AI_TIMEOUT、MR_GEN_STREAM_NOT_SUPPORTED、MR_GEN_CONCURRENT_MODIFICATION。缺失的 4 个错误码在 AiService 失败（UNAVAILABLE）、输入校验失败（INPUT_INVALID）、输出不完整（OUTPUT_INCOMPLETE）、模板加载失败（TEMPLATE_LOAD_FAILED）时无对应错误码可返回，会导致 GlobalExceptionHandler 无法正确映射 HTTP 状态码，需求文档 §3.4.3 错误码补齐要求亦无法达成。
- **建议**：补齐全部 8 个错误码。其中 MR_GEN_AI_INPUT_INVALID 应在 MedicalRecordServiceImpl 检测到 dialogueText 长度 < 50 或为空时返回；MR_GEN_AI_UNAVAILABLE 应在 aiService.generateMedicalRecord() 出现非超时/输入校验类失败（如 IOException、网络中断）时返回；MR_GEN_AI_OUTPUT_INCOMPLETE 应在 AI 返回的 MedicalRecordGenResponse 中 7 个字段全空时返回；MR_GEN_TEMPLATE_LOAD_FAILED 应在 DatabaseTemplateConfigManager.loadFromDatabase 解析失败时返回（当前实现降级为 DEFAULT 模板，与设计文档 §2.3 描述的"模板加载失败"语义不一致——设计允许 silent fallback，但 502 错误码应作为可选暴露路径之一）。

#### [严重] 2. TemplateConfigChangeEvent 事件驱动缓存刷新未实现

- **位置**：`AIMedical/backend/modules/medical-record/src/main/java/com/aimedical/modules/medicalrecord/template/DatabaseTemplateConfigManager.java:31-41`
- **描述**：设计文档 §2.3 §9 明确要求"所有可缓存规则实体（包括科室模板配置）均采用'事件驱动立即失效 + 定时刷新兜底'策略"；§9 列出 TemplateConfigChangeEvent（定义位置 medical-record/event/，发布模块 admin，消费模块 medical-record（TemplateConfigManager））；§7 设计决策表再次强调"TemplateConfigManager（监听 TemplateConfigChangeEvent）"。当前实现仅配置 Caffeine `refreshAfterWrite(60, TimeUnit.SECONDS)` 兜底刷新，**完全缺少 @EventListener / @TransactionalEventListener 监听 TemplateConfigChangeEvent 的代码**。这导致：admin 端更新科室模板后，病历生成服务最长需等待 60 秒才感知变更（违反"事件驱动立即失效"设计目标）。同时 `medical-record/event/` 目录根本未创建，TemplateConfigChangeEvent 事件类本身也不存在。
- **建议**：
  1. 新增 `TemplateConfigChangeEvent`（建议位置 `com.aimedical.modules.medicalrecord.event.TemplateConfigChangeEvent`，发布者由 admin 模块编写——这超出本模块范围，但消费端应先就绪）
  2. 在 DatabaseTemplateConfigManager 上添加 `@TransactionalEventListener(phase = AFTER_COMMIT)` 监听器，调用 `templateCache.invalidate(departmentId)` 实现立即失效
  3. 当前 60 秒 Caffeine 兜底保留作为事件丢失补偿

#### [严重] 3. VisitIdReconciledTask 定时调和任务未实现

- **位置**：`AIMedical/backend/modules/medical-record/` （整个 task 子包不存在）
- **描述**：设计文档 §3.3 VisitIdReconciledTask 定义（行 1505）明确要求："medical-record 模块的 VisitIdReconciledTask（com.aimedical.modules.medicalrecord.task.VisitIdReconciledTask）为定时 reconciled 任务，每 30 分钟由 Spring @Scheduled(cron = "0 */30 * * * ?") 调度。核心职责：扫描 MedicalRecord 表中 visitIdFallback=true 且 visitId 与 encounterId 一致的记录，通过 VisitFacade.findVisitIdByEncounterId(encounterId) 反查正确 visitId。"当前 medical-record 模块完全没有 `task` 子包或 `VisitIdReconciledTask` 类。这导致：当 encounterId 作为 visitId fallback 写入数据库后，**没有任何机制将 visitIdFallback=true 的记录调和为正确 visitId**。该字段在 MedicalRecord 实体（MedicalRecord.java:42）有定义、在 MedicalRecordServiceImpl.java:101 有写入，但永远不会被清除。
- **建议**：新增 `com.aimedical.modules.medicalrecord.task.VisitIdReconciledTask`，按设计文档契约实现 @Scheduled 扫描、@Lock(PESSIMISTIC_WRITE) 锁定、反查 VisitFacade、更新 visitId 后重置 visitIdFallback=false 的流程。

#### [严重] 4. MR_GEN_CONCURRENT_MODIFICATION 错误码路径无法触发（OptimisticLockException 仅在更新时抛出）

- **位置**：`AIMedical/backend/modules/medical-record/src/main/java/com/aimedical/modules/medicalrecord/service/impl/MedicalRecordServiceImpl.java:102-110`
- **描述**：MedicalRecordServiceImpl.generate() 主流程在 102-103 行调用 `medicalRecordRepository.save(entity)` 后立即捕获 `ObjectOptimisticLockingFailureException`。但 `entity` 是新建对象（`recordId = null`，未通过 `findByVisitId` 读取后修改），JPA 的 @Version 乐观锁仅在 UPDATE 提交时校验版本号，**INSERT 路径不会抛出 OptimisticLockException**。因此该 try-catch 块在生产环境中永远不会被触发，MR_GEN_CONCURRENT_MODIFICATION 错误码与对应的 HTTP 409 状态码映射形同虚设。设计文档 §3.3 描述的并发写保护是针对"读取 contentJson → 合并变更字段 → 写回"（增量更新）场景，但 MedicalRecordServiceImpl 当前的 generate() 流程没有"读取已有记录后增量更新"逻辑；该并发保护在 `findByVisitId()` 调用方或未来增量更新端点中才有意义。
- **建议**：
  - 若 Phase 2/3 不存在病历增量更新场景，则该异常处理块应删除（避免误导后续维护者）
  - 若后续 Phase 引入增量更新，应将 try-catch 移到真正的更新逻辑处（如 `updateFields` 端点），而非当前的 generate() 主路径
  - 或在 save() 后立即调用 `saveAndFlush()` 配合 `flush()` 后再做版本检查，以便在并发重复请求插入场景下捕获主键/唯一约束冲突——但这与设计文档描述的"版本号不一致"语义不完全一致

#### [一般] 5. RecordGenerateRequest 缺少输入验证注解

- **位置**：`AIMedical/backend/modules/medical-record/src/main/java/com/aimedical/modules/medicalrecord/dto/RecordGenerateRequest.java:1-49`
- **描述**：设计文档 §3.3 RecordGenerateRequest 明确要求 dialogueText 必填且字符数 50–10000，patientId 必填。MedicalRecordController.java:27 使用了 `@Valid` 注解，但 RecordGenerateRequest 类本身没有 `@NotNull`、`@NotBlank`、`@Size(min=50, max=10000)` 等约束注解。这意味着 @Valid 不会触发任何校验，前端可以发送空 dialogueText 或超长 dialogueText 直达 Service 层。需求文档 §3.4.3 错误码清单中"MR_GEN_AI_INPUT_INVALID" 用于覆盖此类输入校验失败场景（设计文档 §2.3 行 388 明示），但当前 controller 端无拦截，会导致 dialogueText 长度违规的请求被传入 AiService，由 MockAiService 或后续真实 AI 实现返回更模糊的错误。
- **建议**：在 RecordGenerateRequest 字段上添加 jakarta.validation 注解：
  - `dialogueText`: `@NotBlank @Size(min = 50, max = 10000)`
  - `patientId`: `@NotBlank`
  - `encounterId`: `@NotBlank`（在 controller 中已通过 stream 检查先行短路，但 encounterId 实际为必填，否则降级为 MR_GEN_VISIT_NOT_FOUND——这与需求文档"必填"语义不冲突但需要明确）
  - 同时新增 `@ControllerAdvice` 或 `GlobalExceptionHandler` 中处理 `MethodArgumentNotValidException`，映射为 `MR_GEN_AI_INPUT_INVALID` 错误码

#### [一般] 6. 超时配置硬编码，未通过 @Value 注入

- **位置**：`AIMedical/backend/modules/medical-record/src/main/java/com/aimedical/modules/medicalrecord/service/impl/MedicalRecordServiceImpl.java:124, 138`
- **描述**：设计文档 §3.3 明确"VisitFacade 配置独立超时阈值（默认 medical-record.visit-facade.timeout=2s，通过 @Value 或 RestTemplate 超时配置注入）"与"超时配置：ai.timeout.medical-record-generate=12s（非流式）"；§5.5 也列出了 `ai.timeout.medical-record-generate=12s` 与 `medical-record.visit-facade.timeout=2s` 两个配置键。当前实现两个超时值都直接硬编码在 `future.get(2, TimeUnit.SECONDS)` 与 `future.get(12, TimeUnit.SECONDS)` 调用中，未通过 `@Value("${medical-record.visit-facade.timeout:2000}")` 或 `@Value("${ai.timeout.medical-record-generate:12000}")` 注入。导致：环境/性能调优时需修改代码重新发布。
- **建议**：
  - 类级别添加 `@Value("${medical-record.visit-facade.timeout:2000}") long visitFacadeTimeoutMs`
  - 类级别添加 `@Value("${ai.timeout.medical-record-generate:12000}") long aiTimeoutMs`
  - `future.get(visitFacadeTimeoutMs, TimeUnit.MILLISECONDS)` 与 `future.get(aiTimeoutMs, TimeUnit.MILLISECONDS)` 替换硬编码

#### [一般] 7. converter.toRecordGenerateResponse 对非超时失败的处理不完整

- **位置**：`AIMedical/backend/modules/medical-record/src/main/java/com/aimedical/modules/medicalrecord/converter/MedicalRecordConverter.java:43-58`
- **描述**：converter.toRecordGenerateResponse 在 53-55 行**仅**对 errorCode == "MR_GEN_AI_TIMEOUT" 的情况设置 `response.setErrorCode(MedicalRecordErrorCode.MR_GEN_AI_TIMEOUT)`，其他错误码（如 MR_GEN_AI_UNAVAILABLE、MR_GEN_AI_INPUT_INVALID、MR_GEN_AI_OUTPUT_INCOMPLETE）则不会写入 response.errorCode。但更关键的问题是：converter 始终将 `response.setSuccess(true)`（行 56）——这意味着 AI 调用失败（非超时）时，response 仍然 success=true，controller 在 `if (response.isSuccess())` 判断时返回 Result.success(response)，**前端完全感知不到 AI 失败**。测试 `toRecordGenerateResponseShouldHandleNullData`（MedicalRecordConverterTest.java:89-97）显式断言该行为符合预期，但这与设计文档 §3.3 描述的"前端对已生成字段正常渲染，缺失字段显示补全提示"（仅针对超时场景）和"完全降级"场景（degraded=true + 空 fields）需要分别暴露错误的语义不一致。
- **建议**：重构 converter 逻辑：
  - 当 `aiResult.isSuccess() == false && !aiResult.isDegraded()`（硬失败）：设置 `response.setSuccess(false)` + 映射 errorCode，让 controller 短路返回 Result.fail
  - 当 `aiResult.isDegraded() == true`（部分降级）：保留 `response.setSuccess(true)` + 设置 degraded=true + errorCode
  - 当 `aiResult.isSuccess() == true`（正常）：success=true，不设 errorCode
  - 当前实现将 success=true 一刀切，等于把"硬失败"也包装成"成功但有降级标记"，前端无法区分降级与失败

#### [一般] 8. AiResultFactory 未被使用，AiResult 对象手工构造

- **位置**：`AIMedical/backend/modules/medical-record/src/main/java/com/aimedical/modules/medicalrecord/service/impl/MedicalRecordServiceImpl.java:141-146, 149-154, 157-162`
- **描述**：设计文档 §2.3 与 §7 设计决策表明确"超时/降级重载工厂方法统一迁移至 AiResultFactory"——其设计意图是"通过新类型扩展而非修改现有类实现兼容"，并指出 AiResult 的 failure(String) / degraded(String) 原始签名保持不变。ai-api 模块已经新增 AiResultFactory（AiResultFactory.java:1-23）提供 `failure(String errorCode, T partialData)` 和 `degraded(String fallbackReason, T partialData)` 重载。但 MedicalRecordServiceImpl 在三个 catch 块中手工 `new AiResult<>()` 并逐个 setSuccess/setErrorCode/setDegraded/setData，**完全未使用 AiResultFactory**。这导致：
  - 与设计文档决策偏离
  - 代码冗长（三处 try-catch 各 5 行 setter）
  - 当 AiResult 字段扩展时易遗漏
- **建议**：将三处手工构造替换为：
  ```java
  return AiResultFactory.degraded("AI_TIMEOUT", null);  // 三个 catch 块统一
  ```
  或更精确地：
  ```java
  return AiResultFactory.failure("MR_GEN_AI_TIMEOUT", null);
  ```
  设计文档行 363-364 明确列出了这两个重载的语义。

#### [一般] 9. MedicalRecord 实体的 doctorId 字段从未被赋值

- **位置**：`AIMedical/backend/modules/medical-record/src/main/java/com/aimedical/modules/medicalrecord/entity/MedicalRecord.java:40, 90-96`
- **描述**：设计文档 §3.3 明确 MedicalRecord 实体包含"医生 ID（doctorId）"字段。MedicalRecord.java:40 定义该字段、:90-96 提供 getter/setter。但 MedicalRecordServiceImpl.generate() 在新建 MedicalRecord 时（行 94-101）仅设置 patientId、visitId、departmentId、content、visitIdFallback，**未调用 entity.setDoctorId(...)**。这导致：所有持久化的病历记录的 doctorId 列都是 NULL（数据库表 schema 默认允许 NULL，因为该字段没有 @Column(nullable=false)），与设计文档语义偏离。后续医生端"我的病历"按 doctorId 过滤功能将无法工作。
- **建议**：
  - 短期：RecordGenerateRequest 增加 doctorId 字段（需求文档 §3.4.3 输入契约未明示，但设计文档 §3.3 实体描述明确要求），Service 层从前端 CurrentUser 或 SecurityContext 提取当前医生 ID
  - 或：从 `com.aimedical.modules.commonmodule.auth.CurrentUser` 注入当前用户
  - 同时评估是否需要 @Column(nullable = false)——若业务上病历必须关联医生，nullable=false 更严格

#### [一般] 10. MedicalRecord 实体 content 字段列名与设计文档 contentJson 不一致

- **位置**：`AIMedical/backend/modules/medical-record/src/main/java/com/aimedical/modules/medicalrecord/entity/MedicalRecord.java:36-38`
- **描述**：设计文档 §3.3 明确"病历内容（contentJson，TEXT 类型 + JPA @Convert/Jackson 序列化，存储为单列 JSON）"。当前实体字段名为 `content`（MedicalRecord.java:38），由于没有显式 `@Column(name = "content_json")`，JPA 自动生成的列名为 `content`。Hibernate 默认策略下，列名是字段名（content）而非 snake_case（content_json）。这与设计文档的命名约定（contentJson）不一致——数据库 schema 将生成 `content TEXT` 而非 `content_json TEXT`。
- **建议**：将字段名改为 `contentJson` 或显式标注 `@Column(name = "content_json")`。同步需更新 MedicalRecordContentConverter 的 import 路径以及 MedicalRecordTest 的字段访问（如有）。当前测试 MedicalRecordTest.java:18, 33 使用 `getContent()`/`setContent()`，与字段名一致即可，但 DDL 一旦生成 `content` 列名就难以变更（除非使用 Flyway/Liquibase 显式管理 schema）。

#### [一般] 11. ai-api 层 MedicalRecordGenResponse.missingFields 与 partialContent 字段未被消费

- **位置**：`AIMedical/backend/modules/medical-record/src/main/java/com/aimedical/modules/medicalrecord/converter/MedicalRecordConverter.java:21-31`
- **描述**：ai-api 层 MedicalRecordGenResponse（MedicalRecordGenResponse.java:13-14, 75-89）定义了 `missingFields: List<String>` 和 `partialContent: Object` 两个字段。设计文档 §4.5 MedicalRecordConverter 行 1114-1115 明确这两个字段属于 ai-api 层扩展。但 MedicalRecordConverter.toFieldsMap()（行 21-31）**只映射 7 个 string 字段**，missingFields 与 partialContent 完全被忽略：
  - missingFields 是 AI 自行识别的缺失字段清单，业务层用 MissingFieldDetector 自行检测
  - partialContent 是 AI 超时/部分生成时携带的部分结果（设计文档 §3.3 "非流式超时降级路径"），但 Service 层在 timeout 路径下未使用
- **建议**：
  - missingFields：若 AI 实现的检测与本地 MissingFieldDetector 重复，可在配置开启时优先使用 AI 标记（如 `ai.medical-record.trust-missing-fields=true`），否则继续本地检测
  - partialContent：在 callAiWithTimeout() 捕获 TimeoutException 时，尝试 `future.getNow(partialData)` 提取部分结果（若 future 已完成则 getNow 非阻塞返回），并通过 `AiResultFactory.failure("MR_GEN_AI_TIMEOUT", partialData)` 携带
  - 当前实现"AI 标记缺失字段"和"超时 partialData"两个语义都被忽略，是设计文档 §3.3 "使用现有 AiResult.data 字段承载部分生成结果"的契约未达成

#### [轻微] 12. CompletableFuture.supplyAsync 使用默认 ForkJoinPool 调度阻塞式 VisitFacade 调用

- **位置**：`AIMedical/backend/modules/medical-record/src/main/java/com/aimedical/modules/medicalrecord/service/impl/MedicalRecordServiceImpl.java:122-123`
- **描述**：`CompletableFuture.supplyAsync(() -> visitFacade.findVisitIdByEncounterId(encounterId))` 未指定 Executor，使用默认 ForkJoinPool.commonPool()。如果 VisitFacade.findVisitIdByEncounterId 是阻塞式 HTTP/DB 调用，commonPool 的线程被阻塞会影响其他 ForkJoin 任务。建议使用独立的 Executor（如 `@Configuration` 定义的 bounded ThreadPoolTaskExecutor），并通过 `@Value` 注入线程池大小。
- **建议**：新增 `MedicalRecordExecutorConfig` 定义专用 ThreadPoolTaskExecutor，MedicalRecordServiceImpl 通过 `@Qualifier` 注入。

#### [轻微] 13. DepartmentTemplateConfig 内部 Map/Set 使用可变集合

- **位置**：`AIMedical/backend/modules/medical-record/src/main/java/com/aimedical/modules/medicalrecord/template/DepartmentTemplateConfig.java:10-21`
- **描述**：requiredFields（Set）、promptMessages（Map）、suggestedActions（Map）在构造器中按引用赋值，未做不可变封装。理论上调用方可能在外部修改这些集合导致缓存中的 DepartmentTemplateConfig 状态污染。Caffeine 缓存的 value 如果后续被修改会影响下一次读取。
- **建议**：构造器内 `Collections.unmodifiableSet(...)` / `Collections.unmodifiableMap(...)` 包装返回，确保值对象不可变。

#### [轻微] 14. MedicalRecordController 错误码转换路径的覆盖不完整

- **位置**：`AIMedical/backend/modules/medical-record/src/main/java/com/aimedical/modules/medicalrecord/api/MedicalRecordController.java:31-36`
- **描述**：controller 仅检查 `response.isSuccess()`，对 response.errorCode 直接透传给 Result.fail。但若 Service 内部既返回 `success=false` 又设置了 `errorCode=null`（converter 行为——见发现 7），controller 会调用 `Result.fail(null)`。需确认 Result.fail(null) 在 common 模块的语义（是否抛 NPE）。测试 `shouldReturnFailWhenResponseIsNotSuccess`（MedicalRecordControllerTest.java:51-64）只覆盖了 errorCode 非空场景。
- **建议**：在 controller 端补充 `response.getErrorCode() == null` 的兜底（如 Result.fail("MR_GEN_INTERNAL_ERROR")），或在 Service 层确保 success=false 时 errorCode 必非空。

#### [轻微] 15. MedicalRecordContentConverter 静默吞掉序列化异常

- **位置**：`AIMedical/backend/modules/medical-record/src/main/java/com/aimedical/modules/medicalrecord/converter/MedicalRecordContentConverter.java:28-30, 43-45`
- **描述**：convertToDatabaseColumn 和 convertToEntityAttribute 在 catch 块中分别返回 null 与 Collections.emptyMap()，没有日志记录或异常向上抛出。当数据库存储的 JSON 因 enum 演进而包含未知字段时，调用方完全感知不到（虽然测试 `convertToEntityAttributeShouldReturnEmptyMapForUnknownEnumName` 明确这是预期行为，但生产环境可能需要可观测性）。
- **建议**：catch 块内 `log.warn("MedicalRecordContentConverter 异常, 字段={}, 错误={}", ..., e)`，保留静默降级语义但增加可观测性。

#### [轻微] 16. MissingFieldDetectorImpl 中 getFieldName() 中英映射与需求文档略有差异

- **位置**：`AIMedical/backend/modules/medical-record/src/main/java/com/aimedical/modules/medicalrecord/detector/MissingFieldDetectorImpl.java:51-62`
- **描述**：getFieldName() 中 TREATMENT_PLAN 返回 "治疗方案"，而设计文档 §3.3 表格中 TREATMENT_PLAN 对应"治疗意见"（同时 ai-api 层 MedicalRecordGenResponse 字段名为 treatmentPlan，中文含义"治疗意见"）。DEFAULT_TEMPLATE 中 TREATMENT_PLAN 的 suggestedAction 模板为 "请补充{{fieldName}}信息"，实际渲染为"请补充治疗方案信息"——与设计文档的"治疗意见"不一致。
- **建议**：将 TREATMENT_PLAN 映射改为"治疗意见"，与设计文档/需求文档 §3.4.3 输出一致。

#### [轻微] 17. MedicalRecord 实体 PrePersist 不处理 version 初始化

- **位置**：`AIMedical/backend/modules/medical-record/src/main/java/com/aimedical/modules/medicalrecord/entity/MedicalRecord.java:130-138`
- **描述**：Hibernate 对 @Version 字段的新对象会自动设置为 0（无需 PrePersist 处理），但当 DB 已有数据从非 JPA 途径导入时，version 可能为 NULL。MedicalRecord 实体未在 PrePersist 中显式 setVersion(0) 以保证新建时 version 一定非 NULL。
- **建议**：PrePersist 中 `if (this.version == null) this.version = 0;`（预防 NPE，便于审计）。

### 本轮统计

| 严重程度 | 数量 |
|---------|------|
| 严重 | 4 |
| 一般 | 7 |
| 轻微 | 6 |

### 总评

medical-record 模块整体结构清晰，单 Controller 单 Service 的极简设计符合 Phase 2/3 业务定位。MedicalRecordField 7 字段枚举、MedicalRecordContentConverter 的 JSON 双向转换、MissingFieldDetectorImpl 的"非空非 null"判定 + 模板占位符替换、DepartmentTemplateConfig 的 requiredFields + per-field promptMessage/suggestedAction 设计、MedicalRecordRepository 核心查询方法、Default 模板兜底策略均与设计文档对齐良好。测试覆盖较全面（包括 controller 短路、converter 各分支、Service 各降级路径、detector 各判定规则、template 缓存与刷新），单元测试有 Stub 屏蔽外部依赖，JPA Repository 行为通过 StubMedicalRecordRepository 模拟。

**主要问题集中在契约完整性层面**：
1. 8 个错误码只实现 4 个（MR_GEN_AI_UNAVAILABLE、MR_GEN_AI_INPUT_INVALID、MR_GEN_AI_OUTPUT_INCOMPLETE、MR_GEN_TEMPLATE_LOAD_FAILED 缺失），影响错误码→HTTP 状态码映射的完整性
2. 事件驱动缓存刷新（TemplateConfigChangeEvent）与定时调和任务（VisitIdReconciledTask）两个跨切关注点完全缺失，仅靠 60s Caffeine 兜底
3. OptimisticLock 异常处理在 generate() 主路径形同虚设（INSERT 不会触发版本冲突），MR_GEN_CONCURRENT_MODIFICATION 错误码实际不可达
4. 验证注解缺失（dialogueText 50–10000 字符约束未通过 @Size 落地）、超时配置硬编码（2s/12s 未通过 @Value 注入）、AiResultFactory 未使用（手工构造 AiResult）、doctorId 字段未填充、列名 contentJson 偏差等一般性问题
5. converter 对非超时失败场景的处理逻辑使硬失败也包装为 success=true，前端无法区分降级与硬失败

**核心抽象（MedicalRecordField 枚举、TemplateConfigManager、MissingFieldDetector、DepartmentTemplateConfig）的设计意图被正确实现**，但配套的事件、定时任务、错误码、验证机制等"周边支撑"未完全就位。建议优先修复 4 个严重问题后，再处理一般问题中的 converter 重构与超时配置外化。
