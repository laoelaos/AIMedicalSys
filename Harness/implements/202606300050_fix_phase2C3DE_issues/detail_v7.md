# 详细设计（v7）

## 概述

修复规则引擎快照失效回退（C13）和关键词解析（C16）两个 P1 级别缺陷。通过新建 `MatchResult` 类封装返回结果，修改 `TriageRuleEngine` 接口及 `DefaultTriageRuleEngine.match()` 实现，改造 `TriageServiceImpl` 降级路径适配新返回类型。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `consultation/src/main/java/com/aimedical/modules/consultation/rule/MatchResult.java` | 新建 | 封装 match 返回结果（departments + ruleVersionMismatch） |
| `consultation/src/main/java/com/aimedical/modules/consultation/rule/TriageRuleEngine.java` | 修改 | match() 返回类型改为 MatchResult |
| `consultation/src/main/java/com/aimedical/modules/consultation/rule/DefaultTriageRuleEngine.java` | 修改 | 实现 C13（快照回退）+ C16（关键词解析）+ 排序 + ObjectMapper 复用 |
| `consultation/src/main/java/com/aimedical/modules/consultation/service/impl/TriageServiceImpl.java` | 修改 | 降级路径适配 MatchResult，设置 ruleVersionMismatch |
| `consultation/src/test/java/com/aimedical/modules/consultation/DefaultTriageRuleEngineTest.java` | 修改 | 4 处 engine.match() 调用适配 MatchResult |
| `consultation/src/test/java/com/aimedical/modules/consultation/TriageServiceImplTest.java` | 修改 | StubTriageRuleEngine.match() 返回类型改为 MatchResult |

## 类型定义

### MatchResult
**形态**：class
**包路径**：`com.aimedical.modules.consultation.rule`
**职责**：封装 TriageRuleEngine.match() 的返回结果，包含匹配到的推荐科室列表和规则版本不匹配标记。

```java
public class MatchResult {
    private List<RecommendedDepartment> departments;
    private boolean ruleVersionMismatch;

    public MatchResult(List<RecommendedDepartment> departments, boolean ruleVersionMismatch) { ... }
    public List<RecommendedDepartment> getDepartments() { ... }
    public boolean isRuleVersionMismatch() { ... }
}
```

**公开接口**：
- `MatchResult(List<RecommendedDepartment> departments, boolean ruleVersionMismatch)` — 全参构造
- `List<RecommendedDepartment> getDepartments()`
- `boolean isRuleVersionMismatch()`

**构造方式**：直接构造器实例化
**类型关系**：无继承/实现

### TriageRuleEngine（接口修改）
**形态**：interface
**包路径**：`com.aimedical.modules.consultation.rule`
**职责**：分诊规则引擎契约，根据症状匹配规则返回推荐科室。

**变更说明**：`match()` 返回类型从 `List<RecommendedDepartment>` 改为 `MatchResult`

```java
public interface TriageRuleEngine {
    MatchResult match(String chiefComplaint, String ruleVersion, String ruleSetId);
    String currentRuleVersion();
    String currentRuleSetId();
}
```

**公开接口**：
- `MatchResult match(String chiefComplaint, String ruleVersion, String ruleSetId)`
- `String currentRuleVersion()`
- `String currentRuleSetId()`

### DefaultTriageRuleEngine（实现修改）
**形态**：class
**包路径**：`com.aimedical.modules.consultation.rule`
**职责**：TriageRuleEngine 默认实现，基于 TriageRuleRepository + Caffeine 缓存提供规则匹配。

**新增字段**：
- `private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()` — 线程安全，JSON 解析复用

**方法变更——match()**：
```java
@Override
public MatchResult match(String chiefComplaint, String ruleVersion, String ruleSetId) {
    // 1. 从缓存获取全量启用规则
    // 2. 按 ruleVersion + ruleSetId 过滤（快照版本匹配）
    // 3. 若快照过滤结果为空，改用不限定版本/集 ID 重新过滤，设 ruleVersionMismatch = true
    // 4. 对过滤后的规则逐条解析 conditions JSON，按 chiefComplaint 做关键词匹配
    //    - conditions 为 null/空/解析失败 → 该规则无条件通过
    //    - AND 逻辑：全部 keywords 在 chiefComplaint 中包含 → 命中
    //    - OR 逻辑：任一 keyword 在 chiefComplaint 中包含 → 命中
    // 5. 命中规则按 score 降序排序
    // 6. 转换为 List<RecommendedDepartment>
    // 7. 构造并返回 MatchResult(departments, ruleVersionMismatch)
}
```

**行为契约**：
1. `ruleVersion` 和 `ruleSetId` 均为 null 时，不过滤版本/集（当前行为）
2. 快照回退仅发生在：`ruleVersion` 或 `ruleSetId` 非 null 且过滤结果为空时
3. conditions JSON 结构：`{"keywords": ["胸痛", "胸闷"], "logic": "AND"}`，其中 `logic` 仅支持 `"AND"` / `"OR"`，默认按 AND 处理
4. keywords 匹配：`chiefComplaint.toLowerCase().contains(keyword.toLowerCase())`
5. 向后兼容：conditions 为 null / 空字符串 / 解析异常时，该规则无条件通过

### TriageServiceImpl（调用适配）
**变更位置**：`TriageServiceImpl.java:126-127`

```java
// 原：
List<RecommendedDepartment> ruleMatched = triageRuleEngine.match(
    request.getChiefComplaint(), request.getRuleVersion(), request.getRuleSetId());

// 改为：
MatchResult matchResult = triageRuleEngine.match(
    request.getChiefComplaint(), request.getRuleVersion(), request.getRuleSetId());
List<RecommendedDepartment> ruleMatched = matchResult.getDepartments();
```

**新增设置**：在 `fallbackResponse` 设置处（~line 136-146）增加：
```java
fallbackResponse.setRuleVersionMismatch(matchResult.isRuleVersionMismatch());
```

## 错误处理

- conditions JSON 解析失败：使用 try-catch 捕获 `JsonProcessingException`，将该规则视为无条件通过（向后兼容），日志不额外输出
- 规则列表为空时：`ruleVersionMismatch` 按快照回退结果设置，返回空 departments 列表的 MatchResult

## 行为契约

### 方法调用顺序
`DefaultTriageRuleEngine.match()` 内部步骤顺序不可变更：
1. 快照版本过滤（先）
2. 快照失效回退（仅在步骤 1 结果为空时触发）
3. 关键词匹配过滤（对步骤 1 或步骤 2 的结果逐条执行）
4. score 降序排序（对步骤 3 的结果）
5. 类型转换 + MatchResult 构造

### 状态变化规则
- `ruleVersionMismatch` 仅受快照版本过滤结果影响，与关键词匹配/排序无关
- ObjectMapper 是 `private static final`，不依赖实例状态

## 依赖关系

### 依赖的已有类型
- `com.aimedical.modules.consultation.dto.RecommendedDepartment` — 返回值元素类型
- `com.aimedical.modules.consultation.rule.entity.TriageRule` — 规则实体，含 conditions/score 等字段
- `com.aimedical.modules.consultation.repository.TriageRuleRepository` — 数据源
- `com.fasterxml.jackson.databind.ObjectMapper` — JSON 解析
- `com.aimedical.modules.consultation.dto.TriageResponse` — ruleVersionMismatch 字段已存在

### 暴露给后续任务的公开接口
- `TriageRuleEngine.match()` 返回 `MatchResult` — R8 将在 `TriageServiceImpl` 调用处将参数改为 session 快照值

## 测试对齐

### DefaultTriageRuleEngineTest.java（4 处变更）
| 行号 | 原代码 | 新代码 |
|------|-------|-------|
| L26 | `List<RecommendedDepartment> result = engine.match(...)` | `MatchResult mr = engine.match(...); List<RecommendedDepartment> result = mr.getDepartments()` |
| L37 | 同上 | 同上 |
| L49 | 同上 | 同上 |
| L61 | 同上 | 同上 |

### TriageServiceImplTest.java（StubTriageRuleEngine，L755-772）
- `match()` 返回类型从 `List<RecommendedDepartment>` 改为 `MatchResult`
- 构造 `new MatchResult(list, false)` 返回
- `returnEmpty` 分支改为返回 `new MatchResult(Collections.emptyList(), false)`
