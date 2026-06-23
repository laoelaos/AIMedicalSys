# 测试审查报告（v7 r1）

## 审查结果
REJECTED

## 发现

- **[一般]** `MockAiServiceTest.java` — 缺少 `shouldBeAnnotatedWithService` 测试。设计契约（detail_v7.md:191）明确要求验证 `MockAiService` 类标注了 `@Service` 注解，但测试类未包含该测试项，注解退化时无测试覆盖。

- **[一般]** `MockAiServiceTest.java:29-35` — `triageShouldReturnSuccessResult` 仅断言 `assertNotNull(result.getData())`，未验证设计契约（detail_v7.md:192）要求的 `recommendedDepartments[0].departmentName="mock_departmentName"` 和 `reason="mock_reason"` 字段值。

- **[一般]** `NoOpDegradationStrategyTest.java` — 缺少 `shouldBeAnnotatedWithComponent` 测试。设计契约（detail_v7.md:214）要求验证 `NoOpDegradationStrategy` 类标注了 `@Component` 注解。

- **[一般]** `FallbackAiServiceTest.java` — 缺少 `shouldReturnOriginalResultWhenDelegateAlreadyDegraded` 测试。设计契约（detail_v7.md:228）要求覆盖委托返回 `degraded=true` 结果时直接透传原结果的场景。

## 修改要求

1. **MockAiServiceTest.java** — 新增 `shouldBeAnnotatedWithService` 测试，通过反射或 `assertNotNull(service.getClass().getAnnotation(Service.class))` 验证 `@Service` 注解存在。

2. **MockAiServiceTest.java:29-35** — 在 `triageShouldReturnSuccessResult` 中增加对 `result.getData()` 转型为 `TriageResponse` 后的 `getRecommendedDepartments().get(0).getDepartmentName()` 和 `getReason()` 的断言，值分别应为 `"mock_departmentName"` 和 `"mock_reason"`。

3. **NoOpDegradationStrategyTest.java** — 新增 `shouldBeAnnotatedWithComponent` 测试，验证 `@Component` 注解存在。

4. **FallbackAiServiceTest.java** — 新增 `shouldReturnOriginalResultWhenDelegateAlreadyDegraded` 测试，构造一个返回 `AiResult.degraded("reason")` 的 Mock 委托，验证 `fallback.triage(request).join()` 结果的 `isDegraded()` 为 true 且 `fallbackReason` 为 `"reason"`。
