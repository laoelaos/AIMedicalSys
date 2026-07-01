# 详细设计（v5）

## 概述

修复 `TriageServiceImplTest.shouldNotSetDepartmentFieldsWhenFinalDepartmentsJsonIsNull` 测试失败问题。该测试意图验证当三路科室路由全部返回空时（AI 失败 + 规则引擎空 + fallback 空），`finalDepartmentsJson` 为 null，科室字段不被设置。当前因 `StubFallbackProvider` 缺少 `returnEmpty` 标志，始终返回非空列表，导致断言失败。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `AIMedical/backend/modules/consultation/src/test/java/com/aimedical/modules/consultation/TriageServiceImplTest.java` | 修改 | 1. StubFallbackProvider 增加 returnEmpty 标志；2. 测试方法设置 fallbackProvider.returnEmpty = true |

## 类型定义

### 设计说明

本次修改仅在测试内部类 `StubFallbackProvider`（`TriageServiceImplTest.java:560-567`）中添加一个 `boolean returnEmpty` 字段，并在 `getFallbackDepartments()` 中根据该字段决定返回值。该字段默认 `false`（维持现有行为），测试中可设为 `true` 以模拟 fallback 返回空列表的场景。

## 错误处理

不涉及。纯测试桩修改，无新错误处理逻辑。

## 行为契约

### StubFallbackProvider.getFallbackDepartments()

- **前置条件**：无
- **后置条件**：当 `returnEmpty == true` 时返回 `Collections.emptyList()`；否则返回 `[RecommendedDepartment("fallback-dept-id", "内科", 0f)]`
- **默认行为**：`returnEmpty` 默认为 `false`，不影响其他现有测试

### shouldNotSetDepartmentFieldsWhenFinalDepartmentsJsonIsNull

- **前置条件**：`aiService.resultFuture = AiResult.failure("AI_ERROR")` + `ruleEngine.returnEmpty = true` + `fallbackProvider.returnEmpty = true`
- **后置条件**：三路全部返回空 → `departments` 列表为空 → `departmentsJson = null` → `finalDepartmentsJson = null` → `saveTriageRecord` 不设置 `ruleMatchedDepartments` 和 `aiRecommendedDepartments` → `assertNull` 通过

## 依赖关系

| 依赖 | 关系 |
|------|------|
| `DepartmentFallbackProvider` 接口 | StubFallbackProvider 实现的接口，不变 |
| `StubFallbackProvider` | 仅修改测试内部类，不修改生产代码 |
| 其他测试方法 | 不受影响（`returnEmpty` 默认 false） |

## 具体变更加总

### 变更 A：StubFallbackProvider 增加 returnEmpty 字段

**位置**：`TriageServiceImplTest.java`，内部类 `StubFallbackProvider`（约第 560 行）

```java
private static class StubFallbackProvider implements DepartmentFallbackProvider {
    boolean returnEmpty = false;  // 新增

    @Override
    public List<RecommendedDepartment> getFallbackDepartments() {
        if (returnEmpty) {  // 新增条件
            return Collections.emptyList();
        }
        List<RecommendedDepartment> list = new ArrayList<>();
        list.add(new RecommendedDepartment("fallback-dept-id", "内科", 0f));
        return list;
    }
}
```

### 变更 B：测试方法中设置 fallbackProvider.returnEmpty = true

**位置**：`TriageServiceImplTest.java`，`shouldNotSetDepartmentFieldsWhenFinalDepartmentsJsonIsNull`（约第 434-448 行）

在 `ruleEngine.returnEmpty = true;` 后追加：

```java
ruleEngine.returnEmpty = true;
fallbackProvider.returnEmpty = true;  // 新增：使 fallback 也返回空
```
