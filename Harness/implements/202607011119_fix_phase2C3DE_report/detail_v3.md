# 详细设计（v3）

## 概述

修复 consultation 模块三个 P1 级别缺陷：C03/A04/T44（correctedChiefComplaint API 响应层断裂）、C16（规则引擎 JSON 解析失败返回 true）、C17（医生列表 score 排序偏离 OOD 设计）。涉及 4 个源文件修改，2 个测试文件相应更新。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `AIMedical/backend/modules/consultation/src/main/java/com/aimedical/modules/consultation/dto/TriageResponse.java` | 修改 | 新增 `correctedChiefComplaint` 字段 + getter/setter |
| `AIMedical/backend/modules/consultation/src/main/java/com/aimedical/modules/consultation/converter/TriageConverter.java` | 修改 | `toTriageResponse` 中追加 `response.setCorrectedChiefComplaint()` |
| `AIMedical/backend/modules/consultation/src/main/java/com/aimedical/modules/consultation/rule/DefaultTriageRuleEngine.java` | 修改 | `matchesConditions` catch 块 `return true` → `return false` + `log.warn` |
| `AIMedical/backend/modules/consultation/src/main/java/com/aimedical/modules/consultation/service/impl/TriageServiceImpl.java` | 修改 | `findDoctorsForDepartments` 上方添加 TODO 注释 |
| `AIMedical/backend/modules/consultation/src/test/java/com/aimedical/modules/consultation/DefaultTriageRuleEngineTest.java` | 修改 | `shouldPassRuleWhenConditionsInvalidJson` 改为预期 `return false`（规则不匹配） |
| `AIMedical/backend/modules/consultation/src/test/java/com/aimedical/modules/consultation/TriageConverterTest.java` | 修改 | `shouldConvertToTriageResponseWithAiData` 新增断言验证 `result.getCorrectedChiefComplaint()` |

## 类型定义

### `TriageResponse`（consultation DTO，已有类，修改）

**形态**：class
**包路径**：`com.aimedical.modules.consultation.dto`
**职责**：分诊返回 DTO；新增 correctedChiefComplaint 字段保证前端可感知 AI 主诉修正

**新增字段**：
```
private String correctedChiefComplaint;
```

**新增方法**：
```
public String getCorrectedChiefComplaint()
public void setCorrectedChiefComplaint(String correctedChiefComplaint)
```

**修改点**：在现有字段列表末尾、`ruleVersionMismatch` 之后插入新字段及对应 getter/setter。构造器不变。

### `TriageConverter`（已有类，修改）

**包路径**：`com.aimedical.modules.consultation.converter`
**职责**：AI 响应转 consultation DTO；将 correctedChiefComplaint 不仅写回 session 也写入 response

**修改点**：`toTriageResponse` 方法第107-109行 if 块内追加一行：
```java
response.setCorrectedChiefComplaint(aiData.getCorrectedChiefComplaint());
```
修改后 if 块内容：
```java
if (session != null && aiData != null && aiData.getCorrectedChiefComplaint() != null) {
    session.setCorrectedChiefComplaint(aiData.getCorrectedChiefComplaint());
    response.setCorrectedChiefComplaint(aiData.getCorrectedChiefComplaint());
}
```

**不修改**：`toAiTriageRequest` 方法及其他转换逻辑保持不变。

### `DefaultTriageRuleEngine`（已有类，修改）

**包路径**：`com.aimedical.modules.consultation.rule`
**职责**：规则引擎；修正 JSON 解析失败时错误地返回 true（规则无条件匹配所有主诉）

**新增导入**（若尚未存在 Logger）：
```
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
```

**新增静态字段**：
```
private static final Logger log = LoggerFactory.getLogger(DefaultTriageRuleEngine.class);
```

**修改点**：`matchesConditions` 方法 catch 块（第113-115行）：
```
// 修改前：
} catch (JsonProcessingException e) {
    return true;
}

// 修改后：
} catch (JsonProcessingException e) {
    log.warn("Failed to parse conditions JSON for rule, skipping: {}", conditionsJson, e);
    return false;
}
```

**不修改**：`match` 方法主体逻辑、缓存机制、规则排序规则保持不变。

### `TriageServiceImpl`（已有类，修改）

**包路径**：`com.aimedical.modules.consultation.service.impl`
**职责**：分诊服务实现；标记 OOD 设计偏差 — score 排序暂不可行

**修改点**：在 `findDoctorsForDepartments` 方法（第221行）上方插入 TODO 注释：
```java
// TODO: OOD 要求按 score 排序取前 5 名，但 AvailableDoctor 无 score 字段，
//       当前使用 availableSlotCount 降序作为替代，待 DoctorFacade 补充 score 后修正
```

**不修改**：`findDoctorsForDepartments` 方法体（包括排序逻辑 `Integer.compare(b.getAvailableSlotCount(), ...)` 和 `score=0f` 硬编码）保持不变。`saveTriageRecord`、`triage` 等方法不变。

## 错误处理

- **C03 场景**：AI 返回 `correctedChiefComplaint` → session 和 response DTO 均设置该值。response 不涉及异常路径。
- **C16 场景**：`matchesConditions` 中 `ObjectMapper.readTree` 抛出 `JsonProcessingException` → 日志记录警告 + `return false`（规则不匹配）。外部调用方 `match` 方法会照常继续评估其他规则；若全部不匹配则进入 fallback 逻辑（空列表 + ruleVersionMismatch 按原有逻辑判断），不影响后续流程。
- **C17 场景**：仅 TODO 注释，无运行期行为变更。

## 行为契约

- **C03**：`toTriageResponse` 在 `aiData.getCorrectedChiefComplaint() != null` 且 `session != null` 时，response 的 `correctedChiefComplaint` 字段必与 session 的该字段同值。当 session 或 aiData 为 null 时，不设 response 的 correctedChiefComplaint（保持 null）。
- **C16**：`matchesConditions(chiefComplaint, invalidJson)` → 返回 `false`，规则不匹配该主诉。日志输出 `WARN` 级别。
- **C17**：`findDoctorsForDepartments` 行为无变化；TODO 注释标记已知设计偏差，待 AvailableDoctor 增加 score 字段后修正。

## 依赖关系

- **C03**：依赖 `com.aimedical.modules.ai.api.dto.triage.TriageResponse.getCorrectedChiefComplaint()`（ai-api 模块已有，无需新增依赖）
- **C16**：新增依赖 `org.slf4j.Logger`、`org.slf4j.LoggerFactory`（SLF4J 已在 consultation 模块中使用，参见 `TriageServiceImpl` 第52行）
- **C17**：依赖 `com.aimedical.modules.commonmodule.doctor.AvailableDoctor` record 定义，该 record 当前无 score 字段

## 测试影响

### `DefaultTriageRuleEngineTest.shouldPassRuleWhenConditionsInvalidJson`（需修改）

- **当前行为**：断言 `conditions = "not valid json"` 时规则匹配（`assertEquals(1, mr.getDepartments().size())`）— 即 `return true` 导致规则无条件匹配
- **修改后行为**：断言规则不匹配（`assertTrue(mr.getDepartments().isEmpty())`）— 即 `return false` 后该规则被跳过

### `TriageConverterTest.shouldConvertToTriageResponseWithAiData`（需补充断言）

- **当前验证**：仅验证 `session.getCorrectedChiefComplaint()`（第100行）
- **补充验证**：追加 `assertEquals("修正后主诉：头痛疑似偏头痛", result.getCorrectedChiefComplaint())` 验证 response 字段
