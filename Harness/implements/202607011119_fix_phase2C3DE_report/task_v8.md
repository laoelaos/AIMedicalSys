# 任务指令（v8）

## 动作
RETRY

## 任务描述
修复 `MockAiServiceTest.shouldBeAnnotatedWithConditionalOnProperty` 中 `annotation.name()` 断言类型错误。

**文件路径：**
- `AIMedical/backend/modules/ai/ai-impl/src/test/java/com/aimedical/modules/ai/impl/mock/MockAiServiceTest.java`

**修改内容：** 第42行

```java
// 当前（错误）：
assertEquals("ai.mock.enabled", annotation.name());

// 改为：
assertArrayEquals(new String[]{"ai.mock.enabled"}, annotation.name());
```

## 选择理由
R7 验证失败：
- `MockAiServiceTest.shouldBeAnnotatedWithConditionalOnProperty:42 — expected: <ai.mock.enabled> but was: <[ai.mock.enabled]>`
- `@ConditionalOnProperty.name()` 返回 `String[]`，但测试用 `assertEquals(String, String[])` 导致类型不匹配
- 只需改这一行，无需其他修改

## 任务上下文
- `@ConditionalOnProperty` 注解的 `name` 属性类型为 `String[]`（可指定多个属性名）
- `annotation.name()` 返回 `String[]`，即使只传一个值也是单元素数组
- `assertArrayEquals` 是正确匹配方式

## RETRY 说明
R7 失败原因：`assertEquals("ai.mock.enabled", annotation.name())` — String 与 String[] 类型不匹配。R7 的方向正确（替换 shouldBeAnnotatedWithProfile 为 shouldBeAnnotatedWithConditionalOnProperty），仅此断言类型需修正。

## 验证标准
1. 修改后 `mvn test -pl modules/ai/ai-impl -am` 通过（65 用例，0 失败）
2. 全量 `mvn test` 通过（913+ 用例，0 失败，0 错误）
