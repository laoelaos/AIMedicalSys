# 任务指令（v8）

## 动作
RETRY

## 任务描述
修复 R7 验证失败：TriageServiceImplTest.java:663 编译错误 — `result.isRuleVersionMismatch()` 方法在 TriageResponse 中不存在。

修改 1 个文件：
- `modules/consultation/src/test/java/com/aimedical/modules/consultation/TriageServiceImplTest.java:663`：`assertTrue(result.isRuleVersionMismatch())` → `assertTrue(result.getRuleVersionMismatch())`

无需修改生产代码。修改后运行 `DefaultTriageRuleEngineTest` (7 tests) + `TriageServiceImplTest` 验证通过。

## 选择理由
R7 生产代码实现正确（DefaultTriageRuleEngine 快照回退 + 关键词解析 + MatchResult 返回类型），仅测试文件存在编译错误。TriageResponse.ruleVersionMismatch 字段类型为 `Boolean`（包装类型），Java Bean 规范下包装类型 getter 为 `getRuleVersionMismatch()`，但测试误用了 `isRuleVersionMismatch()`。此修复极低风险，应在 1 分钟内完成。

## 任务上下文
R7 实现内容：
- 新建 MatchResult（rule 包），封装 departments + ruleVersionMismatch
- TriageRuleEngine.match() 返回类型改为 MatchResult
- DefaultTriageRuleEngine 实现快照版本无结果→降级使用当前最新规则集重新匹配
- TriageRule.conditions JSON 关键词解析（AND/OR 逻辑）+ 按 score 降序排序
- TriageServiceImpl 降级路径适配 MatchResult，设置 ruleVersionMismatch
- DefaultTriageRuleEngineTest 4 处 match 调用适配 MatchResult
- TriageServiceImplTest StubTriageRuleEngine.match() 返回类型改为 MatchResult

验证失败关键信息：
```
consultation 模块编译错误:
TriageServiceImplTest.java:663 — result.isRuleVersionMismatch() 方法在
com.aimedical.modules.consultation.dto.TriageResponse 中不存在。
```

## 已有代码上下文

### TriageServiceImplTest.java:653-664
```java
@Test
void shouldSetRuleVersionMismatchOnFallbackResponse() {
    DialogueCreateRequest request = createBasicRequest();
    aiService.resultFuture = CompletableFuture.completedFuture(
            AiResult.failure("AI_ERROR"));
    ruleEngine.returnMismatch = true;

    com.aimedical.modules.consultation.dto.TriageResponse result = service.triage(request);

    assertTrue(result.isDegraded());
    assertTrue(result.isRuleVersionMismatch());  // ← 编译错误
}
```

### TriageResponse.java:17,102-108
```java
private Boolean ruleVersionMismatch;  // 包装类型 Boolean

public Boolean getRuleVersionMismatch() {
    return ruleVersionMismatch;
}

public void setRuleVersionMismatch(Boolean ruleVersionMismatch) {
    this.ruleVersionMismatch = ruleVersionMismatch;
}
```

## RETRY 说明
失败原因：TriageResponse.ruleVersionMismatch 声明为 `Boolean`（包装类型），Java Bean 规范下包装类型 getter 为 `getRuleVersionMismatch()` 而非 `isRuleVersionMismatch()`。测试行 663 使用了错误的 `isRuleVersionMismatch()`，导致编译失败。

修正方向：TriageServiceImplTest.java:663 `assertTrue(result.isRuleVersionMismatch())` → `assertTrue(result.getRuleVersionMismatch())`
