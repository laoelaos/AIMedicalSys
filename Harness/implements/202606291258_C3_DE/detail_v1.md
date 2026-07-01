# 详细设计（v1）

## 概述

在 ai-api 模块中扩展分诊（triage）相关 DTO 的字段定义，新增缺失的 DTO 类（AdditionalResponseItem、RecommendedDoctor、MatchedRuleItem），新增 AiResultFactory 静态工厂类，为后续四个业务模块提供完整字段定义的 DTO 支持及 AiResult 构造工厂方法。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `src/main/java/com/aimedical/modules/ai/api/AiResultFactory.java` | 新建 | 提供 AiResult 静态工厂方法重载（含 partialData 参数） |
| `src/main/java/com/aimedical/modules/ai/api/dto/triage/TriageRequest.java` | 修改 | 扩展 additionalResponses、patientId、sessionId、ruleVersion、ruleSetId 字段 |
| `src/main/java/com/aimedical/modules/ai/api/dto/triage/TriageResponse.java` | 修改 | 扩展 recommendedDoctors、matchedRules、needFollowUp、followUpQuestion、confidence、degraded、sessionId、correctedChiefComplaint 字段 |
| `src/main/java/com/aimedical/modules/ai/api/dto/triage/RecommendedDepartment.java` | 修改 | 扩展 departmentId、score 字段 |
| `src/main/java/com/aimedical/modules/ai/api/dto/triage/AdditionalResponseItem.java` | 新建 | 表示附加问答项的 DTO |
| `src/main/java/com/aimedical/modules/ai/api/dto/triage/RecommendedDoctor.java` | 新建 | 表示推荐医生的 DTO |
| `src/main/java/com/aimedical/modules/ai/api/dto/triage/MatchedRuleItem.java` | 新建 | 表示匹配规则项的 DTO |

## 类型定义

### AiResultFactory

**形态**：class（final，工具类风格，private 构造器禁止实例化）
**包路径**：com.aimedical.modules.ai.api
**职责**：提供 AiResult 的静态工厂方法，覆盖含/不含 partialData 的场景

```java
public final class AiResultFactory {
    private AiResultFactory() {}
}
```

**公开接口**：

| 方法签名 | 说明 |
|---------|------|
| `public static <T> AiResult<T> failure(String errorCode, T partialData)` | partialData 写入 data 字段，success=false, degraded=false |
| `public static <T> AiResult<T> degraded(String fallbackReason, T partialData)` | partialData 写入 data 字段，success=false, degraded=true |
| `public static <T> AiResult<T> failure(String errorCode)` | 无 partialData 简化工厂，等价于 `new AiResult<>(false, null, errorCode, false, null)` |
| `public static <T> AiResult<T> success(T data)` | 成功工厂，等价于 `new AiResult<>(true, data, null, false, null)` |

**构造方式**：私有构造器，不可实例化
**类型关系**：无继承/实现

### AdditionalResponseItem

**形态**：class
**包路径**：com.aimedical.modules.ai.api.dto.triage
**职责**：表示分诊流程中附加问答的每一项

```java
public class AdditionalResponseItem {
    private String question;
    private String answer;
    private String answeredAt;
}
```

**公开接口**：
- 默认构造器 `public AdditionalResponseItem()`
- Getter: `getQuestion()` / `getAnswer()` / `getAnsweredAt()` 均返回 `String`
- Setter: `setQuestion(String)` / `setAnswer(String)` / `setAnsweredAt(String)`

**构造方式**：默认构造器
**类型关系**：无

### RecommendedDoctor

**形态**：class
**包路径**：com.aimedical.modules.ai.api.dto.triage
**职责**：表示推荐给患者的医生信息

```java
public class RecommendedDoctor {
    private String doctorId;
    private String doctorName;
    private String departmentId;
    private int availableSlotCount;
    private float score;
}
```

**公开接口**：
- 默认构造器 `public RecommendedDoctor()`
- Getter: `getDoctorId()` / `getDoctorName()` / `getDepartmentId()` 返回 `String`；`getAvailableSlotCount()` 返回 `int`；`getScore()` 返回 `float`
- Setter: `setDoctorId(String)` / `setDoctorName(String)` / `setDepartmentId(String)` / `setAvailableSlotCount(int)` / `setScore(float)`

**构造方式**：默认构造器
**类型关系**：无

### MatchedRuleItem

**形态**：class
**包路径**：com.aimedical.modules.ai.api.dto.triage
**职责**：表示分诊评估中匹配的规则项

```java
public class MatchedRuleItem {
    private String ruleId;
    private String ruleName;
    private float score;
}
```

**公开接口**：
- 默认构造器 `public MatchedRuleItem()`
- Getter: `getRuleId()` / `getRuleName()` 返回 `String`；`getScore()` 返回 `float`
- Setter: `setRuleId(String)` / `setRuleName(String)` / `setScore(float)`

**构造方式**：默认构造器
**类型关系**：无

### TriageRequest（修改）

**形态**：class
**包路径**：com.aimedical.modules.ai.api.dto.triage
**职责**：分诊请求 DTO

```java
public class TriageRequest {
    private String chiefComplaint;
    private List<AdditionalResponseItem> additionalResponses;
    private String patientId;
    private String sessionId;
    private String ruleVersion;
    private String ruleSetId;
}
```

**新增字段**（保留已有字段不变）：

| 字段 | 类型 | 说明 |
|------|------|------|
| additionalResponses | `List<AdditionalResponseItem>` | 可选，附加问答列表 |
| patientId | `String` | 可选，患者 ID |
| sessionId | `String` | 可选，会话 ID |
| ruleVersion | `String` | 可选，规则版本号 |
| ruleSetId | `String` | 可选，规则集 ID |

**新增 getter/setter**：
- `getAdditionalResponses()` / `setAdditionalResponses(List<AdditionalResponseItem>)`
- `getPatientId()` / `setPatientId(String)`
- `getSessionId()` / `setSessionId(String)`
- `getRuleVersion()` / `setRuleVersion(String)`
- `getRuleSetId()` / `setRuleSetId(String)`

**新增 import**：`java.util.List`

### TriageResponse（修改）

**形态**：class
**包路径**：com.aimedical.modules.ai.api.dto.triage
**职责**：分诊响应 DTO

```java
public class TriageResponse {
    private List<RecommendedDepartment> recommendedDepartments;
    private String reason;
    private List<RecommendedDoctor> recommendedDoctors;
    private List<MatchedRuleItem> matchedRules;
    private boolean needFollowUp;
    private String followUpQuestion;
    private Float confidence;
    private boolean degraded;
    private String sessionId;
    private String correctedChiefComplaint;
}
```

**新增字段**（保留已有字段不变）：

| 字段 | 类型 | 说明 |
|------|------|------|
| recommendedDoctors | `List<RecommendedDoctor>` | 可选，推荐医生列表 |
| matchedRules | `List<MatchedRuleItem>` | 可选，匹配规则列表 |
| needFollowUp | `boolean` | 是否需要随访 |
| followUpQuestion | `String` | 可选，随访问题 |
| confidence | `Float` | 可选，置信度（boxed 以支持 null） |
| degraded | `boolean` | 是否降级 |
| sessionId | `String` | 可选，会话 ID |
| correctedChiefComplaint | `String` | 可选，修正后的主诉 |

**新增 getter/setter**：
- `getRecommendedDoctors()` / `setRecommendedDoctors(List<RecommendedDoctor>)`
- `getMatchedRules()` / `setMatchedRules(List<MatchedRuleItem>)`
- `isNeedFollowUp()` / `setNeedFollowUp(boolean)`
- `getFollowUpQuestion()` / `setFollowUpQuestion(String)`
- `getConfidence()` / `setConfidence(Float)`
- `isDegraded()` / `setDegraded(boolean)`
- `getSessionId()` / `setSessionId(String)`
- `getCorrectedChiefComplaint()` / `setCorrectedChiefComplaint(String)`

### RecommendedDepartment（修改）

**形态**：class
**包路径**：com.aimedical.modules.ai.api.dto.triage
**职责**：推荐科室 DTO

```java
public class RecommendedDepartment {
    private String departmentName;
    private String departmentId;
    private float score;
}
```

**新增字段**（保留已有字段不变）：

| 字段 | 类型 | 说明 |
|------|------|------|
| departmentId | `String` | 科室 ID |
| score | `float` | 匹配分数 |

**新增 getter/setter**：
- `getDepartmentId()` / `setDepartmentId(String)`
- `getScore()` / `setScore(float)`

## 错误处理

纯 DTO + 工厂类，不涉及业务错误处理。AiResultFactory 为纯静态工具类，无受检异常抛出。

## 行为契约

- **AiResultFactory** 为 final 类 + private 构造器，禁止继承和实例化
- **failure(errorCode, partialData)**：构造 `AiResult<T>`，success=false, data=partialData, errorCode=传入值, degraded=false, fallbackReason=null
- **degraded(fallbackReason, partialData)**：构造 `AiResult<T>`，success=false, data=partialData, errorCode=null, degraded=true, fallbackReason=传入值
- **failure(errorCode)**：行为同 `AiResult.failure(errorCode)`，即 `new AiResult<>(false, null, errorCode, false, null)`
- **success(data)**：行为同 `AiResult.success(data)`，即 `new AiResult<>(true, data, null, false, null)`
- 所有 DTO 字段通过 setter 设置、getter 读取，无调用顺序约束
- TriageRequest 中 List 类型字段默认值为 null（可选语义）
- TriageResponse 中 boolean 字段默认值为 false，Float 字段默认值为 null

## 依赖关系

- **TriageRequest** 依赖 `AdditionalResponseItem`（同包）
- **TriageResponse** 依赖 `RecommendedDoctor`、`MatchedRuleItem`（同包），依赖 `RecommendedDepartment`（已有）
- **RecommendedDepartment** 无新增外部依赖
- **AiResultFactory** 依赖 `AiResult`（同包）
- 所有新增/修改的类均位于 ai-api 模块内，无跨模块依赖
