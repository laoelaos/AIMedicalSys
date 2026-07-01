# 任务指令（v1）

## 动作
NEW

## 任务描述
在 ai-api 模块中扩展分诊（triage）相关 DTO 的字段定义，新增缺失的 DTO 类，新增 AiResultFactory 静态工厂类。

预期文件路径（所有文件均在 `AIMedical/backend/modules/ai/ai-api/src/main/java/com/aimedical/modules/ai/api/` 下）：
- `AiResultFactory.java` — 新增，提供 failure(errorCode, partialData)、degraded(fallbackReason, partialData)、success(data)、failure(errorCode) 重载静态工厂方法
- `dto/triage/TriageRequest.java` — 修改，扩展字段：additionalResponses（List\<AdditionalResponseItem\>，可选）、patientId（string，可选）、sessionId（string，可选）、ruleVersion（string，可选）、ruleSetId（string，可选）
- `dto/triage/TriageResponse.java` — 修改，扩展字段：recommendedDoctors（List\<RecommendedDoctor\>，可选）、matchedRules（List\<MatchedRuleItem\>，可选）、needFollowUp（boolean）、followUpQuestion（string，可选）、confidence（Float，可选）、degraded（boolean）、sessionId（string，可选）、correctedChiefComplaint（string，可选）
- `dto/triage/RecommendedDepartment.java` — 修改，扩展字段：departmentId（string）、score（float）
- `dto/triage/AdditionalResponseItem.java` — 新增，字段：question（string）/ answer（string）/ answeredAt（string）
- `dto/triage/RecommendedDoctor.java` — 新增，字段：doctorId / doctorName / departmentId / availableSlotCount（int）/ score（float）
- `dto/triage/MatchedRuleItem.java` — 新增，字段：ruleId / ruleName / score（float）

所有类使用 plain Java（无 Lombok），包含默认构造器、getter/setter，与现有 `AiResult.java` 和现有 DTO 风格保持一致。

AiResultFactory 契约（来自 §2.3）：
- `public static <T> AiResult<T> failure(String errorCode, T partialData)` — 超时失败路径，partialData 写入 data 字段
- `public static <T> AiResult<T> degraded(String fallbackReason, T partialData)` — 降级路径，partialData 写入 data 字段
- `public static <T> AiResult<T> failure(String errorCode)` — 无 partialData 的简化失败工厂（等价于 new AiResult<>(false, null, errorCode, false, null)）
- `public static <T> AiResult<T> success(T data)` — 成功工厂（等价于 new AiResult<>(true, data, null, false, null)）

## 选择理由
ai-api DTO 扩展是所有业务模块开发的前置依赖（§10 时序依赖：ai-api 层 DTO 扩展为四个业务模块开发的前置依赖——业务模块的 Converter、Service 实现依赖 ai-api 层 DTO 的完整字段定义）。Triage 分诊是患者端入口核心功能，优先扩展。AiResultFactory 需先于业务模块 Converter 创建，因为业务层依赖工厂类的重载方法承载 partialData。

## 任务上下文
摘录自 §10.1 和 §2.3：

**TriageRequest（ai-api 层）扩展字段**：chiefComplaint（string，已存在）、additionalResponses（List\<AdditionalResponseItem\>，可选）、patientId（string，可选）、sessionId（string，可选）、ruleVersion（string，可选）、ruleSetId（string，可选）。

**TriageResponse（ai-api 层）扩展字段**：recommendedDepartments（List\<RecommendedDepartment\>，已存在）、recommendedDoctors（List\<RecommendedDoctor\>，可选）、reason（string，已存在）、matchedRules（List\<MatchedRuleItem\>，可选）、needFollowUp（bool，可选）、followUpQuestion（string，可选）、confidence（float，可选）、degraded（bool，可选）、sessionId（string，可选）、correctedChiefComplaint（string，可选）。

**RecommendedDepartment（ai-api 层）扩展字段**：departmentId（string）、departmentName（string，已存在）、score（float）。

**新增 ai-api DTO**：AdditionalResponseItem（question/answer/answeredAt）、RecommendedDoctor（doctorId/doctorName/departmentId/availableSlotCount/score）、MatchedRuleItem（ruleId/ruleName/score）。

**AiResultFactory 位置**：com.aimedical.modules.ai.api.AiResultFactory。

## 已有代码上下文
- `AiResult.java` 已定义于 `com.aimedical.modules.ai.api`，含 5 字段（success/data/errorCode/degraded/fallbackReason）和 success(T data)、failure(String errorCode)、degraded(String fallbackReason) 三个静态工厂方法，但缺少 partialData 参数重载
- `TriageRequest.java` 已有 chiefComplaint 字段（String）和 getter/setter
- `TriageResponse.java` 已有 recommendedDepartments（List\<RecommendedDepartment\>）和 reason 字段
- `RecommendedDepartment.java` 已有 departmentName 字段
- 使用 plain Java 风格（无 Lombok）
