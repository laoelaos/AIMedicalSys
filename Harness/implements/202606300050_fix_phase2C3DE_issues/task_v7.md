# 任务指令（v7）

## 动作
NEW

## 任务描述
实现规则引擎快照失效回退+关键词解析（C13+C16）

涉及文件：
- `AIMedical/backend/modules/consultation/src/main/java/com/aimedical/modules/consultation/rule/TriageRuleEngine.java`
- `AIMedical/backend/modules/consultation/src/main/java/com/aimedical/modules/consultation/rule/DefaultTriageRuleEngine.java`
- `AIMedical/backend/modules/consultation/src/main/java/com/aimedical/modules/consultation/rule/entity/TriageRule.java`
- `AIMedical/backend/modules/consultation/src/main/java/com/aimedical/modules/consultation/rule/MatchResult.java`（新建）
- `AIMedical/backend/modules/consultation/src/main/java/com/aimedical/modules/consultation/service/impl/TriageServiceImpl.java`

## 选择理由
C13(P1)+C16(P1) 为规则引擎核心缺陷。R1/R6 已完成数据链路和 DoctorFacade 修复，此轮修复规则匹配引擎本身，为降级路径提供正确的规则匹配逻辑。无前置依赖。

## 任务上下文
来自实现报告 `Docs/Diagnosis/impl/06_phase2C3DE_report.md`：

**C13(P1)**: `DefaultTriageRuleEngine.match()` 使用快照版本（ruleVersion + ruleSetId）查询无结果时→降级使用当前最新版本规则集重新匹配，并标记 `ruleVersionMismatch=true`。OOD §3.1 要求"降级使用当前最新版本规则集重新匹配，并在 TriageResponse 中标记 ruleVersionMismatch=true"。

**C16(P1)**: `DefaultTriageRuleEngine.match()` 仅按规则版本和集标识过滤返回全量命中规则，未对 `conditions` JSON 做关键词解析与 AND/OR 逻辑匹配。OOD §3.1 TriageRule 设计 `conditions` 为关键词匹配条件（`{"keywords": ["胸痛", "胸闷"], "logic": "AND"}`）。需实现：解析 `conditions` JSON，按 keywords 匹配主诉字符串（`chiefComplaint` 包含任一/所有关键词），按 logic 字段确定组合逻辑（AND 要求全部命中，OR 要求任一命中），未命中则排除该规则。

## 已有代码上下文

### 1. `TriageRuleEngine` 接口（当前）
```java
List<RecommendedDepartment> match(String chiefComplaint, String ruleVersion, String ruleSetId);
```

### 2. `DefaultTriageRuleEngine.match` 当前实现
```java
List<TriageRule> rules = ruleCache.get("all_enabled");
// 仅按 ruleVersion + ruleSetId 过滤
List<TriageRule> filtered = rules.stream()
    .filter(r -> (version == null || version.equals(r.getRuleVersion()))
            && (setId == null || setId.equals(r.getRuleSetId()))
            && Boolean.TRUE.equals(r.getEnabled()))
    .collect(Collectors.toList());
// 直接转换为 RecommendedDepartment 返回，无关键词匹配、无限量/排序
for (TriageRule rule : filtered) {
    result.add(new RecommendedDepartment(rule.getResultDepartmentId(), rule.getResultDepartmentName(), rule.getScore()));
}
```

### 3. `TriageRule.conditions` 字段
- JSON 文本格式：`{"keywords": ["胸痛", "胸闷"], "logic": "AND"}`
- keywords: 症状关键词列表（List<String>）
- logic: AND（全部命中）/ OR（任一命中）

### 4. `TriageResponse` 类
- 已有 `Boolean ruleVersionMismatch` 字段及 getter/setter
- 调用方在 `TriageServiceImpl.java:122-148` 降级路径中创建并设置 `fallbackResponse`

## R7 实施要点

### 1. 新建 `MatchResult` 类
包路径：`com.aimedical.modules.consultation.rule`（与 TriageRuleEngine 同包，非 dto 包）
```java
public class MatchResult {
    private List<RecommendedDepartment> departments;
    private boolean ruleVersionMismatch;
    // 全参构造 + getter
}
```

### 2. 修改 `TriageRuleEngine` 接口
- 将 `match()` 返回类型从 `List<RecommendedDepartment>` 改为 `MatchResult`
- 变更后签名：`MatchResult match(String chiefComplaint, String ruleVersion, String ruleSetId)`

### 3. 修改 `DefaultTriageRuleEngine.match` 实现
**ObjectMapper 复用**：ObjectMapper 应定义为 `DefaultTriageRuleEngine` 的 `private static final` 字段（线程安全），或通过构造器注入 Spring 管理的 ObjectMapper Bean。禁止每次调用 `match()` 时 `new ObjectMapper()`。


**C13 — 快照失效回退**：
1. 先按 ruleVersion + ruleSetId 过滤规则
2. 若过滤结果为空（快照版本无匹配规则），则重新按不限定版本/集 ID 过滤（使用当前最新版本规则集），设 `ruleVersionMismatch = true`
3. 若快照过滤有结果，则 `ruleVersionMismatch = false`

**C16 — 关键词解析**：
1. 对上述过滤后的规则列表，逐条解析 `conditions` JSON
2. 使用 Jackson ObjectMapper 解析为 `{"keywords": [...], "logic": "AND|OR"}`
3. `chiefComplaint` 为匹配目标字符串（大小写不敏感）
4. AND 逻辑：全部 keywords 在 chiefComplaint 中包含 → 命中
5. OR 逻辑：任一 keyword 在 chiefComplaint 中包含 → 命中
6. `conditions` 为 null/空/解析失败时：该规则无条件通过（向后兼容）
7. 仅保留命中规则

**排序**：
1. 命中规则按 `score` 字段降序排序（`Comparator.comparing(TriageRule::getScore).reversed()`）

**转换为 MatchResult**：
1. 将排序后的规则列表转换为 `List<RecommendedDepartment>`（保持现有转换逻辑）
2. 构造 `MatchResult(departments, ruleVersionMismatch)` 返回

### 4. 修改 `TriageServiceImpl` 降级路径调用
在 `TriageServiceImpl.java:126` 处：
```java
// 原：List<RecommendedDepartment> ruleMatched = triageRuleEngine.match(...)
MatchResult matchResult = triageRuleEngine.match(request.getChiefComplaint(), request.getRuleVersion(), request.getRuleSetId());
List<RecommendedDepartment> ruleMatched = matchResult.getDepartments();
```
并在 `fallbackResponse` 设置（~line 136-146）中增加：
```java
fallbackResponse.setRuleVersionMismatch(matchResult.isRuleVersionMismatch());
```

### 5. 涉及文件清单
| 操作 | 文件 |
|------|------|
| 新建 | `consultation/src/main/java/com/aimedical/modules/consultation/rule/MatchResult.java` |
| 修改 | `consultation/src/main/java/com/aimedical/modules/consultation/rule/TriageRuleEngine.java` |
| 修改 | `consultation/src/main/java/com/aimedical/modules/consultation/rule/DefaultTriageRuleEngine.java` |
| 修改 | `consultation/src/main/java/com/aimedical/modules/consultation/service/impl/TriageServiceImpl.java` |
| 修改 | `consultation/src/test/java/com/aimedical/modules/consultation/DefaultTriageRuleEngineTest.java` |
| 修改 | `consultation/src/test/java/com/aimedical/modules/consultation/TriageServiceImplTest.java` |

### 6. 测试文件对齐要求

**DefaultTriageRuleEngineTest.java**（4 处 `engine.match()` 调用需适配）：
- L26: `List<RecommendedDepartment> result = engine.match(...)` → `MatchResult mr = engine.match(...)` + `List<RecommendedDepartment> result = mr.getDepartments()`
- L37: 同上
- L49: 同上
- L61: 同上

**TriageServiceImplTest.java**（StubTriageRuleEngine 第 755-772 行）：
- `match()` 返回类型从 `List<RecommendedDepartment>` 改为 `MatchResult`
- 构造 `new MatchResult(list, false)` 返回

### 7. R8 依赖链说明
R7 修改 `TriageServiceImpl.java ~L126` 使用 `MatchResult`：
```java
MatchResult matchResult = triageRuleEngine.match(request.getChiefComplaint(), request.getRuleVersion(), request.getRuleSetId());
List<RecommendedDepartment> ruleMatched = matchResult.getDepartments();
```
R8 将在此同一位置将参数改为 session 快照值（`session.getRuleVersion()` / `session.getRuleSetId()`），请实现时注意此后续修改。

---

## 修订说明（v7 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| 接口返回类型变更导致 2 测试文件编译失败——DefaultTriageRuleEngineTest 4 处 engine.match 调用 + TriageServiceImplTest StubTriageRuleEngine 返回类型 | 文件清单新增 DefaultTriageRuleEngineTest.java（4 处适配 matchResult.getDepartments()）和 TriageServiceImplTest.java（StubTriageRuleEngine.match 返回 MatchResult）；新增第 6 节测试文件对齐要求 |
| DefaultTriageRuleEngine 中 JSON 解析每次 new ObjectMapper() 性能浪费 | 第 3 节开头增加 ObjectMapper 复用约束：private static final 字段或构造器注入 |
| MatchResult 放 dto 包语义不精确 | 包路径改为 com.aimedical.modules.consultation.rule（与 TriageRuleEngine 同包） |
| R7 完成后 R8 将在同一位置（~L126）修改，存在合并冲突风险 | 新增第 7 节 R8 依赖链说明，注明 R8 将把参数改为 session 快照值 |
