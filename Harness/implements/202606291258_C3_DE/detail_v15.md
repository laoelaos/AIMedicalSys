# 详细设计（v15）

## 概述

实现 medical-record 模块（包D-AI2 病历生成）全部代码，包含枚举、DTO、JPA 实体、Repository、Converter、Template 配置管理、MissingField 检测、Service/Impl、Controller，以及配套错误码枚举和 JPA AttributeConverter。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `AIMedical/backend/modules/medical-record/pom.xml` | 修改 | 添加 spring-boot-starter-cache、caffeine 依赖 |
| `.../medical-record/src/main/java/com/aimedical/modules/medicalrecord/enums/MedicalRecordField.java` | 新建 | 7 个病历顶层字段标识符枚举 |
| `.../medical-record/src/main/java/com/aimedical/modules/medicalrecord/exception/MedicalRecordErrorCode.java` | 新建 | 4 个模块专用错误码枚举 |
| `.../medical-record/src/main/java/com/aimedical/modules/medicalrecord/dto/RecordGenerateRequest.java` | 新建 | 病历生成请求 DTO |
| `.../medical-record/src/main/java/com/aimedical/modules/medicalrecord/dto/RecordGenerateResponse.java` | 新建 | 病历生成响应 DTO |
| `.../medical-record/src/main/java/com/aimedical/modules/medicalrecord/dto/FieldMissingHint.java` | 新建 | 缺失字段提示 DTO |
| `.../medical-record/src/main/java/com/aimedical/modules/medicalrecord/entity/MedicalRecord.java` | 新建 | 病历 JPA 实体 |
| `.../medical-record/src/main/java/com/aimedical/modules/medicalrecord/entity/DeptTemplateConfig.java` | 新建 | 科室模板配置 JPA 实体 |
| `.../medical-record/src/main/java/com/aimedical/modules/medicalrecord/converter/MedicalRecordContentConverter.java` | 新建 | JPA AttributeConverter: Map<MedicalRecordField,String> ↔ JSON |
| `.../medical-record/src/main/java/com/aimedical/modules/medicalrecord/repository/MedicalRecordRepository.java` | 新建 | 病历 Repository |
| `.../medical-record/src/main/java/com/aimedical/modules/medicalrecord/repository/DeptTemplateConfigRepository.java` | 新建 | 科室模板 Repository |
| `.../medical-record/src/main/java/com/aimedical/modules/medicalrecord/converter/MedicalRecordConverter.java` | 新建 | ai-api ↔ 业务层 DTO 转换 |
| `.../medical-record/src/main/java/com/aimedical/modules/medicalrecord/template/DepartmentTemplateConfig.java` | 新建 | 科室模板值类型 |
| `.../medical-record/src/main/java/com/aimedical/modules/medicalrecord/template/TemplateConfigManager.java` | 新建 | 模板管理器接口 |
| `.../medical-record/src/main/java/com/aimedical/modules/medicalrecord/template/DatabaseTemplateConfigManager.java` | 新建 | 基于数据库的模板管理器实现 |
| `.../medical-record/src/main/java/com/aimedical/modules/medicalrecord/detector/MissingFieldDetector.java` | 新建 | 缺失字段检测器接口 |
| `.../medical-record/src/main/java/com/aimedical/modules/medicalrecord/detector/MissingFieldDetectorImpl.java` | 新建 | 缺失字段检测器实现 |
| `.../medical-record/src/main/java/com/aimedical/modules/medicalrecord/service/MedicalRecordService.java` | 新建 | 病历服务接口 |
| `.../medical-record/src/main/java/com/aimedical/modules/medicalrecord/service/impl/MedicalRecordServiceImpl.java` | 新建 | 病历服务实现 |
| `.../medical-record/src/main/java/com/aimedical/modules/medicalrecord/api/MedicalRecordController.java` | 新建 | REST 端点 |
| `.../medical-record/src/main/java/com/aimedical/modules/medicalrecord/config/MedicalRecordConfig.java` | 新建 | Caffeine Cache 配置（用于 DatabaseTemplateConfigManager） |

## pom.xml 变更

在现有 `<dependencies>` 内追加：
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
```

## 类型定义

### MedicalRecordField（枚举）

**形态**：`enum`
**包路径**：`com.aimedical.modules.medicalrecord.enums`
**职责**：定义病历 7 个顶层字段标识符

```java
public enum MedicalRecordField {
    CHIEF_COMPLAINT,
    SYMPTOM_DESCRIPTION,
    PRESENT_ILLNESS,
    PAST_HISTORY,
    PHYSICAL_EXAM,
    PRELIMINARY_DIAGNOSIS,
    TREATMENT_PLAN
}
```

**公开接口**：标准 `name()` / `values()` / `valueOf()`
**构造方式**：默认枚举构造
**类型关系**：无

### MedicalRecordErrorCode（枚举）

**形态**：`enum implements ErrorCode`
**包路径**：`com.aimedical.modules.medicalrecord.exception`
**职责**：模块错误码定义

```java
public enum MedicalRecordErrorCode implements ErrorCode {
    MR_GEN_VISIT_NOT_FOUND("MR_GEN_VISIT_NOT_FOUND", "未找到就诊记录"),
    MR_GEN_AI_TIMEOUT("MR_GEN_AI_TIMEOUT", "AI 病历生成超时"),
    MR_GEN_STREAM_NOT_SUPPORTED("MR_GEN_STREAM_NOT_SUPPORTED", "流式模式暂不支持"),
    MR_GEN_CONCURRENT_MODIFICATION("MR_GEN_CONCURRENT_MODIFICATION", "病历数据并发修改");
}
```

**公开接口**：`getCode()` / `getMessage()`
**构造方式**：`MedicalRecordErrorCode(String code, String message)`
**类型关系**：实现 `com.aimedical.common.exception.ErrorCode`

### RecordGenerateRequest（DTO）

**形态**：`class`
**包路径**：`com.aimedical.modules.medicalrecord.dto`
**职责**：病历生成请求参数

```java
public class RecordGenerateRequest {
    private String dialogueText;
    private String patientId;
    private String encounterId;
    private boolean stream;           // default false
    private String departmentId;      // optional
}
```

**公开接口**：getter/setter（全部 5 个字段 + isStream）
**构造方式**：无参构造
**类型关系**：无

### RecordGenerateResponse（DTO）

**形态**：`class`
**包路径**：`com.aimedical.modules.medicalrecord.dto`
**职责**：病历生成响应

```java
public class RecordGenerateResponse {
    private Map<MedicalRecordField, String> fields;
    private List<FieldMissingHint> missingFieldHints;
    private boolean fromFallback;
    private boolean degraded;
    private MedicalRecordErrorCode errorCode;  // 非正常路径时填充错误码
    private boolean success;                   // true=含部分有效数据, false=硬失败(无数据)
}
```

**公开接口**：getter/setter
**构造方式**：无参构造
**类型关系**：持有 `FieldMissingHint` 列表，引用 `MedicalRecordErrorCode`

### FieldMissingHint（DTO）

**形态**：`class`
**包路径**：`com.aimedical.modules.medicalrecord.dto`
**职责**：缺失字段提示信息

```java
public class FieldMissingHint {
    private MedicalRecordField missingField;
    private String promptMessage;
    private String suggestedAction;
}
```

**公开接口**：getter/setter
**构造方式**：无参构造
**类型关系**：引用 `MedicalRecordField`

### MedicalRecord（JPA Entity）

**形态**：`@Entity`
**包路径**：`com.aimedical.modules.medicalrecord.entity`
**职责**：已生成病历持久化

```java
@Entity
@Table(name = "medical_record")
public class MedicalRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long recordId;

    @Column(nullable = false)
    private String patientId;

    @Column(nullable = false)
    private String visitId;

    private String departmentId;

    @Column(columnDefinition = "TEXT")
    @Convert(converter = MedicalRecordContentConverter.class)
    private Map<MedicalRecordField, String> content;

    private String doctorId;

    private Boolean visitIdFallback;       // nullable

    @Version
    private Integer version;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 共 11 个字段的 getter/setter
    @PrePersist 设置 createdAt = LocalDateTime.now()
    @PreUpdate 设置 updatedAt = LocalDateTime.now()
}
```

**公开接口**：全部 11 个字段的 getter/setter
**构造方式**：无参构造（JPA 规范）
**类型关系**：引用 `MedicalRecordContentConverter`

### DeptTemplateConfig（JPA Entity）

**形态**：`@Entity`
**包路径**：`com.aimedical.modules.medicalrecord.entity`
**职责**：科室级别模板配置

```java
@Entity
@Table(name = "dept_template_config")
public class DeptTemplateConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String departmentId;

    @Column(columnDefinition = "TEXT")
    private String requiredFields;        // JSON TEXT（JSON 数组）

    @Column(columnDefinition = "TEXT")
    private String templateFields;        // JSON TEXT（JSON 嵌套对象）

    @Version
    private Integer version;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // getter/setter
    @PrePersist 设置 createdAt
    @PreUpdate 设置 updatedAt
}
```

**公开接口**：全部 7 个字段的 getter/setter
**构造方式**：无参构造
**类型关系**：无

**requiredFields JSON 结构**：
```json
["CHIEF_COMPLAINT", "SYMPTOM_DESCRIPTION", "PRESENT_ILLNESS", "PAST_HISTORY", "PHYSICAL_EXAM", "PRELIMINARY_DIAGNOSIS", "TREATMENT_PLAN"]
```
顶层 JSON 数组，元素为 `MedicalRecordField` 枚举的 `name()` 值。解析时通过 `MedicalRecordField.valueOf()` 还原为枚举。

**templateFields JSON 结构**：
```json
{
  "promptMessages": {
    "CHIEF_COMPLAINT": "请描述患者的主诉症状",
    "SYMPTOM_DESCRIPTION": "请详细描述症状特征",
    ...
  },
  "suggestedActions": {
    "CHIEF_COMPLAINT": "询问患者本次就诊的主要不适",
    "SYMPTOM_DESCRIPTION": "引导患者描述症状的起始时间、部位、性质",
    ...
  }
}
```
顶层 JSON 对象包含两个固定 key（`promptMessages`、`suggestedActions`），各自的值是 `{"枚举名": "文案"}` 结构的 Map。解析时：
- 外层：确认 JSON 对象含 `promptMessages` 和 `suggestedActions` 两个字段
- 内层：遍历每个字段，通过 `MedicalRecordField.valueOf(key)` 还原枚举 key，value 即为该字段的提示文案
- 允许部分字段缺失：某个字段未出现在 `promptMessages` 中时，MissingFieldDetectorImpl 使用默认文案

### MedicalRecordRepository

**形态**：`interface extends JpaRepository`
**包路径**：`com.aimedical.modules.medicalrecord.repository`
**职责**：病历实体的数据库访问

```java
@Repository
public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {
    Optional<MedicalRecord> findByVisitId(String visitId);
    Optional<MedicalRecord> findByPatientId(String patientId);
}
```

**公开接口**：`findByVisitId(String visitId)` → `Optional<MedicalRecord>`；`findByPatientId(String patientId)` → `Optional<MedicalRecord>`；继承 `JpaRepository<MedicalRecord, Long>.findById(Long id)` → `Optional<MedicalRecord>`
**类型关系**：继承 `JpaRepository<MedicalRecord, Long>`

### DeptTemplateConfigRepository

**形态**：`interface extends JpaRepository`
**包路径**：`com.aimedical.modules.medicalrecord.repository`
**职责**：科室模板配置实体的数据库访问

```java
@Repository
public interface DeptTemplateConfigRepository extends JpaRepository<DeptTemplateConfig, Long> {
    Optional<DeptTemplateConfig> findByDepartmentId(String departmentId);
}
```

**公开接口**：`findByDepartmentId(String departmentId)` → `Optional<DeptTemplateConfig>`
**类型关系**：继承 `JpaRepository<DeptTemplateConfig, Long>`

### MedicalRecordContentConverter（JPA AttributeConverter）

**形态**：`class implements AttributeConverter<Map<MedicalRecordField, String>, String>`
**包路径**：`com.aimedical.modules.medicalrecord.converter`
**职责**：Map<MedicalRecordField,String> ↔ JSON 字符串互转

```java
@Converter
public class MedicalRecordContentConverter implements AttributeConverter<Map<MedicalRecordField, String>, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Map<MedicalRecordField, String> attribute) {
        // null → null
        // Map 序列化为 JSON（使用 MedicalRecordField.name() 作为 key）
    }

    @Override
    public Map<MedicalRecordField, String> convertToEntityAttribute(String dbData) {
        // null/空 → emptyMap
        // JSON 反序列化为 Map<MedicalRecordField,String>（key 通过 MedicalRecordField.valueOf() 还原）
    }
}
```

**公开接口**：`convertToDatabaseColumn` / `convertToEntityAttribute`
**构造方式**：默认无参
**类型关系**：实现 `jakarta.persistence.AttributeConverter`

### MedicalRecordConverter（业务转换器）

**形态**：`@Component class`
**包路径**：`com.aimedical.modules.medicalrecord.converter`
**职责**：ai-api DTO 与业务层 DTO 之间的字段映射

```java
@Component
public class MedicalRecordConverter {

    public Map<MedicalRecordField, String> toFieldsMap(MedicalRecordGenResponse aiResponse)
    // 将 aiResponse 的 7 个 String 字段映射为 Map<MedicalRecordField, String>
    // 映射规则：CHIEF_COMPLAINT ← getChiefComplaint() 等
    // 所有 7 个字段均放入 map（值可能为 null），由 MissingFieldDetectorImpl 做空值判定

    public MedicalRecordGenRequest toAiRequest(RecordGenerateRequest request)
    // 将业务层请求字段复制到 ai-api 层 MedicalRecordGenRequest

    public RecordGenerateResponse toRecordGenerateResponse(
            AiResult<MedicalRecordGenResponse> aiResult, List<FieldMissingHint> hints)
    // 组装业务层响应：从 aiResult.getData() 提取 fields；设置 hints；aiResult.isDegraded() 映射到 degraded；
    // aiResult 非 success 时从 errorCode 或 fallbackReason 获取错误码
    // success = true（此方法仅在 AI 已调用且存在有效数据时被调用）
}
```

**公开接口**：上述 3 个方法
**构造方式**：默认无参（@Component）
**类型关系**：依赖 `MedicalRecordGenResponse`（ai-api）、`MedicalRecordGenRequest`（ai-api）、`AiResult`（ai-api）、`RecordGenerateRequest`/`RecordGenerateResponse`/`FieldMissingHint`（本模块）

### DepartmentTemplateConfig（值类型）

**形态**：`class`
**包路径**：`com.aimedical.modules.medicalrecord.template`
**职责**：科室模板配置的内存表示

```java
public class DepartmentTemplateConfig {
    private String departmentId;
    private Set<MedicalRecordField> requiredFields;
    private Map<MedicalRecordField, String> promptMessages;
    private Map<MedicalRecordField, String> suggestedActions;
}
```

**公开接口**：getter/setter（或构造器全部参数）
**构造方式**：全参构造
**类型关系**：引用 `MedicalRecordField`

### TemplateConfigManager（接口）

**形态**：`interface`
**包路径**：`com.aimedical.modules.medicalrecord.template`
**职责**：科室模板查询接口

```java
public interface TemplateConfigManager {
    DepartmentTemplateConfig getTemplate(String departmentId);
}
```

**公开接口**：`getTemplate(String departmentId)` 返回 `DepartmentTemplateConfig`
**行为契约**：departmentId 不存在时返回 DEFAULT 模板

### DatabaseTemplateConfigManager（实现）

**形态**：`@Component class implements TemplateConfigManager`
**包路径**：`com.aimedical.modules.medicalrecord.template`
**职责**：基于数据库的模板配置管理，带 Caffeine 缓存

```java
@Component
public class DatabaseTemplateConfigManager implements TemplateConfigManager {

    private final DeptTemplateConfigRepository repository;
    private final LoadingCache<String, DepartmentTemplateConfig> templateCache;

    // 构造器注入
    // 缓存：Caffeine.newBuilder().refreshAfterWrite(60, TimeUnit.SECONDS).build(cacheLoader)
    // cacheLoader.load(key) = loadFromDatabase(key)

    @Override
    public DepartmentTemplateConfig getTemplate(String departmentId)
    // 从 cache 获取；cache miss → 查 DB → 存在则构造 DepartmentTemplateConfig，
    // 不存在则返回 DEFAULT 模板

    // 事件驱动刷新方法（可通过 ApplicationEventPublisher 触发）
    public void refreshTemplate(String departmentId)
    // cache.invalidate(departmentId)
}
```

**私有方法 `loadFromDatabase(String departmentId)` 解析逻辑**：

```java
private DepartmentTemplateConfig loadFromDatabase(String departmentId) {
    // 1. repository.findByDepartmentId(departmentId) 查询实体
    // 2. 实体不存在 → 返回 DEFAULT 模板（见下文 DEFAULT 说明）
    // 3. 解析 requiredFields：
    //      JSONArray → stream().map(elem -> MedicalRecordField.valueOf(elem.getAsString())).collect(toSet())
    //      解析异常 → 返回 DEFAULT 模板（降级安全）
    // 4. 解析 templateFields：
    //      JSONObject → 提取 "promptMessages" / "suggestedActions" 两个子 JSONObject
    //      每个子对象：entrySet().stream().collect(toMap(
    //          e -> MedicalRecordField.valueOf(e.getKey()),
    //          e -> e.getValue().getAsString()
    //      ))
    //      子对象缺失 → 该子 Map 为空；某个字段缺失 → 该字段无对应文案
    //      解析异常 → 返回 DEFAULT 模板（降级安全）
    // 5. 构造 DepartmentTemplateConfig(departmentId, requiredFields, promptMessages, suggestedActions)
    // 6. 返回
}
```

key 解析要点：
- `requiredFields` JSON 数组解析到 `Set<MedicalRecordField>`，用于差集比对
- `templateFields.promptMessages` JSON 对象解析到 `Map<MedicalRecordField, String>`
- `templateFields.suggestedActions` JSON 对象解析到 `Map<MedicalRecordField, String>`
- 两种 JSON 列均独立解析，任一解析异常（JSON 格式错误、枚举名不匹配）均降级为返回 DEFAULT 模板，不抛出异常

**公开接口**：`getTemplate`、`refreshTemplate`
**构造方式**：构造器注入 `DeptTemplateConfigRepository`
**类型关系**：实现 `TemplateConfigManager`；依赖 `DeptTemplateConfigRepository`、`DeptTemplateConfig`

**DEFAULT 模板说明**：
- departmentId = "DEFAULT"
- requiredFields = 全部 7 个 MedicalRecordField
- promptMessages：对所有字段使用 `"{{fieldName}}字段缺失"`（占位符 `{{fieldName}}` 在 MissingFieldDetectorImpl 中被解析为中文名）
- suggestedActions：对所有字段使用 `"请补充{{fieldName}}信息"`（占位符在 MissingFieldDetectorImpl 中解析）

### MissingFieldDetector（接口）

**形态**：`interface`
**包路径**：`com.aimedical.modules.medicalrecord.detector`
**职责**：检测 AI 响应中缺失的病历字段

```java
public interface MissingFieldDetector {
    List<FieldMissingHint> detect(MedicalRecordGenResponse aiResponse, DepartmentTemplateConfig template);
}
```

**公开接口**：`detect`
**行为契约**：
- 差集算法：template.requiredFields − AI 响应中已填充字段 = 缺失字段
- 已填充判定：字段存在于响应中、值非 null、值非空字符串（含全空白字符串视为缺失）
- 对每个缺失字段，从 template.promptMessages/suggestedActions 中读取文案，替换 `{{fieldName}}` 占位符为中文名，未配置时使用默认文案

### MissingFieldDetectorImpl（实现）

**形态**：`@Component class implements MissingFieldDetector`
**包路径**：`com.aimedical.modules.medicalrecord.detector`
**职责**：缺省字段检测逻辑实现

```java
@Component
public class MissingFieldDetectorImpl implements MissingFieldDetector {

    private final MedicalRecordConverter converter;

    // 构造器注入 MedicalRecordConverter

    @Override
    public List<FieldMissingHint> detect(MedicalRecordGenResponse aiResponse, DepartmentTemplateConfig template) {
        // 1. 调用 converter.toFieldsMap(aiResponse) 获取 Map<MedicalRecordField, String>
        // 2. 遍历 template.requiredFields，对每个字段：
        //    a. 从 Map 中取值
        //    b. 值 == null || value.trim().isEmpty() → 视为缺失
        //    c. 缺失字段 → fill hint，包含占位符解析后的 promptMessage / suggestedAction
        // 3. 返回 List<FieldMissingHint>
    }

    private FieldMissingHint buildHint(MedicalRecordField field, DepartmentTemplateConfig template)
    // 从 template 读取 promptMessage / suggestedAction
    // 替换其中的 {{fieldName}} 占位符为 getFieldName(field) 的返回值
    // 若 template 中未配置该字段的文案，使用默认文案："{{fieldName}}字段缺失" / "请补充{{fieldName}}信息"

    private String resolvePlaceholders(String text, MedicalRecordField field)
    // 将 text 中的 "{{fieldName}}" 替换为 getFieldName(field) 的返回值

    private String getFieldName(MedicalRecordField field)
    // "CHIEF_COMPLAINT" → "主诉" 等中文映射
}
```

**公开接口**：`detect`
**构造方式**：构造器注入 `MedicalRecordConverter`
**类型关系**：实现 `MissingFieldDetector`；依赖 `MedicalRecordConverter`（本模块）、`MedicalRecordGenResponse`（ai-api）

### MedicalRecordService（接口）

**形态**：`interface`
**包路径**：`com.aimedical.modules.medicalrecord.service`
**职责**：病历生成业务接口

```java
public interface MedicalRecordService {
    RecordGenerateResponse generate(RecordGenerateRequest request);
}
```

**公开接口**：`generate(RecordGenerateRequest) → RecordGenerateResponse`

### MedicalRecordServiceImpl（实现）

**形态**：`@Service class implements MedicalRecordService`
**包路径**：`com.aimedical.modules.medicalrecord.service.impl`
**职责**：病历生成完整业务流程编排

```java
@Service
public class MedicalRecordServiceImpl implements MedicalRecordService {

    private final VisitFacade visitFacade;
    private final TemplateConfigManager templateConfigManager;
    private final AiService aiService;
    private final MissingFieldDetector missingFieldDetector;
    private final MedicalRecordConverter medicalRecordConverter;
    private final MedicalRecordRepository medicalRecordRepository;

    // 构造器注入（不含 MedicalRecordContentConverter，JPA @Convert 自动处理）

    @Override
    public RecordGenerateResponse generate(RecordGenerateRequest request) {
        // 1. VisitFacade.findVisitIdByEncounterId() with 2s 超时降级
        // 2. TemplateConfigManager.getTemplate(departmentId)
        // 3. AiService.generateMedicalRecord() with 12s 超时
        // 4. MissingFieldDetector.detect()
        // 5. 持久化 MedicalRecord
        // 6. 组装 RecordGenerateResponse（success=true；visit 未找到时 success=false）
    }

    private String resolveVisitId(String encounterId)
    // 调用 VisitFacade；超时/异常时走降级逻辑

    private AiResult<MedicalRecordGenResponse> callAiWithTimeout(MedicalRecordGenRequest aiRequest)
    // 调用 aiService.generateMedicalRecord()，12s 超时处理

    private MedicalRecord saveMedicalRecord(...)
    // 持久化并处理 OptimisticLockException → MR_GEN_CONCURRENT_MODIFICATION
}
```

**公开接口**：`generate`
**构造方式**：构造器注入（VisitFacade, TemplateConfigManager, AiService, MissingFieldDetector, MedicalRecordConverter, MedicalRecordRepository）
**类型关系**：实现 `MedicalRecordService`；依赖 `VisitFacade`（common-module-api）、`AiService`（ai-api）、`TemplateConfigManager`（本模块）、`MissingFieldDetector`（本模块）、`MedicalRecordConverter`（本模块）、`MedicalRecordRepository`（本模块）

**业务流程详细说明**（generate 方法，stream=true 已在 Controller 层处理）：

1. **VisitFacade 调用**：`visitFacade.findVisitIdByEncounterId(request.getEncounterId())`。配置独立超时 2s。catch 超时/异常时：
   - (a) `encounterId` 非空 → fallback visitId = encounterId，`visitIdFallback = true`，WARN 日志，继续后续流程
   - (b) `encounterId` 为空 → 返回 `RecordGenerateResponse(success=false, errorCode=MR_GEN_VISIT_NOT_FOUND)`，fields=空map，不调用 AI

2. **获取模板**：`templateConfigManager.getTemplate(request.getDepartmentId())`

3. **AI 调用**：`converter.toAiRequest(request)` → `aiService.generateMedicalRecord()` → `future.get(12, TimeUnit.SECONDS)`。超时或异常时：
   - 从 AiResult.data 提取已生成部分字段（如果存在）
   - errorCode=`MR_GEN_AI_TIMEOUT`，degraded=true
   - response.success 保持 true（含部分有效数据）

4. **缺失字段检测**：`missingFieldDetector.detect(aiResponse, template)` → hints

5. **持久化**：创建 `MedicalRecord` 实体，content 字段（含 @Convert 注解）自动将 Map 序列化为 JSON 存入数据库。捕获 `OptimisticLockException` → 设置 errorCode=`MR_GEN_CONCURRENT_MODIFICATION`，success=true（fields 已有数据）

6. **返回**：`converter.toRecordGenerateResponse(aiResult, hints)`，success=true

### MedicalRecordController

**形态**：`@RestController`
**包路径**：`com.aimedical.modules.medicalrecord.api`
**职责**：REST 端点注册

```java
@RestController
@RequestMapping("/api/medical-record")
public class MedicalRecordController {

    private final MedicalRecordService medicalRecordService;

    public MedicalRecordController(MedicalRecordService medicalRecordService) { ... }

    @PostMapping("/generate")
    public Result<RecordGenerateResponse> generate(@Valid @RequestBody RecordGenerateRequest request) {
        // stream=true → 直接返回 Result.fail，不调用 Service
        if (request.isStream()) {
            return Result.fail(MedicalRecordErrorCode.MR_GEN_STREAM_NOT_SUPPORTED);
        }
        RecordGenerateResponse response = medicalRecordService.generate(request);
        if (response.isSuccess()) {
            return Result.success(response);
        }
        // 硬失败（visit 未找到等无有效数据的场景）→ Result.fail
        return Result.fail(response.getErrorCode());
    }
}
```

**公开接口**：`generate(RecordGenerateRequest) → Result<RecordGenerateResponse>`
**构造方式**：构造器注入 `MedicalRecordService`
**类型关系**：返回 `com.aimedical.common.result.Result`，依赖 `MedicalRecordErrorCode`

### MedicalRecordConfig（缓存配置）

**形态**：`@Configuration class`
**包路径**：`com.aimedical.modules.medicalrecord.config`
**职责**：Caffeine Cache Bean 定义（可选，若 DatabaseTemplateConfigManager 直接创建 LoadingCache 则不需要）

```java
@Configuration
public class MedicalRecordConfig {
    // 如 DatabaseTemplateConfigManager 直接使用 Caffeine.newBuilder() 构建则无需此配置类
    // 选择方案：DatabaseTemplateConfigManager 内直接创建 LoadingCache（与 DefaultTriageRuleEngine 一致）
}
```

**决策**：参照 `DefaultTriageRuleEngine` 模式，`DatabaseTemplateConfigManager` 直接在构造器中创建 `LoadingCache`，不额外创建独立配置类。

## 错误处理

| 场景 | 错误码 | success | Controller 返回策略 | 处理方式 |
|------|--------|---------|-------------------|---------|
| stream=true | MR_GEN_STREAM_NOT_SUPPORTED | — | Result.fail(ErrorCode) | Controller 层直接校验返回，不调用 Service |
| VisitFacade encounterId 为空 | MR_GEN_VISIT_NOT_FOUND | false | Result.fail(ErrorCode) | Service 返回 success=false，Controller 转为 Result.fail |
| AI 调用超时(含部分字段) | MR_GEN_AI_TIMEOUT | true | Result.success(response) | response 内含 errorCode + degraded=true，客户端自检 |
| 乐观锁冲突 | MR_GEN_CONCURRENT_MODIFICATION | true | Result.success(response) | fields 已填充，response 内含 errorCode，客户端自检 |
| MissingFieldDetector | 无异常 | true | Result.success(response) | 正常返回 hints（空列表表示无缺失） |

`MedicalRecordErrorCode` 枚举实现 `ErrorCode` 接口，与 `GlobalErrorCode` 模式一致。

**客户端处理指引**：客户端收到 `Result.success(response)` 后，检查 `response.errorCode` 和 `response.degraded` 判断是否降级或含错误信息；收到 `Result.fail` 后直接读取 Result 的 code/message。

## 行为契约

1. **generate 请求-响应生命周期**：controller 校验 stream=true → controller 调用 service.generate() → 返回 RecordGenerateResponse；controller 根据 response.success 决定 Result.fail 或 Result.success
2. **stream=true**：Controller 层直接返回 `Result.fail(MR_GEN_STREAM_NOT_SUPPORTED)`，不调用 Service
3. **success=false 场景**：Service 返回无有效数据（visit 未找到），Controller 转为 `Result.fail(errorCode)`，Result 的 code 和 message 承载错误信息
4. **success=true 场景**（含 degraded 情况）：Controller 返回 `Result.success(response)`，response 内含 errorCode/degraded/fromFallback，客户端自检
5. fields 顺序不限，由 ai-api MedicalRecordGenResponse 字段映射决定
6. fromFallback=true 表示 visitId 来自降级（encounterId fallback）
7. degraded=true 表示 AI 调用经历了降级（超时等）
8. VisitFacade 降级失败（encounterId 为空）时不调用 AI，直接返回 success=false 响应
9. OptimisticLockException 由 JPA @Version 自动检测，catch 后设置 errorCode，success=still true
10. **MissingFieldDetectorImpl 占位符解析**：template 中 promptMessages/suggestedActions 可包含 `{{fieldName}}` 占位符，detector 在构建 FieldMissingHint 时调用 `getFieldName()` 将其替换为中文名（如 `"主诉"`），确保客户端收到已解析的完整中文文案

## 依赖关系

| 类型 | 依赖 |
|------|------|
| MedicalRecordService/Impl | VisitFacade (common-module-api), AiService (ai-api), TemplateConfigManager, MissingFieldDetector, MedicalRecordConverter, MedicalRecordRepository |
| MissingFieldDetectorImpl | MedicalRecordConverter (本模块), MedicalRecordGenResponse (ai-api), DepartmentTemplateConfig |
| MedicalRecordConverter | MedicalRecordGenResponse (ai-api), MedicalRecordGenRequest (ai-api), AiResult (ai-api) |
| MedicalRecordController | MedicalRecordService, Result (common) |
| DatabaseTemplateConfigManager | DeptTemplateConfigRepository, Caffeine |
| MedicalRecord | MedicalRecordContentConverter (JPA Converter, 自动调用) |
| DeptTemplateConfig | 无外部依赖 |
| MedicalRecordRepository | MedicalRecord (entity), JpaRepository |
| DeptTemplateConfigRepository | DeptTemplateConfig (entity), JpaRepository |
| MedicalRecordField | 无外部依赖 |
| MedicalRecordErrorCode | ErrorCode (common) |
| MedicalRecordContentConverter | ObjectMapper (Jackson), MedicalRecordField |
| 全部 | common, common-module-api, ai-api, spring-boot-starter-web, spring-boot-starter-data-jpa, spring-boot-starter-validation, spring-boot-starter-cache, caffeine |

## 修订说明（v15 R1）
| 审查意见 | 修改措施 |
|---------|---------|
| MedicalRecord.contentJson 字段 String 类型与 AttributeConverter&lt;Map&lt;MedicalRecordField,String&gt;,String&gt; 的 X 类型不匹配 | 将 `contentJson` 字段类型从 `String` 改为 `Map&lt;MedicalRecordField, String&gt;`，同时改名为 `content`，保持 `@Convert` 注解使用 |
| DeptTemplateConfig 缺少 `@Version` 乐观锁字段 | 在 `DeptTemplateConfig` 实体中添加 `@Version private Integer version;` 字段 |
| Repository 接口缺少方法签名定义 | 为 `MedicalRecordRepository` 补充 `findByVisitId`、`findByPatientId` 方法签名（`findById` 继承自 JpaRepository）；为 `DeptTemplateConfigRepository` 补充 `findByDepartmentId` 方法签名 |

## 修订说明（v15 R2）
| 审查意见 | 修改措施 |
|---------|---------|
| 区分 Controller 返回策略：stream=true/visit未找到→Result.fail，AI超时等含部分有效数据→Result.success(response) + 客户端通过errorCode/degraded判断 | (1) RecordGenerateResponse 新增 `success` boolean 字段，`errorCode` 类型从 String 改为 MedicalRecordErrorCode 枚举；(2) Controller 层前置校验 stream=true→Result.fail(MR_GEN_STREAM_NOT_SUPPORTED)，根据 response.isSuccess() 决定 return Result.success(response) 或 Result.fail(errorCode)；(3) Service 移除 stream=true 步骤，visit未找到时返回 success=false，AI超时等含部分数据时返回 success=true；(4) 更新错误处理表增加 success/返回策略列，更新行为契约 |

## 修订说明（v15 R3）
| 审查意见 | 修改措施 |
|---------|---------|
| DEFAULT 模板的 `{{fieldName}}` 占位符无解析机制，将导致客户端收到字面未替换的提示文案 | MissingFieldDetectorImpl 中新增强占位符解析方法 `resolvePlaceholders()`：在构建 FieldMissingHint 时将 template 中 promptMessages/suggestedActions 的 `{{fieldName}}` 替换为 `getFieldName(field)` 的中文输出。DEFAULT 模板仍存储 `{{fieldName}}字段缺失` / `请补充{{fieldName}}信息`，detector 在 detect() 执行时实时解析。getFieldName() 中文映射方法被实际调用，客户端收到完整的已解析中文文案 |
| 反射调用 getter 检测字段填充状态，字段变更时静默失效 | 移除 MissingFieldDetectorImpl 中基于反射的 isFieldFilled() 方法。MissingFieldDetectorImpl 改为构造器注入 MedicalRecordConverter，detect() 先调用 converter.toFieldsMap(aiResponse) 获得 Map&lt;MedicalRecordField, String&gt;，然后遍历 template.requiredFields 对该 Map 做 null/empty 判定，编译期对字段名变更完全安全 |
| MedicalRecordServiceImpl 过度依赖 MedicalRecordContentConverter | 从 MedicalRecordServiceImpl 的构造器依赖列表中移除 MedicalRecordContentConverter。JPA @Convert 注解在实体层面自动触发转换，Service 层无需显式持有该 Converter |

## 修订说明（v15 R4）
| 审查意见 | 修改措施 |
|---------|---------|
| 明确定义 DeptTemplateConfig.templateFields 的 JSON 结构（含 promptMessages 和 suggestedActions 的嵌套对象） | 在 DeptTemplateConfig 实体节下方新增 `templateFields JSON 结构` 详细说明，完整描述 JSON 嵌套对象的层级结构：外层 `{"promptMessages": {...}, "suggestedActions": {...}}`，内层各为 `{"枚举名": "文案"}` 格式的 Map。同时补充 `requiredFields` JSON 数组结构说明 |
| 说明 DatabaseTemplateConfigManager.loadFromDatabase() 的解析逻辑 | 在 DatabaseTemplateConfigManager 节新增 `loadFromDatabase(String departmentId)` 私有方法完整伪代码及解析要点，涵盖：(a) requiredFields JSON 数组 → Set 的解析流程；(b) templateFields JSON 对象 → 两个独立 Map 的解析流程；(c) 任一部分解析异常时降级返回 DEFAULT 模板的安全策略 |
