# 任务指令（v3）

## 动作
NEW

## 任务描述

修复 consultation 模块三个 P1 级别缺陷——C03/A04/T44 correctedChiefComplaint 数据流 API 响应层断裂 + C16 规则引擎 JSON 解析失败 + C17 医生列表 score 排序偏离 OOD 设计。

### 1. C03/A04/T44 — correctedChiefComplaint API 响应层断裂

**文件**：`AIMedical/backend/modules/consultation/src/main/java/com/aimedical/modules/consultation/dto/TriageResponse.java`

- 新增 `private String correctedChiefComplaint;` 字段
- 新增对应的 `getCorrectedChiefComplaint()` / `setCorrectedChiefComplaint(String)` 方法

**文件**：`AIMedical/backend/modules/consultation/src/main/java/com/aimedical/modules/consultation/converter/TriageConverter.java`

- 在 `toTriageResponse()` 方法第107-109行的 `if` 块内增加：
  ```java
  response.setCorrectedChiefComplaint(aiData.getCorrectedChiefComplaint());
  ```
  该块现有代码为：
  ```java
  if (session != null && aiData != null && aiData.getCorrectedChiefComplaint() != null) {
      session.setCorrectedChiefComplaint(aiData.getCorrectedChiefComplaint());
  }
  ```
  修改后：
  ```java
  if (session != null && aiData != null && aiData.getCorrectedChiefComplaint() != null) {
      session.setCorrectedChiefComplaint(aiData.getCorrectedChiefComplaint());
      response.setCorrectedChiefComplaint(aiData.getCorrectedChiefComplaint());
  }
  ```

### 2. C16 — DefaultTriageRuleEngine JSON 解析失败返回 true

**文件**：`AIMedical/backend/modules/consultation/src/main/java/com/aimedical/modules/consultation/rule/DefaultTriageRuleEngine.java`

- `matchesConditions` 方法第113-115行 catch 块：
  - `return true;` → `return false;`
  - 添加 `log.warn("Failed to parse conditions JSON for rule, skipping: {}", conditionsJson, e);` 日志输出
- 需在类中添加 Logger（若尚未存在）

### 3. C17 — 医生列表 score 排序偏离 OOD 设计

**文件**：`AIMedical/backend/modules/consultation/src/main/java/com/aimedical/modules/consultation/service/impl/TriageServiceImpl.java`

- 在 `findDoctorsForDepartments` 方法（第221行）上方添加 TODO 注释：
  ```java
  // TODO: OOD 要求按 score 排序取前 5 名，但 AvailableDoctor 无 score 字段，
  //       当前使用 availableSlotCount 降序作为替代，待 DoctorFacade 补充 score 后修正
  ```
- 排序逻辑保持不变（availableSlotCount 降序）
- score 硬编码 0f 保持不变

## 选择理由

R2 已完成 consultation 模块全量 P0 并发安全修复。三个 P1 缺陷均属 consultation 模块核心路径，合并一轮减少上下文切换：
- correctedChiefComplaint 数据流 AI→session→TriageRecord 已通，但 consultation TriageResponse DTO 缺字段 → 前端无法感知主诉修正
- C16 规则引擎 JSON 解析失败返回 true → 所有患者被分诊到同一科室，属逻辑正确性缺陷
- C17 排序不匹配 OOD 设计，但受限于 AvailableDoctor 签名，仅需补充 TODO 标记已知设计偏差

## 任务上下文

摘录自诊断报告 `Docs\Diagnosis\impl\07_phase2C3DE_report.md`：

- **C03/A04/T44**：AI 返回 correctedChiefComplaint → session 被设置 → 持久化到 TriageRecord → 但前端 TriageResponse 无此字段 → 前端无法感知修正
- **C16**：`matchesConditions` 中 `catch (JsonProcessingException e) { return true; }` 意味着规则条件解析失败时规则无条件匹配所有主诉
- **C17**：`findDoctorsForDepartments` 使用 `availableSlotCount` 降序排序，而非 OOD 要求的 score 排序；RecommendedDoctor 构造时 score 硬编码为 0f

## 已有代码上下文

- `consultation/dto/TriageResponse.java`：包含 departments/doctors/reason/matchedRules/sessionId/needFollowUp/followUpQuestion/confidence/degraded/fallbackHint/ruleVersionMismatch，**缺少 correctedChiefComplaint**
- `ai-api/dto/triage/TriageResponse.java`：已有 `correctedChiefComplaint` 字段（第16行）
- `converter/TriageConverter.java` 第107-109行：`session.setCorrectedChiefComplaint(aiData.getCorrectedChiefComplaint())` — 仅写回 session，未设到 response
- `rule/DefaultTriageRuleEngine.java` 第113行：`catch (JsonProcessingException e) { return true; }`
- `service/impl/TriageServiceImpl.java` 第221-249行：`findDoctorsForDepartments` — AvailableDoctor 构造时 score=0f，排序用 availableSlotCount
- `common-module-api/doctor/AvailableDoctor.java`：record(doctorId, doctorName, departmentId, availableSlotCount) — **无 score 字段**

## 预期文件变更清单

| 操作 | 文件路径 |
|------|---------|
| 修改 | `consultation/dto/TriageResponse.java` — 新增 correctedChiefComplaint + getter/setter |
| 修改 | `consultation/converter/TriageConverter.java` — toTriageResponse 中 add response.setCorrectedChiefComplaint |
| 修改 | `consultation/rule/DefaultTriageRuleEngine.java` — catch 块 return true→false + 加日志 |
| 修改 | `consultation/service/impl/TriageServiceImpl.java` — 加 TODO 注释 |
