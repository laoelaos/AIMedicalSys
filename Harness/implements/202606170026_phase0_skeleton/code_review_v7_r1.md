# 代码审查报告（v7 r1）

## 审查结果
REJECTED

## 发现

- **[一般]** `src/test/java/.../mock/MockAiServiceTest.java` — **缺少 `shouldBeAnnotatedWithService` 测试**。设计明细（第 191 行）明确规定 14 个测试方法，其中包括类级别 `@Service` 注解检查。实际只实现了 13 个方法测试，缺少对 `@Service` 存在的验证。

- **[一般]** `src/test/java/.../mock/MockAiServiceTest.java` — **`triageShouldReturnSuccessResult` 未验证 triage 特有字段**。设计（第 192 行，表头 triage 行）要求验证 `recommendedDepartments[0].departmentName="mock_departmentName"` 和 `reason="mock_reason"`，但该测试仅复用了与其余 12 个方法相同的通用断言（success、degraded、data 非 null），未对 triage 特定数据进行断言。

- **[一般]** `src/test/java/.../degradation/NoOpDegradationStrategyTest.java` — **缺少 `shouldBeAnnotatedWithComponent` 测试**。设计（第 213-214 行）明确列出 2 个测试方法，包含 `@Component` 注解检查。实际只实现了 `shouldAlwaysReturnFalse`，缺少注解验证。

- **[一般]** `src/test/java/.../fallback/FallbackAiServiceTest.java` — **缺少 `shouldReturnOriginalResultWhenDelegateAlreadyDegraded` 测试**。设计（第 228-229 行）要求验证当委托返回 `degraded=true` 时，FallbackAiService 直接返回原结果而不经策略判定。该测试场景未实现。

- **[轻微]** `src/test/java/.../mock/MockAiServiceTest.java` — 所有方法测试未对返回的 `CompletableFuture` 调用 `future.isDone()` 验证。设计（第 191 行，表头）要求验证 future 非 null 且 isDone。

- **[轻微]** `src/main/java/.../fallback/FallbackAiService.java` — 构造器在第 57 行直接输出 ERROR 日志，设计要求"构造后首次调用任一方法"时才输出 ERROR。该偏差不影响功能正确性。

## 修改要求（REJECTED）

1. **MockAiServiceTest.java**：新增 `shouldBeAnnotatedWithService` 测试方法，通过反射验证 `MockAiService` 类标注了 `@Service` 注解。
2. **MockAiServiceTest.java**：在 `triageShouldReturnSuccessResult` 中增加对 `((TriageResponse)result.getData()).getRecommendedDepartments().get(0).getDepartmentName()` 和 `.getReason()` 的断言，确认为 `"mock_departmentName"` 和 `"mock_reason"`。
3. **NoOpDegradationStrategyTest.java**：新增 `shouldBeAnnotatedWithComponent` 测试，验证 `NoOpDegradationStrategy` 类标注了 `@Component`。
4. **FallbackAiServiceTest.java**：新增 `shouldReturnOriginalResultWhenDelegateAlreadyDegraded` 测试，mock 委托返回 `AiResult.degraded(reason)`，验证 FallbackAiService 直接返回该结果（`result.isDegraded()=true`），不触发策略判定。
