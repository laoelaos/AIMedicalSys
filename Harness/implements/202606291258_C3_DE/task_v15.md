# 任务指令（v15）

## 动作
NEW

## 任务描述
实现 medical-record 模块（包D-AI2 病历生成）全部代码，包含以下类型（按依赖顺序）：

### 枚举
- `enum MedicalRecordField` — 定义 7 个病历顶层字段标识符：CHIEF_COMPLAINT / SYMPTOM_DESCRIPTION / PRESENT_ILLNESS / PAST_HISTORY / PHYSICAL_EXAM / PRELIMINARY_DIAGNOSIS / TREATMENT_PLAN
- 位置：`com.aimedical.modules.medicalrecord.enums.MedicalRecordField`

### DTO
- `RecordGenerateRequest` — 字段：dialogueText(String)、patientId(String)、encounterId(String)、stream(boolean, default false)、departmentId(String, optional)
- `RecordGenerateResponse` — 字段：fields(Map\<MedicalRecordField, String\>)、missingFieldHints(List\<FieldMissingHint\>)、fromFallback(boolean)、degraded(boolean)
- `FieldMissingHint` — 字段：missingField(MedicalRecordField)、promptMessage(String)、suggestedAction(String)
- 位置：`com.aimedical.modules.medicalrecord.dto.*`

### 实体
- `MedicalRecord` JPA @Entity — 字段：recordId(Long, @Id @GeneratedValue)、patientId(String)、visitId(String)、departmentId(String)、contentJson(String, @Convert using Jackson AttributeConverter)、doctorId(String)、visitIdFallback(Boolean, nullable)、version(Integer, @Version)、createdAt(LocalDateTime)、updatedAt(LocalDateTime)
- `DeptTemplateConfig` JPA @Entity — 字段：id(Long, @Id @GeneratedValue)、departmentId(String, unique)、requiredFields(String, JSON TEXT)、templateFields(String, JSON TEXT)、version(Integer, @Version)、createdAt(LocalDateTime)、updatedAt(LocalDateTime)
- 位置：`com.aimedical.modules.medicalrecord.entity.*`

### Repository
- `MedicalRecordRepository` — 方法：findByVisitId(String visitId)、findByPatientId(String patientId)、findById(Long id)（均返回 Optional）
- `DeptTemplateConfigRepository` — 方法：findByDepartmentId(String departmentId)（返回 Optional）
- 位置：`com.aimedical.modules.medicalrecord.repository.*`

### Converter
- `MedicalRecordConverter` — 方法：
  - `Map<MedicalRecordField, String> toFieldsMap(MedicalRecordGenResponse aiResponse)` — 将 ai-api MedicalRecordGenResponse 的 7 个字段映射为 Map\<MedicalRecordField, String\>
  - `MedicalRecordGenRequest toAiRequest(RecordGenerateRequest request)` — 将业务层请求映射为 ai-api 层 MedicalRecordGenRequest
  - `RecordGenerateResponse toRecordGenerateResponse(AiResult<MedicalRecordGenResponse> aiResult, List<FieldMissingHint> hints)` — 组装业务层响应
- 位置：`com.aimedical.modules.medicalrecord.converter.MedicalRecordConverter`

### Template 配置
- `DepartmentTemplateConfig` (value class) — 字段：departmentId(String)、requiredFields(Set\<MedicalRecordField\>)、promptMessages(Map\<MedicalRecordField, String\>)、suggestedActions(Map\<MedicalRecordField, String\>)
- `TemplateConfigManager` (interface) — 方法：`DepartmentTemplateConfig getTemplate(String departmentId)`（departmentId 不存在时返回 DEFAULT 模板，DEFAULT 的 requiredFields=全部 7 个字段，promptMessages/suggestedActions 使用默认文案）
- `DatabaseTemplateConfigManager` (implements TemplateConfigManager) — 从 DeptTemplateConfigRepository 查询，使用 Caffeine 缓存（refreshAfterWrite 60s），科室不存在时返回 DEFAULT 模板；支持事件驱动缓存刷新
- 位置：`com.aimedical.modules.medicalrecord.template.*`

### Detector
- `MissingFieldDetector` (interface) — 方法：`List<FieldMissingHint> detect(MedicalRecordGenResponse aiResponse, DepartmentTemplateConfig template)`
  - 缺失判定：字段不存在于响应中、值为 null、值为空字符串（含全空白字符串）均视为缺失
  - 差集比对：模板必填字段 − AI 响应已填充字段 = 缺失字段
  - FieldMissingHint 生成：对每个缺失字段，从 template 中读取 promptMessage/suggestedAction，未配置时使用默认文案（"{{fieldName}}字段缺失" / "请补充{{fieldName}}信息"）
- `MissingFieldDetectorImpl` (implements MissingFieldDetector)
- 位置：`com.aimedical.modules.medicalrecord.detector.*`

### Service
- `MedicalRecordService` (interface) — 方法：`RecordGenerateResponse generate(RecordGenerateRequest request)`
- `MedicalRecordServiceImpl` (implements MedicalRecordService) — 业务流程：
  1. 调用 VisitFacade.findVisitIdByEncounterId(encounterId) 获取 visitId（降级：encounterId 非空时直接作为 visitId fallback，设置 visitIdFallback=true；encounterId 为空时返回 MR_GEN_VISIT_NOT_FOUND 错误码+部分内容）
  2. 获取 TemplateConfigManager.getTemplate(departmentId)
  3. 调用 AiService.generateMedicalRecord() 生成结构化病历
  4. 超时/降级处理：从 AiResult.data 提取已生成部分字段，保留已生成字段+缺失字段标记+MR_GEN_AI_TIMEOUT 错误码
  5. MissingFieldDetector.detect() 检测缺失字段并生成提示
  6. 持久化 MedicalRecord（contentJson 存储 fields map 的 JSON 序列化）
  7. 返回 RecordGenerateResponse
- 位置：`com.aimedical.modules.medicalrecord.service.*`

### Controller
- `MedicalRecordController` — POST /api/medical-record/generate，接收 RecordGenerateRequest 返回 RecordGenerateResponse
  - stream=true 时返回 MR_GEN_STREAM_NOT_SUPPORTED 错误码（Phase 2/3 仅支持非流式）
- 位置：`com.aimedical.modules.medicalrecord.api.MedicalRecordController`

## 选择理由
T11 是计划中最后一个未实现任务。依赖链：T3（ai-api MedicalRecordGenRequest/Response DTO 扩展）✓、T4（SessionStore/SuggestionStore 接口）✓、T5（VisitFacade/DoctorFacade/DrugFacade 门面接口+RegistrationEvent）✓、T7（模块骨架 pom.xml）✓ — 全部已 PASSED，前置条件完全满足。T10 已 BLOCKED（连续 3 轮失败，4/5 失败为 T9 缺陷），不阻断 T11。

## 任务上下文

### 依赖模块列表
- common — BaseEntity、BusinessException、GlobalErrorCode（com.aimedical.common.*）
- common-module-api — VisitFacade（com.aimedical.modules.commonmodule.visit.VisitFacade）、DoctorFacade、DrugFacade
- ai-api — AiService（com.aimedical.modules.ai.api.AiService）、AiResult（com.aimedical.modules.ai.api.AiResult）、AiResultFactory、MedicalRecordGenRequest/MedicalRecordGenResponse（com.aimedical.modules.ai.api.dto.medicalrecord.*）

### 已有模块骨架
- pom.xml 已创建（依赖 common/common-module-api/ai-api/spring-boot-starter-web/data-jpa/validation/test）
- MedicalRecordPlaceholderTest.java 已存在（占位测试）

### 参考已有实现（consultation 模块）
- 包结构、依赖注入方式、Service+Impl 模式、Controller 端点注册方式、Entity+Repository 模式均与 consultation 模块一致

### 超时配置
- ai.timeout.medical-record-generate=12s（非流式）
- medical-record.visit-facade.timeout=2s（VisitFacade 调用超时）

### stream=true 处理
stream=true 时：返回 RecordGenerateResponse + errorCode=MR_GEN_STREAM_NOT_SUPPORTED，HTTP 200（非异常）。Phase 2/3 仅非流式模式。

### VisitFacade 调用降级策略
VisitFacade 配置独立超时阈值 2s。调用超时或异常时，依次尝试：(a) encounterId 非空时直接 fallback 为 visitId，设置 visitIdFallback=true；(b) encounterId 为空时返回 MR_GEN_VISIT_NOT_FOUND 错误码 + 已生成部分病历内容。降级场景记录 WARN 级别日志。

### 错误码（需依赖 GlobalErrorCode 扩展或使用已有 error code 常量）
- MR_GEN_VISIT_NOT_FOUND — VisitFacade 降级失败
- MR_GEN_AI_TIMEOUT — AI 调用超时
- MR_GEN_STREAM_NOT_SUPPORTED — 流式模式暂不支持
- MR_GEN_CONCURRENT_MODIFICATION — 乐观锁冲突

### 测试要求
为以下关键类型编写单元测试：
- MedicalRecordConverterTest — 字段映射正确性
- RecordGenerateRequest/Response DTO 测试 — getter/setter/构造器
- MedicalRecordFieldTest — 枚举值完整性
- MedicalRecordControllerTest — 端点注册
- FieldMissingHintTest — DTO 测试
- MedicalRecordTest / DeptTemplateConfigTest — 实体字段
- MissingFieldDetectorImplTest — 缺失字段检测逻辑（所有字段缺失、部分缺失、全部填充、null vs 空字符串 vs 空白字符串边界）
- TemplateConfigManager/DatabaseTemplateConfigManagerTest — 模板查找与 DEFAULT 兜底
- MedicalRecordServiceImplTest — 正常生成流程、VisitFacade 降级、AI 超时降级、stream=true 返回错误、并发修改

## 测试文件清单（建议）
| 测试文件 | 关键验证点 |
|---------|-----------|
| MedicalRecordFieldTest | 7 个枚举值、name() 映射正确 |
| RecordGenerateRequestTest | 字段 getter/setter/service 构造 |
| RecordGenerateResponseTest | 字段 getter/setter/service 构造 |
| FieldMissingHintTest | 字段 getter/setter/service 构造 |
| MedicalRecordTest | JPA 实体字段映射、乐观锁 @Version |
| DeptTemplateConfigTest | JPA 实体字段映射 |
| MedicalRecordRepositoryTest | 基础 CRUD、findByVisitId/findByPatientId |
| DeptTemplateConfigRepositoryTest | findByDepartmentId |
| MedicalRecordConverterTest | ai-api → 业务层 DTO 字段映射完整性、null 安全 |
| MissingFieldDetectorImplTest | 差集比对：全部填充/部分缺失/全部缺失/null/空字符串/空白字符串边界 |
| TemplateConfigManagerTest | getTemplate 返回正确模板、DEFAULT 兜底 |
| DatabaseTemplateConfigManagerTest | 缓存行为、DEFAULT 兜底 |
| MedicalRecordServiceImplTest | 正常路径、VisitFacade 降级（fallback visitId）、VisitFacade 降级（error）、AI 超时降级、stream=true 错误、AI 降级 degraded=true |
| MedicalRecordControllerTest | POST 端点注册、stream=true 错误码 |
| MedicalRecordPlaceholderTest | 已有占位测试保持通过 |
