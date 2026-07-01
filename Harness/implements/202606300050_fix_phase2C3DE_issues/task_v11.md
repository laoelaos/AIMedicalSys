# 任务指令（v11）

## 动作
NEW

## 任务描述
修复 medical-record 模块 7 项问题（M04-M07, M09-M11），涉及 MedicalRecordServiceImpl, MedicalRecord, MedicalRecordConverter, RecordGenerateRequest, MedicalRecordErrorCode。

## 选择理由
R10（A09 测试修复）已完成并验证通过（consultation 140 测试通过，TriageConverterTest 12 测试全部通过）。R11 为下一优先级群组，7 项 P0/P1 修复全部在 medical-record 模块内，无跨模块依赖。

## 实施顺序
M04（UPDATE 路-径）→ M05（验证注解）→ M06（@Value 超时）→ M07（success=false）→ M09（doctorId）→ M10（@Column name）→ M11（missingFields/partialContent）— M05/M06/M10 无前后依赖可并行修改。

---

### M04: UPDATE 路径 + 乐观锁捕获

**目标**: `MedicalRecordServiceImpl.generate()` 先 `findByVisitId(visitId)` 查找已有记录，存在则更新字段（而非 always new），`save()` 捕获 `ObjectOptimisticLockingFailureException` → 设 `errorCode=MR_GEN_CONCURRENT_MODIFICATION`

**当前代码** (MedicalRecordServiceImpl.java:95-116):
```java
MedicalRecord entity = new MedicalRecord();   // always new
entity.setPatientId(request.getPatientId());
entity.setVisitId(visitId);
entity.setDepartmentId(request.getDepartmentId());
if (aiResponse != null) {
    entity.setContent(medicalRecordConverter.toFieldsMap(aiResponse));
}
entity.setVisitIdFallback(visitIdFallback);
try {
    medicalRecordRepository.save(entity);
} catch (ObjectOptimisticLockingFailureException e) {
    // catch exists but never triggered since always new
}
```

**修改要点**:
- 在 `entity.setPatientId(...)` 之前，先 `medicalRecordRepository.findByVisitId(visitId)`（`MedicalRecordRepository` 已提供此方法）
- 如果存在，复用该 entity（保持 `recordId` 和 `version`，触发 JPA UPDATE 语义）
- UPDATE 路径的 save() 因 `@Version` 版本号变更可能抛出 `ObjectOptimisticLockingFailureException`，当前 catch 块已正确处理
- `MedicalRecord` 实体已有 `@Version private Integer version`（L44-45），无需添加

**涉及文件**: `MedicalRecordServiceImpl.java`

---

### M05: RecordGenerateRequest 验证注解

**目标**: `RecordGenerateRequest.dialogueText` 增加 `@NotNull @Size(min = 50, max = 10000)`

**当前代码** (RecordGenerateRequest.java:3-9):
```java
public class RecordGenerateRequest {
    private String dialogueText;
    // ...
}
```

**修改要点**:
- 增加 `import jakarta.validation.constraints.NotNull;`
- 增加 `import jakarta.validation.constraints.Size;`
- `@NotNull @Size(min = 50, max = 10000)` 注解在 `dialogueText` 字段上

**涉及文件**: `RecordGenerateRequest.java`

---

### M06: @Value 超时注入

**目标**: 用 `@Value` 注入替代 hardcoded 超时值

**当前代码** (MedicalRecordServiceImpl.java):
```java
// L125: visitFacade 超时
String visitId = future.get(2, TimeUnit.SECONDS);   // hardcoded 2s

// L139: AI 超时
return future.get(12, TimeUnit.SECONDS);             // hardcoded 12s
```

**修改要点**:
- 在 `MedicalRecordServiceImpl` 增加字段：
  - `@Value("${ai.timeout.medical-record-generate:12}") private int aiTimeout;`
  - `@Value("${medical-record.visit-facade.timeout:2}") private int visitFacadeTimeout;`
- `resolveVisitId()` 中 `future.get(visitFacadeTimeout, TimeUnit.SECONDS)`
- `callAiWithTimeout()` 中 `future.get(aiTimeout, TimeUnit.SECONDS)`

**涉及文件**: `MedicalRecordServiceImpl.java`

---

### M07: toRecordGenerateResponse success=false

**目标**: `MedicalRecordConverter.toRecordGenerateResponse()` 在非超时失败时设 `success=false`

**当前代码** (MedicalRecordConverter.java:43-58):
```java
public RecordGenerateResponse toRecordGenerateResponse(
        AiResult<MedicalRecordGenResponse> aiResult, List<FieldMissingHint> hints) {
    RecordGenerateResponse response = new RecordGenerateResponse();
    if (aiResult.getData() != null) {
        response.setFields(toFieldsMap(aiResult.getData()));
    } else {
        response.setFields(new HashMap<>());
    }
    response.setMissingFieldHints(hints);
    response.setDegraded(aiResult.isDegraded());
    if (aiResult.getErrorCode() != null && "MR_GEN_AI_TIMEOUT".equals(aiResult.getErrorCode())) {
        response.setErrorCode(MedicalRecordErrorCode.MR_GEN_AI_TIMEOUT);
    }
    response.setSuccess(true);   // ← always true
    return response;
}
```

**修改要点**:
- `setSuccess(true)` 改为条件判断：
  - `aiResult.isSuccess()` 且 `aiResult.getData() != null` 时 → `true`
  - 其他情况（`!aiResult.isSuccess()` 或 `aiResult.getData() == null`）且 errorCode 不是 TIMEOUT 时 → `false`
- 注意 errorCode 为 `"MR_GEN_AI_TIMEOUT"` 时保持 `success=true`（超时降级仍视为业务成功，只是 degraded）

**涉及文件**: `MedicalRecordConverter.java`

**测试影响**: `MedicalRecordConverterTest.toRecordGenerateResponseShouldHandleNullData`（L89-97）当前断言 `assertTrue(response.isSuccess())`，修改后应断言 `assertFalse(response.isSuccess())`

---

### M09: generate() 写入 doctorId

**目标**: `MedicalRecordServiceImpl.generate()` 从 request 或 context 获取 doctorId 设入 MedicalRecord 实体

**当前代码** (MedicalRecordServiceImpl.java:95-102):
```java
MedicalRecord entity = new MedicalRecord();
entity.setPatientId(request.getPatientId());
entity.setVisitId(visitId);
entity.setDepartmentId(request.getDepartmentId());
// doctorId 未设置
```

**修改要点**:
- `RecordGenerateRequest` 增加 `private String doctorId` + getter/setter
- `MedicalRecordServiceImpl.generate()` 在设置 entity 字段时增加 `entity.setDoctorId(request.getDoctorId())`

**涉及文件**: `RecordGenerateRequest.java`, `MedicalRecordServiceImpl.java`

---

### M10: content @Column name

**目标**: `MedicalRecord.content` 的 `@Column` 改为 `name="content_json"`

**当前代码** (MedicalRecord.java:36-38):
```java
@Column(columnDefinition = "TEXT")
@Convert(converter = MedicalRecordContentConverter.class)
private Map<MedicalRecordField, String> content;
```

**修改要点**:
- `@Column(columnDefinition = "TEXT")` → `@Column(name = "content_json")`

**涉及文件**: `MedicalRecord.java`

---

### M11: toFieldsMap 消费 missingFields/partialContent

**目标**: `MedicalRecordConverter.toFieldsMap()` 读取 `MedicalRecordGenResponse.getMissingFields()` 和 `getPartialContent()` 写入返回的 `Map<MedicalRecordField, String>`

**当前代码** (MedicalRecordConverter.java:21-31):
```java
public Map<MedicalRecordField, String> toFieldsMap(MedicalRecordGenResponse aiResponse) {
    Map<MedicalRecordField, String> map = new HashMap<>();
    map.put(MedicalRecordField.CHIEF_COMPLAINT, aiResponse.getChiefComplaint());
    // ... 7 个字段映射
    return map;
}
```

**修改要点**:
- `MedicalRecordGenResponse` 已有 `getMissingFields()`（返回 `List<String>`）和 `getPartialContent()`（返回 `Object`）
- 将 `missingFields` list 序列化为逗号分隔字符串存入 `"missing_fields"` key（或 MedicalRecordField 枚举中新增对应字段，根据实际枚举定义）
- 将 `partialContent` 用 `String.valueOf()` 或 JSON 序列化存入 `"partial_content"` key

**涉及文件**: `MedicalRecordConverter.java`

---

## 任务上下文

**需求来源**: 实现报告 `Docs/Diagnosis/impl/06_phase2C3DE_report.md` 中病历模块 7 项问题

| 编号 | 优先级 | 描述 | 涉及文件 |
|------|--------|------|---------|
| M04 | P0 | 乐观锁+UPDATE | MedicalRecordServiceImpl, MedicalRecord |
| M05 | P0 | 输入验证 | RecordGenerateRequest |
| M06 | P0 | 超时外化 | MedicalRecordServiceImpl |
| M07 | P1 | success=false | MedicalRecordConverter |
| M09 | P1 | doctorId 赋值 | MedicalRecordServiceImpl, RecordGenerateRequest |
| M10 | P1 | content 列名 | MedicalRecord |
| M11 | P1 | missingFields 消费 | MedicalRecordConverter |

## 已有代码上下文

**MedicalRecord.java**: 实体已有 `@Version` (L44)、`content` 字段为 `Map<MedicalRecordField, String>` (L36-38)、`doctorId` 字段已存在 (L40)

**MedicalRecordServiceImpl.java**: `generate()` 方法始终创建 `new MedicalRecord()` (L95)，save() 已包裹 try-catch 捕获 `ObjectOptimisticLockingFailureException` (L105-111)，超时 hardcoded 为 2s (L125) 和 12s (L139)

**MedicalRecordConverter.java**: `toFieldsMap()` 映射 7 个标准字段 (L21-31)，`toRecordGenerateResponse()` 始终设 `success=true` (L56)

**RecordGenerateRequest.java**: 纯 POJO，dialogueText 无验证注解，无 doctorId 字段

**MedicalRecordGenResponse.java**: 已有 `missingFields: List<String>` (L13) 和 `partialContent: Object` (L14) 字段

**MedicalRecordErrorCode.java**: 已定义 `MR_GEN_CONCURRENT_MODIFICATION` (L10) 等 8 个错误码

## 测试文件清单

| 文件路径 | 涉及修改 |
|---------|---------|
| `MedicalRecordServiceImplTest.java` | M04 测试可保持现有断言（`shouldHandleOptimisticLockException` 已存在）；M09 需补充 doctorId 断言 |
| `MedicalRecordConverterTest.java` | M07 `toRecordGenerateResponseShouldHandleNullData` 断言 `assertTrue`→`assertFalse`；M11 补充 missingFields/partialContent 测试 |
