# 测试审查报告（v8 r1）

## 审查结果
REJECTED

## 发现

- **[严重]** `SchedulingRetryConfigTest.java:27` — 测试断言有类型错误：`@ConditionalOnProperty.value()` 返回 `String[]`，但测试使用 `isEqualTo()` 与 `String` 比较，导致断言永远失败。该测试代码本身存在缺陷，无法正确验证配置属性，属于测试无效问题。

- **[一般]** `TriageConverterTest.java:115` — 测试期望 `result.getDoctors()` 为 `null`，但详细设计规定 `TriageResponse.doctors` 为 `List<RecommendedDoctor>`（约束 0-5 项），空列表 `[]` 是符合设计的合法值。测试期望与设计不符，测试断言错误。

- **[信息]** `TriageServiceImplTest.java:118` — 测试正确检测到源码实现遗漏（三次 AI 失败后未设置 `fallbackHint`）。该测试本身有效，测试断言正确，问题在源码而非测试。

## 修改要求（仅 REJECTED 时）

1. `SchedulingRetryConfigTest.java:27`：`@ConditionalOnProperty.value()` 返回 `String[]`，测试应使用数组/集合比较方式（如 `isEqualTo(new String[]{"consultation.scheduling.enabled"})` 或 `containsExactly("consultation.scheduling.enabled")`），而非与 `String` 直接比较。这是测试代码自身的断言缺陷，需修正以使测试有效。

2. `TriageConverterTest.java:115`：根据详细设计，`TriageResponse.doctors` 约束为 0-5 项，空列表 `[]`（0 项）是合法值。测试应改为期望 `result.getDoctors()` 为空列表而非 `null`。如 `assertNotNull(result.getDoctors())` + `assertTrue(result.getDoctors().isEmpty())`。需与设计保持一致。
