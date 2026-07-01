# 任务指令（v5）

## 动作
RETRY

## 任务描述
修复 `TriageServiceImplTest.shouldNotSetDepartmentFieldsWhenFinalDepartmentsJsonIsNull` 测试失败问题。

**涉及问题**：C04+E02+S04+C20（事务边界+UPDATE+并发控制群组的测试修复）
**涉及文件**：`modules/consultation/src/test/java/com/aimedical/modules/consultation/TriageServiceImplTest.java`（1 个文件）

### 具体变更

1. **StubFallbackProvider 增加 `returnEmpty` 标志**：
   - 在 `StubFallbackProvider` 内部类增加 `boolean returnEmpty = false` 字段
   - `getFallbackDepartments()` 方法中：当 `returnEmpty == true` 时返回 `Collections.emptyList()`，否则返回原有的 `[RecommendedDepartment("fallback-dept-id", "内科", 0f)]`

2. **修复测试方法 `shouldNotSetDepartmentFieldsWhenFinalDepartmentsJsonIsNull`**：
   - 在已设置的 `ruleEngine.returnEmpty = true` 之后，增加 `fallbackProvider.returnEmpty = true`
   - 使三路全部返回空：AI 失败（`AiResult.failure("AI_ERROR")`）→ 规则引擎空（`ruleEngine.returnEmpty = true`）→ fallback 空（`fallbackProvider.returnEmpty = true`）
   - 确保 `departments` 列表最终为空 → `finalDepartmentsJson` 为 null → `saveTriageRecord` 不设置任何科室字段 → `assertNull` 通过

## 选择理由
R4（R3 的 RETRY）验证 FAILED，但失败源于测试逻辑错误而非生产代码缺陷。生产代码修改 `aiResult.isDegraded()` → `response.isDegraded()` 正确，无需回退。只需修复测试桩和测试断言即可通过验证。此轮为同组问题（C04+E02+S04+C20）的第三次尝试，若仍失败则将标记 BLOCKED。

## 任务上下文
当前 `saveTriageRecord` 方法中（TriageServiceImpl.java:192-246）：
- `record.setDegraded(response.isDegraded())` — 正确，使用业务层最终降级决策
- 科室路由 `if (finalDepartmentsJson != null) { if (response.isDegraded()) { ... } else { ... } }` — 正确
- 测试 `shouldNotSetDepartmentFieldsWhenFinalDepartmentsJsonIsNull` 意图验证边界条件：当 `departments` 列表为空时，`finalDepartmentsJson` 为 null，科室字段应不被设置

当前测试设置 `ruleEngine.returnEmpty = true`，但 `StubFallbackProvider` 无 `returnEmpty` 标志，始终返回非空列表，导致 `departments` 不为空，`finalDepartmentsJson` 为 JSON 字符串，`ruleMatchedDepartments` 被设置。修复后需要三路全空才能触发边界条件。

## 已有代码上下文
### StubFallbackProvider（TriageServiceImplTest.java:560-567）
```java
private static class StubFallbackProvider implements DepartmentFallbackProvider {
    @Override
    public List<RecommendedDepartment> getFallbackDepartments() {
        List<RecommendedDepartment> list = new ArrayList<>();
        list.add(new RecommendedDepartment("fallback-dept-id", "内科", 0f));
        return list;
    }
}
```

### shouldNotSetDepartmentFieldsWhenFinalDepartmentsJsonIsNull（TriageServiceImplTest.java:434-448）
```java
@Test
void shouldNotSetDepartmentFieldsWhenFinalDepartmentsJsonIsNull() {
    DialogueCreateRequest request = createBasicRequest();
    aiService.resultFuture = CompletableFuture.completedFuture(
            AiResult.failure("AI_ERROR"));
    ruleEngine.returnEmpty = true;
    // 缺少 fallbackProvider.returnEmpty = true;

    service.triage(request);

    assertTrue(recordRepository.saved);
    assertNotNull(recordRepository.record);
    assertTrue(recordRepository.record.getDegraded());
    assertNull(recordRepository.record.getRuleMatchedDepartments());
    assertNull(recordRepository.record.getAiRecommendedDepartments());
}
```

### saveTriageRecord（TriageServiceImpl.java:192-246）
```java
private void saveTriageRecord(..., TriageResponse response) {
    String departmentsJson = null;
    // ...
    if (departments != null && !departments.isEmpty()) {
        departmentsJson = objectMapper.writeValueAsString(departments);
    }
    String finalDepartmentsJson = departmentsJson;
    transactionTemplate.execute(status -> {
        // ... record setup ...
        record.setDegraded(response.isDegraded());
        if (finalDepartmentsJson != null) {
            if (response.isDegraded()) {
                record.setRuleMatchedDepartments(finalDepartmentsJson);
            } else {
                record.setAiRecommendedDepartments(finalDepartmentsJson);
            }
        }
        // ...
    });
}
```

## RETRY 说明
### 验证失败原因
R4 验证 FAILED（26 通过，1 失败）：
- `TriageServiceImplTest.shouldNotSetDepartmentFieldsWhenFinalDepartmentsJsonIsNull:446` — `expected: <null> but was: <[{"departmentId":"fallback-dept-id","departmentName":"内科","score":0.0}]>`

**根因分析**：测试 `shouldNotSetDepartmentFieldsWhenFinalDepartmentsJsonIsNull` 设置 `ruleEngine.returnEmpty = true` 期望 `finalDepartmentsJson` 为 null，但：
1. `StubFallbackProvider.getFallbackDepartments()` 始终返回 `[RecommendedDepartment("fallback-dept-id", "内科", 0f)]`
2. 降级路径中规则引擎返回空后走 fallback → `departments = fallbackProvider.getFallbackDepartments()` 非空
3. `departmentsJson = objectMapper.writeValueAsString(departments)` 得到 JSON 字符串，非 null
4. `finalDepartmentsJson` 非 null → 进入 department 设置分支 → `ruleMatchedDepartments` 被设置为 fallback 部门 JSON
5. `assertNull(record.getRuleMatchedDepartments())` 失败

### 修正方向
1. StubFallbackProvider 增加 `returnEmpty` 标志
2. 测试中设置 `fallbackProvider.returnEmpty = true`，使三路代理全部返回空
3. 保持现有生产代码不变（`response.isDegraded()` 修改正确）
