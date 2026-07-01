# 详细设计（v11）

## 概述
修复 medical-record 模块 7 项问题（M04-M07, M09-M11），涉及 MedicalRecordServiceImpl（generate() 方法重构、@Value 超时注入、doctorId 写入）、MedicalRecord（@Column name 修改）、MedicalRecordConverter（toFieldsMap 扩展 + toRecordGenerateResponse success 逻辑修正）、RecordGenerateRequest（验证注解 + doctorId 字段）、MedicalRecordField 枚举（新增两个枚举值）。

## 文件规划
| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `AIMedical/backend/modules/medical-record/src/main/java/com/aimedical/modules/medicalrecord/service/impl/MedicalRecordServiceImpl.java` | 修改 | M04: UPDATE 路径+乐观锁捕获；M06: @Value 超时注入；M09: doctorId 写入 |
| `AIMedical/backend/modules/medical-record/src/main/java/com/aimedical/modules/medicalrecord/converter/MedicalRecordConverter.java` | 修改 | M07: success 条件判断；M11: missingFields/partialContent 消费 |
| `AIMedical/backend/modules/medical-record/src/main/java/com/aimedical/modules/medicalrecord/dto/RecordGenerateRequest.java` | 修改 | M05: @NotNull/@Size 验证注解；M09: doctorId 字段+getter/setter |
| `AIMedical/backend/modules/medical-record/src/main/java/com/aimedical/modules/medicalrecord/entity/MedicalRecord.java` | 修改 | M10: @Column name = "content_json" |
| `AIMedical/backend/modules/medical-record/src/main/java/com/aimedical/modules/medicalrecord/enums/MedicalRecordField.java` | 修改 | M11: 新增 MISSING_FIELDS, PARTIAL_CONTENT 枚举值 |

## 类型定义

### M04: UPDATE 路径 + 乐观锁捕获

**实现位置**: `MedicalRecordServiceImpl.java` L95-111（generate 方法内）

**修改描述**:
- 在 `MedicalRecord entity = new MedicalRecord()` 位置改为先执行 `medicalRecordRepository.findByVisitId(visitId)` 查询已有记录
- 如果存在（`Optional.isPresent()`），复用该 entity（保留 recordId 和 version，触发 JPA UPDATE 语义）；不存在则 `new MedicalRecord()`
- UPDATE 路径下 `save()` 的 `ObjectOptimisticLockingFailureException` 捕获块（当前 L105-111）保持不变，已正确处理

**行为契约**:
- 前置条件：`visitId` 已解析成功（`resolveResult != null`）
- 正常路径：存在已有记录→UPDATE（version+1）；不存在→INSERT（version=0）
- 异常路径：并发冲突→catch 块设置 `errorCode=MR_GEN_CONCURRENT_MODIFICATION`

### M05: RecordGenerateRequest 验证注解

**实现位置**: `RecordGenerateRequest.java` L3-8

**修改描述**:
- 增加 import: `jakarta.validation.constraints.NotNull`, `jakarta.validation.constraints.Size`
- `dialogueText` 字段增加注解：`@NotNull @Size(min = 50, max = 10000)`

**入参类型变更**: `RecordGenerateRequest.dialogueText` 类型不变（`String`），增加 Jakarta Validation 注解约束

### M06: @Value 超时注入

**实现位置**: `MedicalRecordServiceImpl.java`（字段区域 + resolveVisitId + callAiWithTimeout）

**修改描述**:
- 在类级别增加 2 个 `@Value` 字段：
  - `@Value("${ai.timeout.medical-record-generate:12}") private int aiTimeout;`
  - `@Value("${medical-record.visit-facade.timeout:2}") private int visitFacadeTimeout;`
- 新增 import: `org.springframework.beans.factory.annotation.Value`
- `resolveVisitId()` 中 `future.get(2, TimeUnit.SECONDS)` → `future.get(visitFacadeTimeout, TimeUnit.SECONDS)`
- `callAiWithTimeout()` 中 `future.get(12, TimeUnit.SECONDS)` → `future.get(aiTimeout, TimeUnit.SECONDS)`

### M07: toRecordGenerateResponse success=false

**实现位置**: `MedicalRecordConverter.java` L43-58（toRecordGenerateResponse 方法）

**修改描述**:
- `response.setSuccess(true)` 改为条件表达式：
  - `aiResult.isSuccess() && aiResult.getData() != null` → `true`
  - 其他情况（`!aiResult.isSuccess()` 或 `aiResult.getData() == null`）且 errorCode 不是 `MR_GEN_AI_TIMEOUT` → `false`
  - errorCode 为 `MR_GEN_AI_TIMEOUT` 时保持 `success=true`（超时降级仍视为业务成功，只是 degraded）

**行为契约**:
- 前置条件：`aiResult` 非 null
- 返回 `success=true` 的条件：`aiResult.isSuccess() && aiResult.getData() != null`，或 `"MR_GEN_AI_TIMEOUT".equals(aiResult.getErrorCode())`
- 其他情况返回 `success=false`

### M09: generate() 写入 doctorId

**实现位置**: `RecordGenerateRequest.java` + `MedicalRecordServiceImpl.java` L95-102

**修改描述**:
- `RecordGenerateRequest` 增加字段 + getter/setter：
  - `private String doctorId;`
- `MedicalRecordServiceImpl.generate()` 在设置 entity 字段时，在 `entity.setDepartmentId(...)` 后增加 `entity.setDoctorId(request.getDoctorId())`

### M10: content @Column name

**实现位置**: `MedicalRecord.java` L36

**修改描述**:
- `@Column(columnDefinition = "TEXT")` → `@Column(name = "content_json", columnDefinition = "TEXT")`
- `@Convert(converter = MedicalRecordContentConverter.class)` 保持不变

### M11: toFieldsMap 消费 missingFields/partialContent

**实现位置**: `MedicalRecordField.java` + `MedicalRecordConverter.java` L21-31

**修改描述**:
- `MedicalRecordField` 枚举新增 2 个值：
  - `MISSING_FIELDS` — 对应 AI 返回的 missingFields 列表
  - `PARTIAL_CONTENT` — 对应 AI 返回的 partialContent 对象
- `MedicalRecordConverter.toFieldsMap()` 在现有 7 个字段映射之后增加：
  - `map.put(MedicalRecordField.MISSING_FIELDS, String.join(",", aiResponse.getMissingFields() != null ? aiResponse.getMissingFields() : Collections.emptyList()))`
  - `map.put(MedicalRecordField.PARTIAL_CONTENT, aiResponse.getPartialContent() != null ? objectMapper.writeValueAsString(aiResponse.getPartialContent()) : null)`
  - 需要 `com.fasterxml.jackson.databind.ObjectMapper` — `MedicalRecordConverter` 增加 `ObjectMapper` 字段（`@Autowired` 或构造注入），`writeValueAsString` 可能抛出 `JsonProcessingException`，转换为非受检异常或包装为 try-catch 返回 `null`

**类型关系**: `MedicalRecordField` 是 enum，现有 7 个值，新增 2 个 → 共 9 个值

## 错误处理

M04 的乐观锁错误码 `MR_GEN_CONCURRENT_MODIFICATION` 已在 `MedicalRecordErrorCode.java` L10 定义，无需新增。
其他问题不涉及新增错误码。

## 行为契约

### MedicalRecordServiceImpl.generate() 完整流程（重构后）
1. `resolveVisitId(encounterId)` — 若失败返回 null → 返回 MR_GEN_VISIT_NOT_FOUND
2. `templateConfigManager.getTemplate(departmentId)` — 获取科室模板
3. `medicalRecordConverter.toAiRequest(request)` → `callAiWithTimeout(aiRequest)` — AI 调用
4. `missingFieldDetector.detect(aiResponse, template)` — 缺失字段检测
5. `medicalRecordRepository.findByVisitId(visitId)` — 查找已有记录
   - 存在 → 复用 entity（保留 recordId, version, createdAt）
   - 不存在 → `new MedicalRecord()`
6. 设置 entity 字段：patientId, visitId, departmentId, doctorId, content, visitIdFallback
7. `medicalRecordRepository.save(entity)` — 保存（UPDATE 可能抛出 OOLFE）
8. `medicalRecordConverter.toRecordGenerateResponse(aiResult, hints)` — 组装响应

### MedicalRecordConverter.toFieldsMap()（扩展后）
- 输入：`MedicalRecordGenResponse`（含 7 个标准字段 + missingFields + partialContent）
- 输出：`Map<MedicalRecordField, String>`（9 个键值对）
- missingFields 为 null 或空列表时存入空字符串
- partialContent 为 null 时存入 null

### MedicalRecordConverter.toRecordGenerateResponse()（修正后）
- `success` 字段按业务规则条件赋值（非超时失败→false）
- 其他字段行为不变

## 依赖关系

| 被依赖类型 | 提供方 | 用途 |
|-----------|-------|------|
| `MedicalRecordRepository.findByVisitId(String)` | medical-record 模块 | M04: 查询已有记录 |
| `@Value` / `@Value("${...}")` | Spring Framework | M06: 注入外化超时配置 |
| `jakarta.validation.constraints.NotNull` / `@Size` | Jakarta Validation | M05: 输入验证注解 |
| `MedicalRecordField` enum | medical-record 模块 | M11: 新增枚举值 |
| `String.join` / `Collections.emptyList()` | JDK | M11: 序列化 missingFields 列表 |
| `ObjectMapper` (Jackson) | `com.fasterxml.jackson.databind` | M11: JSON 序列化 partialContent |

## 修订说明（v11 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| M10: @Column 错误移除了 columnDefinition="TEXT"，应保留 | `@Column(name = "content_json", columnDefinition = "TEXT")` — 保留 columnDefinition，仅补充 name 属性 |
| M11: partialContent 使用 String.valueOf() 可能丢失数据结构，建议 JSON 序列化 | 改为 `objectMapper.writeValueAsString()`，`MedicalRecordConverter` 增加 `ObjectMapper` 注入 |
